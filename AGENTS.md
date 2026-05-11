<!-- code-review-graph MCP tools -->
## MCP Tools: code-review-graph

**IMPORTANT: This project has a knowledge graph. ALWAYS use the
code-review-graph MCP tools BEFORE using Grep/Glob/Read to explore
the codebase.** The graph is faster, cheaper (fewer tokens), and gives
you structural context (callers, dependents, test coverage) that file
scanning cannot.

### When to use graph tools FIRST

- **Exploring code**: `semantic_search_nodes` or `query_graph` instead of Grep
- **Understanding impact**: `get_impact_radius` instead of manually tracing imports
- **Code review**: `detect_changes` + `get_review_context` instead of reading entire files
- **Finding relationships**: `query_graph` with callers_of/callees_of/imports_of/tests_for
- **Architecture questions**: `get_architecture_overview` + `list_communities`

Fall back to Grep/Glob/Read **only** when the graph doesn't cover what you need.

### Key Tools

| Tool | Use when |
|------|----------|
| `detect_changes` | Reviewing code changes — gives risk-scored analysis |
| `get_review_context` | Need source snippets for review — token-efficient |
| `get_impact_radius` | Understanding blast radius of a change |
| `get_affected_flows` | Finding which execution paths are impacted |
| `query_graph` | Tracing callers, callees, imports, tests, dependencies |
| `semantic_search_nodes` | Finding functions/classes by name or keyword |
| `get_architecture_overview` | Understanding high-level codebase structure |
| `refactor_tool` | Planning renames, finding dead code |

### Workflow

1. The graph auto-updates on file changes (via hooks).
2. Use `detect_changes` for code review.
3. Use `get_affected_flows` to understand impact.
4. Use `query_graph` pattern="tests_for" to check coverage.

---

## Verifying changes: run the CI pipeline locally

**Do not declare work done until you have run the same tasks CI runs.** `assembleDebug` succeeding
is not enough — CI runs `check`, lint, KSP-generated tests, and paparazzi screenshot verification,
any of which can fail when `assembleDebug` is green. Past incidents where this rule was skipped:
golden test resources stale after a KotlinPoet bump, lint baselines stale after a Compose/AGP bump,
paparazzi alpha incompatible with a Gradle bump, screenshot tests not discovering KSP-generated test
classes.

### Build environment

- **JDK 21 is required.** Paparazzi 2.0.0-alpha04 declares JVM 21+ as a build-runtime dependency, so
  a JDK 17 build fails at configuration time. Export before invoking gradle:
  ```sh
  export JAVA_HOME="$(/usr/libexec/java_home -v 21)"
  ```
  CI uses `actions/setup-java@v4` with `java-version: 21` in `.github/workflows/android.yml` — keep
  these in sync.
- The Gradle wrapper is at 9.5.0; do not invoke a system-installed gradle. Always use `./gradlew`.

### The three CI jobs and how to run them locally

`.github/workflows/android.yml` defines three jobs. Always run the first two before declaring work
done. The third needs an Android emulator; run it locally only if your change could plausibly affect
it (UI / instrumentation / showkase-browser-testing-*).

1. **`build` job — `./gradlew check`**
   ```sh
   ./gradlew check --stacktrace -x :showkase-screenshot-testing-paparazzi-sample:testDebugUnitTest
   ```
   The `-x` flag is mandatory and matches what CI does — paparazzi-2.0.0-alpha04 hooks into
   `testDebugUnitTest` via a Gradle 9-incompatible test-results API, so this task fails for the
   wrong reason if included. Until a newer paparazzi alpha ships, the paparazzi-sample's unit tests
   run only in the dedicated `paparazi-screenshot-tests` CI job.

   This runs: kotlin compilation, unit tests, lint, detekt, KSP, and the showkase-processor
   golden-file tests.

2. **`paparazi-screenshot-tests`
   job — `./gradlew :showkase-screenshot-testing-paparazzi-sample:verifyPaparazziDebug`**
   ```sh
   ./gradlew :showkase-screenshot-testing-paparazzi-sample:verifyPaparazziDebug --stacktrace
   ```
   This compares newly-rendered Compose screenshots against committed golden PNGs in
   `showkase-screenshot-testing-paparazzi-sample/src/test/snapshots/`. Marked
   `continue-on-error: true` in CI because of the alpha04/Gradle-9 incompatibility, but a passing
   local run is still meaningful evidence the change is safe. To record new goldens after
   intentional UI changes:
   `./gradlew :showkase-screenshot-testing-paparazzi-sample:recordPaparazziDebug`.

3. **`ui-testing` job — `./gradlew connectedCheck` + `./gradlew executeScreenshotTests`** (skip
   unless relevant)
   Requires a connected Android emulator/device. Use `reactivecircus/android-emulator-runner`
   semantics if you need to mirror CI exactly; otherwise leave it to CI.

### Common recovery patterns

- **Showkase processor golden mismatches** (after bumping KotlinPoet, KSP, or the processor itself):
  set `const val UPDATE_TEST_OUTPUTS = true` in
  `showkase-processor-testing/src/test/java/com/airbnb/android/showkase_processor_testing/BaseProcessorTest.kt`,
  run `./gradlew :showkase-processor-testing:testDebugUnitTest`, set the flag back to `false`,
  re-run to confirm. Each module's expected outputs live under
  `showkase-processor-testing/src/test/resources/<TestClass>/<test_method>/output/`.
- **Lint failures from new Compose/AGP checks** (NonObservableLocale, ContextCastToActivity,
  LocalContextGetResourceValueCall, etc.): every Android module has a
  `lint { baseline = file("lint-baseline.xml") }` block and a `lint-baseline.xml` file. To absorb
  new findings: `./gradlew updateLintBaseline`. Review the diff in `lint-baseline.xml` before
  committing — make sure the new entries are pre-existing patterns rather than regressions you just
  introduced.
- **Paparazzi screenshot mismatches** (after bumping Compose, paparazzi, or any UI-affecting
  dependency): `./gradlew :showkase-screenshot-testing-paparazzi-sample:recordPaparazziDebug` to
  refresh goldens, then commit the changed PNGs. Always sanity-check at least a few golden diffs
  visually before committing.
- **`testDebugUnitTest`: "no tests discovered"** in `:showkase-screenshot-testing-paparazzi-sample`:
  the Showkase KSP processor generates `MyPaparazziShowkaseScreenshotTestImpl.kt` at
  `build/generated/ksp/debugUnitTest/kotlin/...`. If that file is missing, KSP incremental cache is
  stale. Delete `showkase-screenshot-testing-paparazzi-sample/build/` and re-run, or pass
  `--rerun-tasks`.

### Verification checklist before declaring done

- [ ] `JAVA_HOME` points at a JDK 21 install.
- [ ] 
  `./gradlew check --stacktrace -x :showkase-screenshot-testing-paparazzi-sample:testDebugUnitTest`
  passes.
- [ ] `./gradlew :showkase-screenshot-testing-paparazzi-sample:verifyPaparazziDebug --stacktrace`
  passes (or failure is the known paparazzi/Gradle-9 `TestResultsProvider.hasOutput`
  incompatibility, which is `continue-on-error` in CI).
- [ ] If your change is UI- or instrumentation-adjacent, also run `./gradlew connectedCheck` against
  an emulator.
- [ ] No new entries in any `lint-baseline.xml` that aren't intentional.
- [ ] No new golden PNGs in `showkase-screenshot-testing-paparazzi-sample/src/test/snapshots/` that
  you didn't mean to add.

If any of the above can't be run (no JDK 21 installed, no emulator available), say so explicitly in
your summary rather than reporting success.
