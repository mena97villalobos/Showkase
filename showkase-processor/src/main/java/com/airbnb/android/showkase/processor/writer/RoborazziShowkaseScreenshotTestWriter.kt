package com.airbnb.android.showkase.processor.writer

import com.google.devtools.ksp.processing.CodeGenerator
import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.CodeBlock
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.TypeSpec
import com.squareup.kotlinpoet.ksp.writeTo

/**
 * Generates the Roborazzi-backed screenshot test class for classes annotated with
 * `@ShowkaseScreenshot` that implement [RoborazziShowkaseScreenshotTest].
 *
 * The generated test:
 * - runs under [RobolectricTestRunner] with `@GraphicsMode(NATIVE)` and `@Config(sdk = 33)`
 *   (Robolectric requires these at class level; SDK cannot change at runtime),
 * - iterates every Showkase preview (components + colors + typography) inside a single `@Test`,
 *   collecting per-preview failures so all diffs surface in one run,
 * - reads the device qualifier from the user's `companion object` at runtime via
 *   `RuntimeEnvironment.setQualifiers(...)`, so users can override qualifiers without forking the
 *   generator,
 * - writes goldens to `src/test/snapshots/roborazzi/<id>.png` and compares against them.
 */
class RoborazziShowkaseScreenshotTestWriter(private val codeGenerator: CodeGenerator) {

    internal fun generateScreenshotTests(
        screenshotTestPackageName: String,
        rootModulePackageName: String,
        testClassName: String,
    ) {
        val implClassName = "${testClassName}Impl"
        val userTestClass = ClassName(screenshotTestPackageName, testClassName)
        val fileBuilder = getFileBuilder(screenshotTestPackageName, implClassName)

        fileBuilder
            .addImport(rootModulePackageName, "getMetadata")
            .addType(
                TypeSpec.classBuilder(implClassName)
                    .addAnnotation(runWithAndroidJUnit4())
                    .addAnnotation(graphicsModeNative())
                    .addAnnotation(configSdk())
                    .addFunction(testAllPreviewsFunction(userTestClass))
                    .build(),
            )

        fileBuilder.build().writeTo(codeGenerator, aggregating = true)
    }

    private fun runWithAndroidJUnit4(): AnnotationSpec =
        AnnotationSpec.builder(ShowkaseScreenshotTestWriter.RUNWITH_CLASSNAME)
            .addMember("%T::class", ROBOLECTRIC_TEST_RUNNER)
            .build()

    private fun graphicsModeNative(): AnnotationSpec =
        AnnotationSpec.builder(GRAPHICS_MODE_CLASS_NAME)
            .addMember("%T.Mode.NATIVE", GRAPHICS_MODE_CLASS_NAME)
            .build()

    private fun configSdk(): AnnotationSpec =
        AnnotationSpec.builder(CONFIG_CLASS_NAME)
            .addMember("sdk = [33]")
            .build()

    /**
     * Emits a single `@Test fun test_all_previews()` that iterates every Showkase preview and
     * snapshots each. We can't use [ParameterizedRobolectricTestRunner] because its `@Parameters`
     * factory runs before Robolectric's sandbox is initialized, and `Showkase.getMetadata()`'s
     * reflective lookup of the generated `*RootModuleCodegen` class returns empty in that state.
     *
     * Per-preview failure isolation is sacrificed for reliability — a diff on one preview still
     * lets us continue to the next via `captureRoboImage`'s default behaviour (it throws on diff,
     * but we wrap each call in `runCatching` so we collect all failures before failing the test).
     */
    private fun testAllPreviewsFunction(userTestClass: ClassName): FunSpec =
        FunSpec.builder("test_all_previews")
            .addAnnotation(ShowkaseScreenshotTestWriter.JUNIT_TEST)
            .addCode(
                CodeBlock.builder()
                    .addStatement(
                        "%T.setQualifiers(%T.qualifiers())",
                        RUNTIME_ENVIRONMENT_CLASS_NAME,
                        userTestClass,
                    )
                    .addStatement(
                        "val metadata = %T.getMetadata()",
                        ShowkaseExtensionFunctionsWriter.SHOWKASE_OBJECT_CLASS_NAME,
                    )
                    .addStatement(
                        "val previews = mutableListOf<%T>()",
                        ROBORAZZI_TEST_PREVIEW_CLASS_NAME,
                    )
                    .addStatement(
                        "metadata.componentList.mapTo(previews, ::%T)",
                        COMPONENT_TEST_PREVIEW_CLASS_NAME,
                    )
                    .addStatement(
                        "metadata.colorList.mapTo(previews, ::%T)",
                        COLOR_TEST_PREVIEW_CLASS_NAME,
                    )
                    .addStatement(
                        "metadata.typographyList.mapTo(previews, ::%T)",
                        TYPOGRAPHY_TEST_PREVIEW_CLASS_NAME,
                    )
                    .addStatement("val outputDir = %S", "src/test/snapshots/roborazzi")
                    .addStatement("val options = %T.roborazziOptions()", userTestClass)
                    .addStatement("val failures = mutableListOf<String>()")
                    .beginControlFlow("previews.forEach { preview ->")
                    .beginControlFlow("runCatching")
                    .addStatement(
                        "%T(filePath = %P, roborazziOptions = options) { preview.Content() }",
                        CAPTURE_ROBO_IMAGE_CLASS_NAME,
                        "\${outputDir}/\${preview.id}.png",
                    )
                    .endControlFlow()
                    .beginControlFlow(".onFailure { e ->")
                    .addStatement("failures += %P", "\${preview.id}: \${e.message}")
                    .endControlFlow()
                    .endControlFlow()
                    .beginControlFlow("if (failures.isNotEmpty())")
                    .addStatement(
                        "error(\"Roborazzi screenshot failures (\${failures.size}):\\n\" + " +
                                "failures.joinToString(\"\\n\"))"
                    )
                    .endControlFlow()
                    .build(),
            )
            .build()

    companion object {
        private const val ROBOLECTRIC = "org.robolectric"
        private val ROBOLECTRIC_TEST_RUNNER = ClassName(
            ROBOLECTRIC,
            "RobolectricTestRunner",
        )
        private val GRAPHICS_MODE_CLASS_NAME = ClassName(
            "$ROBOLECTRIC.annotation",
            "GraphicsMode",
        )
        private val CONFIG_CLASS_NAME = ClassName(
            "$ROBOLECTRIC.annotation",
            "Config",
        )
        private val RUNTIME_ENVIRONMENT_CLASS_NAME = ClassName(
            ROBOLECTRIC,
            "RuntimeEnvironment",
        )

        private const val ROBORAZZI = "com.github.takahirom.roborazzi"
        private val CAPTURE_ROBO_IMAGE_CLASS_NAME = ClassName(ROBORAZZI, "captureRoboImage")

        private const val ROBORAZZI_SHOWKASE_PACKAGE =
            "com.airbnb.android.showkase.screenshot.testing.roborazzi"
        private val ROBORAZZI_TEST_PREVIEW_CLASS_NAME = ClassName(
            ROBORAZZI_SHOWKASE_PACKAGE,
            "RoborazziShowkaseTestPreview",
        )
        private val COMPONENT_TEST_PREVIEW_CLASS_NAME = ClassName(
            ROBORAZZI_SHOWKASE_PACKAGE,
            "ComponentRoborazziShowkaseTestPreview",
        )
        private val COLOR_TEST_PREVIEW_CLASS_NAME = ClassName(
            ROBORAZZI_SHOWKASE_PACKAGE,
            "ColorRoborazziShowkaseTestPreview",
        )
        private val TYPOGRAPHY_TEST_PREVIEW_CLASS_NAME = ClassName(
            ROBORAZZI_SHOWKASE_PACKAGE,
            "TypographyRoborazziShowkaseTestPreview",
        )
    }
}
