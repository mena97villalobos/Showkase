# Showkase architectural review

Review date: 2026-05-10. Reviewer perspective: Android architect. Scope: `showkase`, `showkase-processor`, `showkase-annotation`, `showkase-screenshot-testing` + its two backend modules. Sample / browser-testing / sample-submodule modules sampled for patterns only. Focus areas: (1) public API & binary-compat, (2) processor architecture, (3) Compose UI & perf, (4) testing strategy, (5) KAPT removal roadmap.

All findings cite `file:line` so each is independently actionable. Out of scope for this review:
version bumps (Compose, Kotlin, AGP, Roborazzi, shot, vanniktech), README rewrites, new features,
code changes. The review is doc-only.

> **Note (post-review):** The Paparazzi backend (`showkase-screenshot-testing-paparazzi*` modules)
> was subsequently replaced by Roborazzi after `paparazzi-2.0.0-alpha04` proved incompatible with
> Gradle 9. References in this document to those modules describe the codebase at the time of writing;
> the active screenshot pipeline is now `showkase-screenshot-testing-roborazzi` / `-roborazzi-sample`.

## 1. Executive summary

Showkase is in good architectural shape for a 5-year-old, actively-maintained Compose library: a clean three-layer split (annotation surface, browser, screenshot testing), an XProcessing-based annotation processor that already supports both KAPT and KSP, and a well-scoped public surface. The biggest opportunities are *cleanup*, not redesign.

**Top five recommendations**, ranked by impact × effort:

| # | Recommendation | Priority | Effort 
|---|---|---|---|
| 1 | **Remove KAPT entirely.** ~180 LOC dead-on-arrival, plus build-system simplification across 7 modules. KSP already has parity. Detailed three-phase roadmap in §5. | **P0** | M |
| 2 | **Fix three "discarded-copy" state bugs** in the browser — `.clear()` / `.clearActiveSearch()` results are not propagated, so back-press doesn't actually reset state. See §6, finding 6.3. | **P0** | S |
| 3 | **Delete dead code**: `BackButtonHandler.kt` (unused, with TODO to remove), 38 lines of commented-out `TopAppBar` code in `ShowkaseBrowserApp.kt`, three `Log.e("BackPressed", …)` diagnostics, and the `drawerContent = null` Scaffold parameter. ~120 LOC of pure deletion. | **P1** | S |
| 4 | **Add stable `key`s to six `LazyColumn` callsites.** Currently every list reorders trigger full recomposition. See §6, finding 6.5. | **P1** | S |
| 5 | **Tighten the public API:** mark `ShowkaseProvider`, `ToolbarTitle`, and `SimpleTextCard` as `internal` / `@RestrictTo`; harden the reflective Activity load. See §3. | **P1** | S |

**Health by focus area:**

- **API design** — solid. Three accidental leaks (P1), one ergonomics-vs-stability tension on `ShowkaseScreenshotTest.onScreenshot`'s 8-parameter signature.
- **Processor** — well structured, but the KAPT/KSP duality is now pure overhead and adds ~180 LOC of fragile branching (reflection, value-class workaround, separate parameter-default detection paths).
- **Compose UI** — works, but has technical-debt sediment: dead code, missing list keys, three state bugs around back-press, hardcoded light theme, all M2.
- **Testing** — strong on the processor (50+ golden-file tests across both backends), weak on the browser itself (zero unit tests in `showkase`). KAPT removal needs careful test bookkeeping (BaseProcessorTest pins KAPT to Kotlin 1.9 language version, a hidden constraint).
- **KAPT removal** — fully feasible in three sequenced phases. CI already exercises both paths; the KSP path is the one that needs to stay.

## 2. Module structure & dependency graph

14 modules total, grouped by role:

```
Public API consumers depend on:
  showkase ───── api ───── showkase-annotation
       └── api ──> showkase-screenshot-testing ──> showkase-screenshot-testing-shot
                                              \─── showkase-screenshot-testing-paparazzi

Internal:                                Sample / fixtures:
  showkase-processor                       sample, sample-submodule, sample-submodule-2
  showkase-processor-testing               showkase-browser-testing(+2 submodules)
                                           showkase-screenshot-testing-paparazzi-sample
```

`api()` transitively-exported declarations (i.e. what library consumers will pull in just by depending on Showkase):

- `showkase/build.gradle.kts:52` — `api(project(":showkase-annotation"))` ✓ (consumers need the annotation classes; correct)
- `showkase-screenshot-testing/build.gradle.kts:47-58` — exports `showkase` plus JUnit, Test Core/Rules/Runner, `compose-uiTest` (correct for the use case; consumers writing screenshot tests need these)
- `showkase-screenshot-testing-shot/build.gradle.kts:50-62` — exports the above plus `shot-android` (correct)
- `showkase-screenshot-testing-paparazzi/build.gradle.kts:75-86` — `compileOnly(libs.test.paparazzi)` (smart: avoids forcing Paparazzi on consumers who don't use this backend) and `api(project(":showkase"))`

**Coupling concern (P2)** — `ShowkaseBrowserComponent` lives in the `showkase` module rather than `showkase-annotation` because it holds `@Composable () -> Unit` and therefore needs the Compose runtime classpath. There's a longstanding TODO at `showkase/src/main/java/com/airbnb/android/showkase/models/ShowkaseBrowserComponent.kt:6` to "move it to a different module". For this review's scope I'd leave it — splitting the data model into a separate "showkase-models" module would be a real architectural lift, and the current placement is a defensible workaround. Worth tracking but not worth fixing in isolation.

**Coupling concern (P2)** — `showkase-screenshot-testing/ShowkaseScreenshotTest.kt:22` imports `com.airbnb.android.showkase.ui.padding4x` directly from the browser module's UI package. That's a UI-styling constant leaking into a public testing API. The fix is trivial (duplicate `4.dp` locally), worth doing when next touching that file.

## 3. Public API surface

The library's binary-stability contract, by entry point:

### 3.1 Entry points (must stay stable)

- `ShowkaseBrowserActivity.getIntent(context, rootModuleCanonicalName)` — `showkase/src/main/java/com/airbnb/android/showkase/ui/ShowkaseBrowserActivity.kt:98-101`. Documented in README. Single argument shape is solid; signature is unlikely to need changes.
- `Showkase` receiver object — `showkase/src/main/java/com/airbnb/android/showkase/models/Showkase.kt:9`. Pure marker; extensions like `getBrowserIntent` / `getMetadata` are generated. Stable.
- `Showkase.getMetadata()` (generated) — returns `ShowkaseElementsMetadata`.
- `ShowkaseScreenshotTest` interface — `showkase-screenshot-testing/src/main/java/com/airbnb/android/showkase/screenshot/testing/ShowkaseScreenshotTest.kt:59-184`.
- All annotations in `showkase-annotation/src/main/java/com/airbnb/android/showkase/annotation/` — public ABI.

### 3.2 Visibility leaks to fix (P1)

Three types are `public` by default but should be tightened:

- **`ShowkaseProvider` interface — `showkase/src/main/java/com/airbnb/android/showkase/models/ShowkaseProvider.kt:7`.** Already documented as "for internal usage only" (line 5). It's the contract between the generated codegen class and the activity's reflective load. Mark as `@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)` — keeps it public on the bytecode for generated code to see it across modules, but signals to consumers that it's not for them. Adds `androidx.annotation` if not present.
- **`ToolbarTitle` composable — `showkase/src/main/java/com/airbnb/android/showkase/ui/ShowkaseBrowserApp.kt:319`.** No visibility modifier → `public`. Internal helper, only called from `AppBarTitle` in the same file. Should be `private`.
- **`SimpleTextCard` composable — `showkase/src/main/java/com/airbnb/android/showkase/ui/CommonComponents.kt:26-46`.** No visibility modifier → `public`. Only called from `ShowkaseGroupsScreen` and `ShowkaseCategoriesScreen` within the module. Should be `internal`.

### 3.3 Annotation surface (`showkase-annotation`)

Eight annotations + two supporting types (`ScreenshotConfig` sealed interface, `ScreenshotCaptureType` enum, `ShowkaseRootModule` interface). Notes:

- **`ShowkaseComposable` (`showkase-annotation/.../ShowkaseComposable.kt:68-85`)** — `@Repeatable`, `@MustBeDocumented`, source retention. 10 parameters; large but reasonable for a config-as-annotation. The nested `ScreenshotCaptureConfig` defaults at lines 79-84 carry an inline comment "Need to specify default values here or else KAPT throws an error" — pure KAPT artifact. Comment can be deleted once KAPT is gone (the defaults themselves stay; they're part of the public API).
- **`ScreenshotCaptureType.MultipleImagesAtOffsets` (`ShowkaseComposable.kt:135-141`)** — the KDoc notes "This isn't working currently in Paparazzi, see https://github.com/cashapp/paparazzi/pull/1645." Stale TODO worth re-verifying; if paparazzi has since fixed it, the workaround in `PaparazziShowkaseScreenshotTest.kt:114-119` (offset loop) is now the canonical path.
- **`ShowkaseMultiPreviewCodegenMetadata` (`showkase-annotation/.../ShowkaseMultiPreviewCodegenMetadata.kt:1-10`)** — missing `@Retention` (defaults to `RUNTIME`), missing `@MustBeDocumented`, missing blank line after `package`. Should be `@Retention(AnnotationRetention.BINARY)` since it's read from classpath at compile time only — no need to survive at runtime. **P2 ergonomics**, not user-facing.
- **`ShowkaseCodegenMetadata` (`showkase-annotation/.../ShowkaseCodegenMetadata.kt:23-45`)** — correctly `@Retention(AnnotationRetention.BINARY)`. Good. Has 19 parameters; this is reasonable given it's a serialization format, not a user-facing annotation.
- **`ShowkaseRootCodegen` (`showkase-annotation/.../ShowkaseRootCodegen.kt`)** — `@Retention(AnnotationRetention.RUNTIME)` because `getShowkaseRootCodegenOnClassPath` (`ShowkaseProcessor.kt:559-573`) reads it via XProcessing's `getAnnotation` on a classpath element. RUNTIME might be overkill if the read is at compile time; BINARY may suffice. Worth verifying.

### 3.4 Activity reflective load hardening (P1)

`ShowkaseBrowserActivity.kt:67-82` does:

```kotlin
val showkaseComponentProvider =
    Class.forName("$classKey$AUTOGEN_CLASS_NAME").getDeclaredConstructor().newInstance()
val showkaseMetadata = (showkaseComponentProvider as ShowkaseProvider).metadata()
```

…and catches only `ClassNotFoundException`. Other realistic failure modes are uncaught:

- `NoSuchMethodException` (no no-arg constructor — possible if a future processor change adds a parameter)
- `InstantiationException` (abstract class)
- `IllegalAccessException`
- `ClassCastException` (the generated class doesn't implement the expected interface — e.g. signature drift after a release)
- `InvocationTargetException`
- `LinkageError` (multi-classloader edge cases)

Recommend widening the catch to `ReflectiveOperationException` (covers the first four) and adding a `ClassCastException` branch — or, even cleaner, wrapping the whole load in a single `runCatching { … }.getOrElse { showkaseException(it) }` and surfacing `ShowkaseException` with the original cause. Failure surfaces as the existing `ShowkaseErrorScreen` rather than a hard crash on the user's device.

### 3.5 `ShowkaseScreenshotTest.onScreenshot` parameter list

`showkase-screenshot-testing/.../ShowkaseScreenshotTest.kt:85-94` — 8 parameters, file-level Detekt suppression `@Suppress("Detekt.TooGenericExceptionCaught", "Detekt.TooGenericExceptionThrown", "Detekt.LongParameterList")` at line 58. **P2**: consider replacing the parameter list with a single `ScreenshotMetadata` data class. Backwards-incompatible change so it'd want to ship behind a deprecation cycle. Not urgent.

## 4. KSP/KAPT processor architecture

Showkase's annotation processor is a single-class entrypoint with a writer-per-output-file pattern, abstracted over both backends via Room's `androidx.room:room-compiler-processing` (XProcessing).

### 4.1 Entry-point design

- `showkase-processor/src/main/java/com/airbnb/android/showkase/processor/BaseProcessor.kt:22-24` — abstract class implements **both** `AbstractProcessor` (for KAPT) and `SymbolProcessor` (for KSP). A nullable `kspEnvironment` (line 23) determines which mode is live. The `process` method is implemented twice (once for each interface; lines 60-72 and 74-82), both delegating to `internalProcess` (line 84-100) which uses XProcessing types uniformly.
- `BaseProcessor.kt:43` — `isKsp()` helper. Used only for timer logging at line 90.
- `META-INF/services/javax.annotation.processing.Processor` and `META-INF/services/com.google.devtools.ksp.processing.SymbolProcessorProvider` register the processor for both runtimes.
- `ShowkaseProcessor.kt:42-46` — `ShowkaseProcessorProvider : SymbolProcessorProvider` thin KSP factory. `ShowkaseProcessor.kt:48-51` — KAPT-default constructor `@JvmOverloads`.

This dual-mode design is *correct* but exists entirely to support KAPT. Once KAPT is gone, `BaseProcessor` collapses to a `SymbolProcessor` wrapper or merges into `ShowkaseProcessor` outright.

### 4.2 Code-generation writers

Six writers in `showkase-processor/src/main/java/com/airbnb/android/showkase/processor/writer/`:

| Writer | Output | XFiler mode | Purpose |
|---|---|---|---|
| `ShowkaseCodegenMetadataWriter` | `ShowkaseMetadata_<pkg_underscored>.kt` | Aggregating | Per-module: emits `@ShowkaseCodegenMetadata`-annotated stub methods so root module can discover them via classpath scan |
| `ShowkaseBrowserPropertyWriter` | `<module>ShowkaseBrowserComponent.kt`, `<module>ShowkaseColor.kt`, `<module>ShowkaseTypography.kt` | Isolating | Per-module: emits typed `val`s |
| `ShowkaseBrowserWriter` | `<Root>Codegen.kt` implementing `ShowkaseProvider` | Aggregating | Root module: aggregates all metadata into the runtime provider |
| `ShowkaseExtensionFunctionsWriter` | `<Root>ShowkaseExtensionFunctionsCodegen.kt` | Aggregating | Root module: generates `Showkase.getBrowserIntent(context)` / `Showkase.getMetadata()` |
| `ShowkaseScreenshotTestWriter` | `<TestClass>Codegen.kt` | Aggregating | If `@ShowkaseScreenshot` present: shot integration |
| `PaparazziShowkaseScreenshotTestWriter` | `<TestClass>Codegen.kt` | Aggregating | If `@ShowkaseScreenshot` + paparazzi base class: paparazzi integration |

The Aggregating-vs-Isolating choice looks correct. `ShowkaseBrowserPropertyWriter` is Isolating because each property file is self-contained (one per discovered element); everything else is Aggregating because it depends on the full round set.

### 4.3 Backend-specific branching (this is what KAPT removal deletes)

Five distinct branch points exist for KAPT-specific behavior:

1. **Top-level function detection** — `showkase-processor/src/main/java/com/airbnb/android/showkase/processor/models/ShowkaseMetadata.kt:469-492`. KAPT path reflects on `XTypeElement.kotlinMetadata` (a private field) via `getFieldWithReflection`. KSP path is a single line: `enclosingElement !is XTypeElement`. The reflection helper is at `showkase-processor/.../utils/ReflectionUtils.kt:9-39`.
2. **Parameter default-value detection** — `showkase-processor/.../logging/ShowkaseValidator.kt:116-121` (branch), `:126-137` (KAPT impl using `kotlin.metadata.KmFunction.declaresDefaultValue`), `:139-166` (KAPT helper, ~28 lines), `:168-182` (KSP impl, ~15 lines using `parameter.hasDefaultValue`). The KAPT path is the bulkier one — XProcessing's `hasDefaultValue` returns wrong answers for top-level functions under javac, forcing the manual kotlin-metadata parse.
3. **Color value-class workaround** — `ShowkaseValidator.kt:199-209`. KAPT can't see through `@JvmInline value class Color(val value: ULong)`, so it sees `Color` as `Long`. KSP sees the wrapper type directly.
4. **Backend detector** — `ShowkaseMetadata.kt:495-503`. `isJavac()` is a try-catch on `XElement.toJavac()`. Used by branches 1, 2, 3.
5. **Custom multi-preview annotation registration** — `ShowkaseProcessor.kt:72-87`. The `multiPreviewType` processor argument exists only for KAPT users (KSP users discover custom annotations via `@ShowkaseMultiPreviewCodegenMetadata` classpath scan in `processCustomAnnotationFromClasspath`, line 210). Lines 72-81 contain the comment "This should only be provided by KAPT users".

Supporting KAPT-only files / dependencies:

- `showkase-processor/.../utils/KotlinMetadataUtils.kt` (10 lines) — wraps `KotlinClassMetadata.readStrict` for branches 1, 2.
- `showkase-processor/.../utils/ReflectionUtils.kt` (45 lines) — generic `getFieldWithReflection<T>` used only by branch 1.
- `kotlin-metadata-jvm` library dependency in `showkase-processor/build.gradle.kts` — required by KotlinMetadataUtils.kt.
- `tasks.withType<Test>().configureEach { jvmArgs("--add-opens=jdk.compiler/...") }` in `showkase-processor-testing/build.gradle.kts:73-86` — only needed because KAPT in `kotlin-compile-testing` needs javac internals on Java 17+.

### 4.4 Open processor TODOs

Four substantive open items:

- **`ShowkaseProcessor.kt:617-618`** — Preview-parameter composables aren't screenshot-tested. "There's no way to get information about how many previews are dynamically generated using preview parameter as it happens on run time and our codegen doesn't get enough information to be able to predict how many extra composables the preview parameters extrapolate to." Real architectural gap. **P2 follow-up** — would require runtime test enumeration (parameterized junit) rather than codegen-time counts.
- **`ShowkaseMetadata.kt:405-407` and `:433-435`** — Color/Typography fields inside companion objects don't work. "Properties are generated outside the companion object in java land." A KAPT-era bug; **revisit after KAPT removal** to see if it's fixed for free under KSP.
- **`ShowkaseValidator.kt:216-218` and `:243-245`** — `private` modifier check for color/typography fields is missing because the javac element view always shows fields as private. Another KAPT-era constraint; **revisit after KAPT removal** — KSP can read the source visibility correctly.
- **`ShowkaseValidator.kt:384-385`** — `@ShowkaseScreenshot(rootShowkaseClass = …)` validates the type but not that it's actually `@ShowkaseRoot`-annotated. Minor; produces a confusing error message later if mismatched.

### 4.5 Error handling

- `BaseProcessor.kt:102-115` — `tryOrPrintError` catches all `Throwable` and prints `e.stackTraceToString()` via `messager.printMessage`. Surfaces KSP-swallowed errors. Acceptable. The detekt suppression at line 103 is honest.
- `ShowkaseProcessorException` (`showkase-processor/.../exceptions/ShowkaseProcessorException.kt`) supports an `element` to enable element-attached diagnostics. Clean pattern.

## 5. KAPT removal roadmap

This is the headline deliverable of the review. The work is feasible, sequenced, and each phase ships independently. CI already exercises both paths (`./gradlew check` with KAPT; `:showkase-screenshot-testing-paparazzi-sample:verifyPaparazziDebug -PuseKsp=true` with KSP), so the verification path is well-trodden.

### Phase A — Strip the `useKsp` flag and switch CI to KSP-only

**Goal:** Remove every `if (project.hasProperty("useKsp")) { … } else { … }` block in build files. Keep only the KSP branch. No processor changes.

**Files touched (7 module build scripts):**

- `sample/build.gradle.kts` (lines 8-23, 57-67, 77-83)
- `sample-submodule/build.gradle.kts` (similar shape)
- `sample-submodule-2/build.gradle.kts`
- `showkase-browser-testing/build.gradle.kts` (also has `buildConfigField` conditional at lines ~41-46)
- `showkase-browser-testing-submodule/build.gradle.kts`
- `showkase-browser-testing-submodule-2/build.gradle.kts`
- `showkase-screenshot-testing-paparazzi-sample/build.gradle.kts` (also drop conditional `kspTest`/`kaptTest`)

**Catalog & root:**
- `gradle/libs.versions.toml` — remove `kotlin-kapt` plugin alias.
- `build.gradle.kts` (root) — no change needed; KAPT was never in the plugins block.

**CI:**
- `.github/workflows/android.yml:24-25` — change `./gradlew check --stacktrace` to be the *KSP* path. Since KSP is now the only path, no `-PuseKsp=true` flag is needed anywhere. The flag itself becomes a no-op.
- Lines 41-42, 136, 149: keep the existing KSP-specific invocations as-is; they remain correct.
- Optional: drop the `useKsp` matrix-strategy semantics from the UI-testing job which currently runs the suite twice (lines 126-149).

**README:**
- Update install instructions (lines ~68-88, 177-187) to remove the "KAPT" alternative path. Document only the KSP setup. The `multiPreviewType` compiler-argument syntax (lines 454-462) goes away (replaced by KSP's automatic `@ShowkaseMultiPreviewCodegenMetadata` classpath discovery, which Showkase already does).

**Risk:** Low. No processor changes. CI verifies both paths today; after this phase, only KSP runs but the KSP path was already passing.

**Verify:** `./gradlew check` (now KSP), `./gradlew :showkase-screenshot-testing-paparazzi-sample:verifyPaparazziDebug`. Both should pass without `-PuseKsp=true`. Expected LOC delta: -120 build-script lines.

### Phase B — Remove KAPT branches from the processor

**Goal:** Delete `isJavac()` and every branch on it. The processor becomes single-path.

**Files touched:**

- `showkase-processor/src/main/java/com/airbnb/android/showkase/processor/models/ShowkaseMetadata.kt`
  - Delete `isJavac()` (lines 495-503).
  - Simplify `isTopLevel` (lines 469-492) to `fun XElement.isTopLevel(enclosingElement: XMemberContainer): Boolean = enclosingElement !is XTypeElement`.
  - Drop import of `XConverters.toJavac`.
- `showkase-processor/src/main/java/com/airbnb/android/showkase/processor/logging/ShowkaseValidator.kt`
  - Delete the KAPT branch at lines 116-117. Collapse the `when` to two arms: empty params → false, otherwise → `validateKspComposableParameters` (rename to drop the `Ksp` prefix at this point).
  - Delete `validateKaptComposableParameter` extension functions (lines 126-166, ~40 lines).
  - Delete the KAPT branch in `validateColorElement` at lines 199-209; replace with the unconditional KSP check `if (element.type.rawType == colorType.rawType) return`.
  - Drop imports: `XConverters.toJavac` (line 9), `models.isJavac` (line 22), `utils.kotlinMetadata` (line 24), `kotlin.metadata.*` (lines 28-30).
- `showkase-processor/src/main/java/com/airbnb/android/showkase/processor/ShowkaseProcessor.kt`
  - Delete `supportedCustomAnnotationTypes()` (lines 74-81).
  - Update `getSupportedAnnotationTypes` to drop the call to it (line 66).
  - Update `getSupportedOptions()` (lines 83-87) to remove `"multiPreviewType"`.
- `showkase-processor/src/main/java/com/airbnb/android/showkase/processor/utils/KotlinMetadataUtils.kt` — **delete the whole file** (was only used by KAPT branches).
- `showkase-processor/src/main/java/com/airbnb/android/showkase/processor/utils/ReflectionUtils.kt` — **delete the whole file** if no other consumers (verify via `git grep getFieldWithReflection` — based on this review it was only used by `isTopLevel`).
- `showkase-processor/build.gradle.kts` — remove `implementation(libs.kotlinXMetadata)` line. Optionally remove the `kotlinPoet`-related opt-in compiler args if no longer needed (verify with build).

**Risk:** Medium. This touches the processor's hot path. The 50+ golden-file tests in `showkase-processor-testing` are the safety net.

**Verify:**
1. `./gradlew :showkase-processor:check` — unit tests pass.
2. `./gradlew :showkase-processor-testing:check` — integration tests pass.
3. Manually diff generated outputs in `showkase-processor-testing/src/test/resources/ShowkaseProcessorTest/<case>/output/` against pre-change versions if any expected-output files change. Most should be byte-identical since both backends already produce the same output by design.

**Expected LOC delta:** ~-180 LOC in processor sources + 2 deleted files.

### Phase C — Drop the dual-mode infrastructure

**Goal:** Delete the `AbstractProcessor` half. The processor becomes a pure KSP `SymbolProcessor`.

**Files touched:**

- `showkase-processor/src/main/java/com/airbnb/android/showkase/processor/BaseProcessor.kt`
  - Remove `AbstractProcessor` from the supertype list (line 24).
  - Remove the KAPT-only `init(processingEnv)` override (lines 47-51).
  - Remove the KAPT `process(annotations, roundEnv)` override (lines 60-72).
  - Remove `getSupportedSourceVersion` override (line 45) — only KAPT cares.
  - Remove `isKsp()` (line 43) — always true.
  - The class becomes ~70 lines instead of ~120.
  - Consider inlining `BaseProcessor` into `ShowkaseProcessor` outright once it's KSP-only — there's no longer a second backend, so the abstraction earns less.
- `showkase-processor/src/main/resources/META-INF/services/javax.annotation.processing.Processor` — **delete the whole file**.
- `showkase-processor/src/main/java/com/airbnb/android/showkase/processor/ShowkaseProcessor.kt`
  - Remove `@SupportedSourceVersion(SourceVersion.RELEASE_17)` annotation (line 48) — KAPT-only.
  - Remove `@JvmOverloads` from the constructor (line 49) — needed only so KAPT can call the no-arg ctor. The KSP path uses `ShowkaseProcessorProvider.create()`.
  - Remove imports for `javax.annotation.processing.SupportedSourceVersion`, `javax.lang.model.SourceVersion`.
- `showkase-processor-testing/build.gradle.kts:73-86` — **delete the entire `tasks.withType<Test>().configureEach { jvmArgs(--add-opens=…) }` block**. No more KAPT means no more javac internal-API access required.
- `showkase-processor-testing/src/test/java/com/airbnb/android/showkase_processor_testing/BaseProcessorTest.kt`
  - Remove the `Mode` enum's `KAPT` value (line 32). Or remove the `Mode` enum entirely — single mode left.
  - Remove `Mode.KAPT` branch (lines 63-66). The remaining `Mode.KSP` branch is now unconditional.
  - Remove `languageVersion = "1.9"` constraint (was only set in KAPT mode at line 64). Lock the test language version to `2.1` (line 57).
  - Default `modes` parameter (line 40): drop or simplify since there's only one mode.

**Risk:** Low if Phases A and B are clean. The infrastructure removal is mechanical.

**Verify:**
1. `./gradlew :showkase-processor-testing:check` — golden tests pass at language version 2.1.
2. `./gradlew check` — full build green.
3. `./gradlew assemble` for sample modules to verify the published-artifact path.

**Expected LOC delta:** ~-80 LOC + 2 deleted files + JVM-args removed from test config.

### Phase C optional extension: drop XProcessing entirely?

Once KAPT is gone, XProcessing's reason-for-being (abstracting both backends) goes away. **Worth its own evaluation, not blocking on this roadmap.** Switching to KSP-native types would drop a transitive dependency (`androidx.room:room-compiler-processing`, ~5MB) and reduce one level of indirection. The cost is rewriting every reference to `XElement`, `XTypeElement`, `XAnnotation`, `XProcessingEnv`, `XFiler` etc. — that's a large diff across the whole processor module.

Recommendation: defer. Keep XProcessing for stability; revisit if/when Room's compiler-processing API churns or if there's a compelling KSP-2-only API the project wants.

### Summary

| Phase | LOC delta | Risk | Files removed | Verification |
|---|---|---|---|---|
| A — Build scripts | ~-120 | Low | 0 | `./gradlew check` |
| B — Processor branches | ~-180 | Medium | 2 (KotlinMetadataUtils, ReflectionUtils) | `:showkase-processor-testing:check` |
| C — Dual-mode infrastructure | ~-80 | Low | 1 service file + maybe BaseProcessor | full build |
| **Total** | **~-380 LOC** | | **3 files** | |

## 6. Compose UI patterns & performance

Findings in roughly priority order.

### 6.1 Three "discarded-copy" state bugs (P0)

The `ShowkaseBrowserScreenMetadata` extension functions `clear()` and `clearActiveSearch()` (`showkase/src/main/java/com/airbnb/android/showkase/models/ShowkaseBrowserScreenMetadata.kt:29-41`) return a *new copy*; they're pure functions. Three callsites discard the return value, meaning the back-press doesn't actually clear state:

- **`showkase/src/main/java/com/airbnb/android/showkase/ui/ShowkaseComponentsInAGroupScreen.kt:81`** — `showkaseBrowserScreenMetadata.clear()` is called and discarded, then `navigateTo(...)`. The metadata isn't cleared.
- **`showkase/src/main/java/com/airbnb/android/showkase/ui/ShowkaseComponentStylesScreen.kt:88`** — `showkaseBrowserScreenMetadata.clearActiveSearch()` discarded. Search state isn't reset on back-press from styles.
- **`showkase/src/main/java/com/airbnb/android/showkase/ui/ShowkaseCategoriesScreen.kt:92`** — `showkaseBrowserScreenMetadata.clear()` discarded inside `goBackToCategoriesScreen` (called from `ShowkaseGroupsScreen.kt:60-68` back-press handler when `onRootScreen` is false).

Fix: pass through `onUpdateShowkaseBrowserScreenMetadata(showkaseBrowserScreenMetadata.clear())` (or `.clearActiveSearch()`) so the state mutation propagates. Five lines of changes. These are real user-visible bugs: search query persists across screen navigations.

### 6.2 Dead / commented-out code (P1)

- **`showkase/src/main/java/com/airbnb/android/showkase/ui/BackButtonHandler.kt` (entire file, 73 lines).** The file's TODO at line 48 says "Replace with the version Compose just exposed in the activity-compose bindings." That replacement is already live: every screen uses `androidx.activity.compose.BackHandler` directly (e.g. `ShowkaseComponentDetailScreen.kt:7,98`, `ShowkaseGroupsScreen.kt:3,59`, etc.). A `git grep BackButtonHandler` in `showkase/src/main` shows the function is only declared in this file — **never called**. Same for `Handler(`. Delete the entire file.
- **`showkase/src/main/java/com/airbnb/android/showkase/ui/ShowkaseBrowserApp.kt:192-229`** — 38 lines of commented-out `TopAppBar { … }` with comment "Commented out due to TopAppBar not working properly in beta-01 for this use case." Beta-01 was 2021; the workaround above (custom `Row` with `Surface(elevation=4.dp)`) is now the live code. Delete the commented block.
- **`showkase/src/main/java/com/airbnb/android/showkase/ui/ShowkaseCategoriesScreen.kt:83, 87, 91`** — `Log.e("BackPressed", "isSearchActive")` / `"onRootScreen"` / `"else"` — diagnostic logging left in production code. Delete the three `Log.e` lines and the unused `import android.util.Log` at line 3.
- **`showkase/src/main/java/com/airbnb/android/showkase/ui/ShowkaseBrowserApp.kt:88`** — `Scaffold(drawerContent = null, …)`. The drawer slot isn't used. Drop the parameter (it defaults to `null` anyway in the Scaffold signature).

### 6.3 Composition side-effects (P1)

**`showkase/src/main/java/com/airbnb/android/showkase/ui/ShowkaseGroupsScreen.kt:149-176`** — `ShowkaseTypographyGroupsScreen` calls `onUpdateShowkaseBrowserScreenMetadata(...)` directly inside the composable body when `groupedTypographyMap.size == 1` (line 150-154). This is a side-effect during composition; it triggers a recomposition, which re-runs the side-effect, potentially in a loop (Compose will eventually skip if the state is equal, but it's still fragile). The correct shape is:

```kotlin
LaunchedEffect(groupedTypographyMap) {
    if (groupedTypographyMap.size == 1) {
        onUpdateShowkaseBrowserScreenMetadata(
            showkaseBrowserScreenMetadata.copy(
                currentGroup = groupedTypographyMap.entries.first().key,
            )
        )
    }
}
```

### 6.4 Missing stable `key`s on `LazyColumn { items(...) }` (P1)

Six `LazyColumn`s in the browser, none with explicit `key`s. For lists that reorder (e.g., when a search filter narrows the set), Compose falls back to position-based identity, causing every item to recompose on every change. Recommendations:

| File | Line | Items | Suggested key |
|---|---|---|---|
| `ShowkaseGroupsScreen.kt` | 38 | groups (`Map.Entry<String, List<*>>`) | `key = { it.key }` — group name is unique |
| `ShowkaseComponentsInAGroupScreen.kt` | 42 | `ShowkaseBrowserComponent` (one per name) | `key = { it.componentKey }` |
| `ShowkaseColorsInAGroupScreen.kt` | 57 | `ShowkaseBrowserColor` | `key = { "${it.colorGroup}_${it.colorName}" }` |
| `ShowkaseComponentStylesScreen.kt` | 38 | `ShowkaseBrowserComponent` (per style) | `key = { it.componentKey }` |
| `ShowkaseTypographyInAGroupScreen.kt` | 51 | `ShowkaseBrowserTypography` | `key = { "${it.typographyGroup}_${it.typographyName}" }` |
| `ShowkaseCategoriesScreen.kt` | 27 | 3 fixed entries | `key = { it.key.name }` — low impact since it's 3 items |
| `ShowkaseComponentDetailScreen.kt` | 74 | single-item list | N/A — only one item |

The component-key cases are highest-priority: those lists can be large (50-200+ items in real projects) and filter on every search keystroke.

### 6.5 Bidirectional state sync in search field (P2)

**`showkase/src/main/java/com/airbnb/android/showkase/ui/ShowkaseBrowserApp.kt:347-404`** — `ShowkaseSearchField` keeps a local `mutableStateOf(searchQuery.orEmpty())` for the TextField value, then has two `LaunchedEffect`s:

- Lines 355-358: `LaunchedEffect(localSearchQuery) { delay(300); searchQueryValueChange(localSearchQuery) }` — debounced push from local to caller.
- Lines 360-364: `LaunchedEffect(searchQuery) { if (searchQuery != localSearchQuery) { localSearchQuery = searchQuery.orEmpty() } }` — pull from caller to local.

This is a "two-way bound" pattern that can oscillate: external update arrives → local updated → debounce fires → external updated. If the external value sometimes round-trips back as a different string (e.g. trimming), you get a loop. The cleaner pattern is a single source of truth — lift state up to the screen-metadata holder, or use `rememberSaveable` with the searchQuery as the canonical value. Also: the first `LaunchedEffect` fires on initial composition with `localSearchQuery = ""`, which calls `searchQueryValueChange("")` immediately — usually harmless but worth a `if (localSearchQuery.isNotEmpty()) { ... }` guard or a `derivedStateOf` filter.

### 6.6 Hardcoded light mode (P2)

**`showkase/src/main/java/com/airbnb/android/showkase/ui/ShowkaseBrowserApp.kt:74-80`** — `LocalConfiguration provides lightModeConfiguration` forces every previewed component into light mode regardless of the user's system theme. Intent: deterministic rendering. Trade-off: users on a dark-mode device see a jarring light-mode browser, and the only way to see a component in dark mode is via the "Dark Mode" variant in `ShowkaseComponentDetailScreen.kt:205-216`.

Consider adding an in-app toggle (existing nav state has room) or honoring system theme by default. **Not urgent**, but worth thinking about.

### 6.7 Hardcoded colors / theme (P2)

- **`ShowkaseBrowserApp.kt:84`** — `Surface(color = Color.White)` instead of `MaterialTheme.colors.surface` / `background`.
- The entire app uses **Compose Material 2** (`androidx.compose.material.*`), not M3. M3 migration is significant and out of scope here, but worth flagging as a future modernization target.

### 6.8 `ComponentCard` rewraps `MaterialTheme` per card (P2)

**`showkase/src/main/java/com/airbnb/android/showkase/ui/CommonComponents.kt:76-78`** — Each rendered `ComponentCard` wraps in `MaterialTheme(colors = if (darkMode) darkColors() else lightColors())`. In list contexts (`ShowkaseComponentsInAGroupScreen`, `ShowkaseComponentStylesScreen`), this means a fresh `MaterialTheme` per visible card. The `darkColors()` / `lightColors()` calls allocate fresh `Colors` instances on each call. Hoist the two `Colors` instances to file-level `private val`s.

### 6.9 Unsafe `LocalContext as AppCompatActivity` casts (P2)

Three places assume the host activity is `AppCompatActivity`:

- `ShowkaseGroupsScreen.kt:58` — `val activity = LocalContext.current as AppCompatActivity`
- `ShowkaseTypographyInAGroupScreen.kt:33` — same
- `ShowkaseCategoriesScreen.kt:25` — same

If a consumer embeds `ShowkaseBrowserActivity` indirectly through a different host (unusual but possible), or composes the inner composables from a different activity hierarchy, these crash. Since `ShowkaseBrowserActivity` itself extends `AppCompatActivity` (`ShowkaseBrowserActivity.kt:22`), it's safe today — but the cast pattern is fragile. Consider passing an `onFinish: () -> Unit` callback down instead.

### 6.10 `rememberOnBackPressedDispatcherOwner` creates orphan dispatcher (P2)

**`showkase/src/main/java/com/airbnb/android/showkase/ui/ShowkaseComponentDetailScreen.kt:243-254`** — Creates a fresh `OnBackPressedDispatcher()` (line 251) wrapped in an anonymous `OnBackPressedDispatcherOwner` and provides it via `LocalOnBackPressedDispatcherOwner`. The comment in `ComponentCard` (`CommonComponents.kt:70-71`) explains: "to make sure that the navigation of the ShowkaseBrowser does not break when one of the previews has a back press handler in the implementation of the component." That's *intentional* — it isolates back-press from previewed components — but the dispatcher is unwired (not attached to any activity's lifecycle), so it's effectively a black hole. Worth a comment explaining that's deliberate. The same pattern is duplicated in `PaparazziShowkaseScreenshotTest.kt:152-157`.

### 6.11 Recomposition opportunity: no `derivedStateOf` (P2)

A repo-wide grep for `derivedStateOf` finds zero usages. Candidates where it would prevent redundant recompositions:

- `ShowkaseBrowserApp.kt:82-83` — `val currentRoute = navBackStackEntry?.destination?.route`. Many screens key off `currentRoute`; wrapping in `derivedStateOf` avoids retriggering on `navBackStackEntry` updates that don't change the route string.
- `ShowkaseBrowserApp.kt:444-448` — `val startDestination = startDestination(...)` is recomputed on every parent recomposition. Wrap in `remember(groupedColorsMap, groupedTypographyMap, groupedComponentMap)`.

### 6.12 Misnamed parameter in shared screen function (P3)

**`ShowkaseGroupsScreen.kt:18`** — `ShowkaseGroupsScreen` accepts a `groupedTypographyMap: Map<String, List<*>>` parameter, but it's called from `ShowkaseComponentGroupsScreen` (line 108) and `ShowkaseColorGroupsScreen` (line 129) as well. The parameter is a generic "grouped map of UI elements". Rename to `groupedMap` or `groupedElementsMap`. Pure renaming, no behavior change.

## 7. Testing strategy & gaps

### 7.1 The `showkase` module has no unit tests of its own

A grep over `showkase/src/test/` and `showkase/src/androidTest/` finds nothing. The browser logic — filter functions in `ShowkaseGroupsScreen.kt:81-97`, `ShowkaseComponentsInAGroupScreen.kt:87-103`, etc., the `goBack*` functions, and the `startDestination` decision tree (`ShowkaseBrowserApp.kt:466-482`) — has no direct test coverage. These are pure-function helpers (`internal fun matchSearchQuery`, `internal fun getNumOfUIElements`, etc.) that would be trivial to unit-test.

There's a separate `showkase-browser-testing` module with instrumented tests, but those are integration tests of the activity, not unit tests of the navigation/filter logic.

**Recommendation:** Add a `showkase/src/test/` directory with unit tests for the filter functions and the `startDestination` decision logic. ~20-50 short tests, no UI needed. Low effort, high value.

### 7.2 Processor coverage is strong; KAPT removal needs careful test bookkeeping

`showkase-processor-testing/src/test/java/com/airbnb/android/showkase_processor_testing/BaseProcessorTest.kt:40` defaults to running every test for *both* KAPT and KSP. Tests are golden-file based; over 50 cases in `src/test/resources/ShowkaseProcessorTest/<case>/{input,output}/`.

**Hidden constraint to watch during Phase C of the KAPT removal:** `BaseProcessorTest.kt:64` sets `languageVersion = "1.9"` for KAPT mode (vs `"2.1"` for KSP at line 57). Some test inputs may use Kotlin 1.9-era syntax to keep both modes happy. After dropping KAPT, the language version becomes 2.1 unconditionally; verify no test inputs rely on 1.9 behavior. If they do (unlikely, since the test inputs are simple Composable functions), update them.

The test runner has an `UPDATE_TEST_OUTPUTS = false` flag at line 23 that, when set to `true`, rewrites the expected `output/` files. This is the standard "regenerate goldens" pattern.

### 7.3 Preview-parameter screenshot gap

`ShowkaseProcessor.kt:617-618` documents that composables with `@PreviewParameter` aren't covered by generated screenshot tests. This is a real gap, not a KAPT-era artifact. Closing it requires runtime test enumeration (e.g. parameterized JUnit) rather than codegen-time counts, since the parameter provider can produce a dynamic number of instances. **P2 follow-up; not blocking on KAPT removal.**

### 7.4 Browser recomposition tests don't exist

`showkase-browser-testing` has navigation/integration tests (verified by `connectedCheck` in CI), but no test of the kind that catches recomposition regressions (e.g. `composeTestRule.setContent { … }` followed by assertions about how many times a given composable was invoked). Given the missing `key`s in §6.4 and the bidirectional state sync in §6.5, this is a real coverage gap.

**P2:** Add a few targeted Compose UI tests that mount one of the list screens, change the search query, and assert the composable invocation count. The `androidx.compose.ui.test` testing library handles this natively.

### 7.5 Shot vs paparazzi duplication

`ShotShowkaseScreenshotTest.kt:10-25` and `PaparazziShowkaseScreenshotTest.kt:62-99` both implement `ShowkaseScreenshotTest`'s `onScreenshot` — but the actual bitmap-capture loop and metadata routing are duplicated *inside* `ShowkaseScreenshotTest` (`takeComposableScreenshot`, `takeTypographyScreenshot`, `takeColorScreenshot` at lines 96-184). The two backends share that helper; only the `onScreenshot` implementation differs. This is *good* — abstraction is clean.

One small dup: both backends define a `LocalOnBackPressedDispatcherOwner provides …` wrapper around content, with the exact same fake dispatcher implementation. Could be hoisted to a shared `screenshotComposeContent { … }` helper in the base `ShowkaseScreenshotTest`. Minor.

## 8. Module-level findings

### `showkase`

- API surface is intentionally narrow (3 entry points + 4 data models + 1 interface). Good.
- Three accidental visibility leaks (§3.2). Fix.
- Six missing list keys, three state bugs, dead-code accumulation, hardcoded light mode (§6).
- Zero unit tests (§7.1).
- TODO at `ShowkaseBrowserComponent.kt:6` about module coupling — leave for now.

### `showkase-annotation`

- 380 LOC, well-scoped, well-documented.
- One annotation (`ShowkaseMultiPreviewCodegenMetadata`) missing `@Retention`/`@MustBeDocumented` for consistency (§3.3).
- The `ScreenshotCaptureConfig` annotation's KAPT-workaround comment (`ShowkaseComposable.kt:79`) will become stale — clean up during Phase A/B of KAPT removal.

### `showkase-processor`

- 3500 LOC. Two-tier architecture: `BaseProcessor` (dual-backend) → `ShowkaseProcessor` (logic) → 6 writers.
- Biggest cleanup opportunity is KAPT removal (§5): ~380 LOC drop across phases A-C.
- Four open TODOs (§4.4). Three of the four are KAPT-era and may resolve themselves after Phase B.
- `BaseProcessor.tryOrPrintError` (lines 102-115) is the right safety net for processor robustness.

### `showkase-screenshot-testing`

- 200 LOC, well-designed extension point with two clean backend implementations.
- One bleed-through: imports `padding4x` from the browser module (§2). Fix.
- `ShowkaseScreenshotTest.onScreenshot` has 8 parameters and three Detekt suppressions — consider data-class wrapper (§3.5). Backwards-compat tax — defer.
- The duplicated `LocalOnBackPressedDispatcherOwner` wrapper across both backends (§7.5) is minor.

## 9. Prioritized recommendations

Top picks for execution order. "Effort" is rough engineering-day cost: S = under a day, M = 1-3 days, L = a week+.

| # | Recommendation | Priority | Effort | Why first | Where |
|---|---|---|---|---|---|
| 1 | Fix three "discarded-copy" state bugs | **P0** | S | User-visible bugs, 5-line fix | §6.1 |
| 2 | KAPT removal — Phase A (strip useKsp from build) | **P0** | M | Unlocks Phase B; pure mechanical | §5 / Phase A |
| 3 | KAPT removal — Phase B (processor branches) | **P0** | M | Biggest dead-code reduction (~180 LOC) | §5 / Phase B |
| 4 | KAPT removal — Phase C (drop dual-mode) | **P0** | S | Quick finish to the migration | §5 / Phase C |
| 5 | Tighten `ShowkaseProvider`, `ToolbarTitle`, `SimpleTextCard` visibility | **P1** | S | Reduces accidental API surface | §3.2 |
| 6 | Delete `BackButtonHandler.kt`, commented `TopAppBar` block, `Log.e` lines, `drawerContent` | **P1** | S | ~120 LOC of pure deletion | §6.2 |
| 7 | Add stable `key`s to six `LazyColumn` callsites | **P1** | S | Recomposition perf for medium-large lists | §6.4 |
| 8 | Fix composition side-effect in `ShowkaseTypographyGroupsScreen` | **P1** | S | Latent recomposition-loop hazard | §6.3 |
| 9 | Harden Activity reflective load (catch wider exception set) | **P1** | S | Better failure surface for misconfigured consumers | §3.4 |
| 10 | Add unit tests for showkase browser logic | **P2** | M | Currently zero coverage in module | §7.1 |
| 11 | Restructure `ShowkaseSearchField` to single source of truth | **P2** | S | Eliminate oscillation risk | §6.5 |
| 12 | Hoist `MaterialTheme(lightColors()/darkColors())` calls out of `ComponentCard` | **P2** | S | Per-card allocation cleanup | §6.8 |
| 13 | Stable IDs for color/typography screenshots (not `hashCode().toString()`) | **P2** | S | Stable golden filenames | §7.4 area |
| 14 | Investigate companion-object Color/Typography support after Phase B | **P2** | S | TODO at `ShowkaseMetadata.kt:405, 433` may resolve | §4.4 |
| 15 | M2 → M3 migration | **P3** | L | Separate workstream | §6.7 |
| 16 | Move `ShowkaseBrowserComponent` out of `showkase` to a new `showkase-models` module | **P3** | M | Architectural lift; not urgent | §2 |
| 17 | Replace `ShowkaseScreenshotTest.onScreenshot` 8-param signature with data class | **P3** | M | Backwards-incompat; defer until next major | §3.5 |
| 18 | Drop XProcessing in favor of KSP-native APIs | **P3** | L | After KAPT, evaluate if XProcessing is still earning its dep | §5 Phase C extension |

## 10. Out of scope (and why)

- **No version bumps** for Compose, Kotlin, AGP, KSP, Roborazzi, shot, vanniktech, detekt. These are
  separate workstreams with their own risk profiles (especially Compose M2→M3).
- **No README rewrites.** README install instructions reference both KAPT and KSP setups; that gets updated as part of Phase A's CI/docs sweep, but not as part of this review.
- **No new features** — preview-parameter screenshot support, dark-mode toggle in browser, theme customization, drawer/categories revamp. The review flags gaps but doesn't scope new work.
- **No commit/PR creation.** The review is doc-only; each recommendation can be picked up as its own PR.
- **No deep dive into sample / browser-testing modules.** Sampled for pattern confirmation; not exhaustively reviewed.
- **No security review** of the reflective Activity load. The class name is a build-time constant baked into the intent; reflection is on the user's own classpath. Lower risk surface, but worth a one-day pass at some point.

---

*This review is a doc-only deliverable. None of the recommendations have been implemented as part of this pass. Each one is independently shippable as its own PR.*
