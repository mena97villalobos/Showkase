package com.airbnb.android.showkase.processor

import com.airbnb.android.showkase.annotation.ShowkaseCodegenMetadata
import com.airbnb.android.showkase.annotation.ShowkaseColor
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.airbnb.android.showkase.annotation.ShowkaseMultiPreviewCodegenMetadata
import com.airbnb.android.showkase.annotation.ShowkaseRoot
import com.airbnb.android.showkase.annotation.ShowkaseRootCodegen
import com.airbnb.android.showkase.annotation.ShowkaseScreenshot
import com.airbnb.android.showkase.annotation.ShowkaseTypography
import com.airbnb.android.showkase.processor.exceptions.ShowkaseProcessorException
import com.airbnb.android.showkase.processor.logging.ShowkaseExceptionLogger
import com.airbnb.android.showkase.processor.logging.ShowkaseValidator
import com.airbnb.android.showkase.processor.models.ShowkaseMetadata
import com.airbnb.android.showkase.processor.models.ShowkaseMetadataType
import com.airbnb.android.showkase.processor.models.getCodegenMetadataTypes
import com.airbnb.android.showkase.processor.models.getShowkaseColorMetadata
import com.airbnb.android.showkase.processor.models.getShowkaseMetadata
import com.airbnb.android.showkase.processor.models.getShowkaseMetadataFromCustomAnnotation
import com.airbnb.android.showkase.processor.models.getShowkaseMetadataFromPreview
import com.airbnb.android.showkase.processor.models.getShowkaseTypographyMetadata
import com.airbnb.android.showkase.processor.utils.ensureConsistentOrdering
import com.airbnb.android.showkase.processor.utils.findAnnotationBySimpleName
import com.airbnb.android.showkase.processor.utils.getAnnotation
import com.airbnb.android.showkase.processor.utils.getAsBoolean
import com.airbnb.android.showkase.processor.utils.getAsInt
import com.airbnb.android.showkase.processor.utils.getAsString
import com.airbnb.android.showkase.processor.utils.getAsStringList
import com.airbnb.android.showkase.processor.utils.requireAnnotation
import com.airbnb.android.showkase.processor.writer.PaparazziShowkaseScreenshotTestWriter
import com.airbnb.android.showkase.processor.writer.ShowkaseBrowserProperties
import com.airbnb.android.showkase.processor.writer.ShowkaseBrowserPropertyWriter
import com.airbnb.android.showkase.processor.writer.ShowkaseBrowserWriter
import com.airbnb.android.showkase.processor.writer.ShowkaseBrowserWriter.Companion.CODEGEN_AUTOGEN_CLASS_NAME
import com.airbnb.android.showkase.processor.writer.ShowkaseCodegenMetadataWriter
import com.airbnb.android.showkase.processor.writer.ShowkaseExtensionFunctionsWriter
import com.airbnb.android.showkase.processor.writer.ShowkaseScreenshotTestWriter
import com.google.devtools.ksp.KspExperimental
import com.google.devtools.ksp.processing.Resolver
import com.google.devtools.ksp.processing.SymbolProcessor
import com.google.devtools.ksp.processing.SymbolProcessorEnvironment
import com.google.devtools.ksp.processing.SymbolProcessorProvider
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration

class ShowkaseProcessorProvider : SymbolProcessorProvider {
    override fun create(environment: SymbolProcessorEnvironment): SymbolProcessor {
        return ShowkaseProcessor(environment)
    }
}

class ShowkaseProcessor(
    kspEnvironment: SymbolProcessorEnvironment,
) : BaseProcessor(kspEnvironment) {

    private val exceptionLogger = ShowkaseExceptionLogger()
    private lateinit var currentResolver: Resolver
    private val showkaseValidator by lazy { ShowkaseValidator { currentResolver } }

    override fun processRound(resolver: Resolver) {
        currentResolver = resolver
        val componentMetadata = processComponentAnnotation(resolver)
        val colorMetadata = processColorAnnotation(resolver)
        val typographyMetadata = processTypographyAnnotation(resolver)
        processShowkaseMetadata(
            resolver = resolver,
            componentMetadata = componentMetadata,
            colorMetadata = colorMetadata,
            typographyMetadata = typographyMetadata
        )
    }

    override fun finish() {
        exceptionLogger.publishMessages(logger)
    }

    private fun processComponentAnnotation(resolver: Resolver): Set<ShowkaseMetadata.Component> {
        val showkaseComposablesMetadata = processShowkaseAnnotation(resolver)
        val previewComposablesMetadata = processPreviewAnnotation(resolver)

        val customPreviewFromClassPathMetadata = processCustomAnnotationFromClasspath(resolver)
        return (showkaseComposablesMetadata + previewComposablesMetadata + customPreviewFromClassPathMetadata)
            .dedupeAndSort()
            .toSet()
    }

    private fun processShowkaseAnnotation(
        resolver: Resolver
    ): Set<ShowkaseMetadata.Component> {
        val skipPrivatePreviews = options["skipPrivatePreviews"].toBoolean()
        return resolver.getSymbolsWithAnnotation(ShowkaseComposable::class.qualifiedName!!)
            .toList()
            .ensureConsistentOrdering()
            .mapNotNull { element ->
                if (showkaseValidator.checkElementIsAnnotationClass(element)) return@mapNotNull null
                val skipElement = showkaseValidator.validateComponentElementOrSkip(
                    element,
                    ShowkaseComposable::class.java.simpleName,
                    skipPrivatePreviews
                )
                if (skipElement) return@mapNotNull null
                getShowkaseMetadata(
                    element = element as KSFunctionDeclaration,
                    showkaseValidator = showkaseValidator,
                )
            }.flatten().mapNotNull { it }.toSet()
    }

    private fun processPreviewAnnotation(resolver: Resolver): Set<ShowkaseMetadata.Component> {
        val skipPrivatePreviews = options["skipPrivatePreviews"].toBoolean()
        val requireShowkaseComposableAnnotation =
            options["requireShowkaseComposableAnnotation"].toBoolean()

        if (requireShowkaseComposableAnnotation) return emptySet()

        return resolver.getSymbolsWithAnnotation(PREVIEW_CLASS_NAME)
            .toList()
            .ensureConsistentOrdering()
            .mapNotNull { element ->
                if (showkaseValidator.checkElementIsAnnotationClass(element)) {
                    // Writing preview data to a internal annotation to store values through
                    // processing rounds
                    ShowkaseBrowserWriter(codeGenerator).writeCustomAnnotationElementToMetadata(
                        element
                    )
                    return@mapNotNull processCustomAnnotation(
                        skipPrivatePreviews = skipPrivatePreviews,
                        resolver = resolver,
                        annotation = element
                    )
                }
                val skipElement = showkaseValidator.validateComponentElementOrSkip(
                    element,
                    PREVIEW_SIMPLE_NAME,
                    skipPrivatePreviews
                )
                if (skipElement) return@mapNotNull null
                getShowkaseMetadataFromPreview(
                    element = element as KSFunctionDeclaration,
                    showkaseValidator = showkaseValidator
                )
            }
            .flatten().mapNotNull { it }.toSet()
    }

    private fun processCustomAnnotation(
        skipPrivatePreviews: Boolean,
        resolver: Resolver,
        annotation: KSClassDeclaration? = null
    ): Set<ShowkaseMetadata.Component> {
        val supportedTypes = mutableListOf<String>()
        if (annotation != null) {
            annotation.qualifiedName?.asString()?.let { supportedTypes.add(it) }
        }
        val components = mutableSetOf<ShowkaseMetadata.Component>()

        supportedTypes.map { supportedType ->
            val annotatedElements = resolver.getSymbolsWithAnnotation(supportedType).toList()
            annotatedElements
                .map { annotatedElement ->
                    if (!showkaseValidator.checkElementIsAnnotationClass(annotatedElement)) {
                        val skipable = showkaseValidator.validateComponentElementOrSkip(
                            element = annotatedElement,
                            annotationName = supportedType,
                            skipPrivatePreviews = skipPrivatePreviews
                        )
                        if (!skipable) {
                            components.addAll(
                                getShowkaseMetadataFromCustomAnnotation(
                                    element = annotatedElement as KSFunctionDeclaration,
                                    showkaseValidator = showkaseValidator,
                                    supportedType.getCustomAnnotationSimpleName(),
                                ).toSet()
                            )
                        }
                    }
                }
        }
        return components
    }

    private fun String.getCustomAnnotationSimpleName(): String {
        return this.split(".").last()
    }

    @OptIn(KspExperimental::class)
    private fun processCustomAnnotationFromClasspath(resolver: Resolver): Set<ShowkaseMetadata.Component> {
        // In this function we are checking generated classpath for MultiPreview codegen annotations.
        // We also check the current module if there is any composables that are annotated with the qualified name
        // from the annotation from classpath. We use the fields from the classpath annotation to build
        // common data for the ShowkaseMetadata.

        val skipPrivatePreviews = options["skipPrivatePreviews"] == "true"
        // Supported annotations from classpath
        val supportedCustomPreview = mutableSetOf<ShowkaseMultiPreviewCodegenMetadata>()
        resolver.getDeclarationsFromPackage(CODEGEN_PACKAGE_NAME)
            .filterIsInstance<KSClassDeclaration>()
            .toList()
            .ensureConsistentOrdering()
            .flatMap { it.declarations.toList() }
            .mapNotNull {
                return@mapNotNull when (
                    val annotation = it.getAnnotation(ShowkaseMultiPreviewCodegenMetadata::class)
                ) {
                    null -> {
                        null
                    }

                    else -> {
                        val codeGenAnnotation = ShowkaseMultiPreviewCodegenMetadata(
                            previewName = annotation.getAsString("previewName"),
                            previewGroup = annotation.getAsString("previewGroup"),
                            supportTypeQualifiedName = annotation.getAsString("supportTypeQualifiedName"),
                            packageName = annotation.getAsString("packageName"),
                            showkaseWidth = annotation.getAsInt("showkaseWidth"),
                            showkaseHeight = annotation.getAsInt("showkaseHeight"),
                        )
                        supportedCustomPreview.add(codeGenAnnotation)
                    }
                }
            }
        val components = mutableSetOf<ShowkaseMetadata.Component>()
        supportedCustomPreview
            .mapIndexed { index: Int, customPreviewMetadata: ShowkaseMultiPreviewCodegenMetadata ->
                resolver
                    .getSymbolsWithAnnotation(customPreviewMetadata.supportTypeQualifiedName)
                    .toList()
                    .ensureConsistentOrdering()
                    .mapIndexed elementRoot@{ elementIndex, xElement ->
                        val skippable = showkaseValidator.validateComponentElementOrSkip(
                            xElement,
                            customPreviewMetadata.supportTypeQualifiedName,
                            skipPrivatePreviews = skipPrivatePreviews
                        )
                        if (!skippable) {
                            components.add(
                                getShowkaseMetadata(
                                    xElement = xElement as KSFunctionDeclaration,
                                    customPreviewMetadata = customPreviewMetadata,
                                    elementIndex = elementIndex,
                                    index = index,
                                    showkaseValidator = showkaseValidator
                                )
                            )
                        }
                    }
            }
        return components
    }

    private fun writeMetadataFile(
        componentMetadata: Set<ShowkaseMetadata.Component>,
        colorMetadata: Set<ShowkaseMetadata>,
        typographyMetadata: Set<ShowkaseMetadata>,
    ): ShowkaseBrowserProperties {
        val aggregateMetadataList = componentMetadata + colorMetadata + typographyMetadata
        if (aggregateMetadataList.isEmpty()) return ShowkaseBrowserProperties()

        ShowkaseCodegenMetadataWriter(codeGenerator).apply {
            generateShowkaseCodegenFunctions(aggregateMetadataList)
        }
        ShowkaseBrowserPropertyWriter(codeGenerator).apply {
            return generateMetadataPropertyFiles(
                componentMetadata = componentMetadata,
                colorMetadata = colorMetadata,
                typographyMetadata = typographyMetadata,
            )
        }
    }

    private fun Collection<ShowkaseMetadata.Component>.dedupeAndSort() = this.distinctBy {
        // It's possible that a composable annotation is annotated with both Preview &
        // ShowkaseComposable(especially if we add more functionality to Showkase and they diverge
        // in the customizations that they offer). In that scenario, its important to dedupe the
        // composables as they will be processed across both the rounds. We first ensure that
        // only distict method's are passed onto the next round. We do this by deduping on
        // the combination of packageName, the wrapper class when available(otherwise it
        // will be null) & the methodName.
        if (it.componentIndex != null) {
            "${it.packageName}_${it.enclosingClassName}_${it.elementName}_${it.componentIndex}"
        } else {

            "${it.packageName}_${it.enclosingClassName}_${it.elementName}"
        }
    }
        .distinctBy {
            // We also ensure that the component groupName and the component name are unique so
            // that they don't show up twice in the browser app. This also de-duplicates based
            // on the fully qualified function name to support categorization with additional
            // fields (e.g. tags, extraMetadata, etc) on custom browsers.
            if (it.componentIndex != null) {
                "${it.fqPrefix}_${it.showkaseName}_${it.showkaseGroup}_${it.showkaseStyleName}_${it.componentIndex}"
            } else {
                "${it.fqPrefix}_${it.showkaseName}_${it.showkaseGroup}_${it.showkaseStyleName}"
            }
        }
        .sortedBy {
            it.fqPrefix
        }

    private fun processColorAnnotation(resolver: Resolver): Set<ShowkaseMetadata> {
        return resolver.getSymbolsWithAnnotation(ShowkaseColor::class.qualifiedName!!)
            .toList()
            .ensureConsistentOrdering()
            .map { element ->
                showkaseValidator.validateColorElement(
                    element,
                    ShowkaseColor::class.java.simpleName
                )
                getShowkaseColorMetadata(element as KSPropertyDeclaration, showkaseValidator)
            }.toSet()
    }

    private fun processTypographyAnnotation(
        resolver: Resolver,
    ): Set<ShowkaseMetadata> {
        val textStyleType by lazy {
            resolver.getClassDeclarationByName(resolver.getKSNameFromString(TYPE_STYLE_CLASS_NAME))
                ?.asStarProjectedType()
                ?: error("TextStyle type not found")
        }

        return resolver.getSymbolsWithAnnotation(ShowkaseTypography::class.qualifiedName!!)
            .toList()
            .ensureConsistentOrdering()
            .map { element ->
                showkaseValidator.validateTypographyElement(
                    element,
                    ShowkaseTypography::class.java.simpleName,
                    textStyleType
                )
                getShowkaseTypographyMetadata(element as KSPropertyDeclaration, showkaseValidator)
            }.toSet()
    }

    private fun processShowkaseMetadata(
        resolver: Resolver,
        componentMetadata: Set<ShowkaseMetadata.Component>,
        colorMetadata: Set<ShowkaseMetadata>,
        typographyMetadata: Set<ShowkaseMetadata>
    ) {
        // Showkase root annotation
        val rootElement = getShowkaseRootElement(resolver)

        // Showkase test annotation
        val (screenshotTestElement, screenshotTestType) = getShowkaseScreenshotTestElement(
            resolver
        )

        var showkaseBrowserProperties = ShowkaseBrowserProperties()

        // If root element is not present in this module, it means that we only need to write
        // the metadata file for this module so that the root module can use this info to
        // include the composables from this module into the final codegen file.
        val currentShowkaseBrowserProperties =
            writeMetadataFile(componentMetadata, colorMetadata, typographyMetadata)

        if (rootElement != null) {
            // This is the module that should aggregate all the other metadata files and
            // also use the showkaseMetadata set from the current round to write the final file.
            showkaseBrowserProperties = writeShowkaseFiles(
                rootElement,
                currentShowkaseBrowserProperties
            )
        }

        if (screenshotTestElement != null && screenshotTestType != null) {
            // Generate screenshot test file if ShowkaseScreenshotTest is present in the root module
            writeScreenshotTestFiles(
                screenshotTestElement, screenshotTestType, rootElement,
                showkaseBrowserProperties
            )
        }
    }

    private fun getShowkaseRootElement(
        resolver: Resolver,
    ): KSClassDeclaration? {
        val showkaseRootElements = resolver.getSymbolsWithAnnotation(ShowkaseRoot::class.qualifiedName!!)
            .toList()
            .ensureConsistentOrdering().toSet()
        showkaseValidator.validateShowkaseRootElement(showkaseRootElements)
        return showkaseRootElements.singleOrNull() as KSClassDeclaration?
    }

    private fun getShowkaseScreenshotTestElement(
        resolver: Resolver
    ): Pair<KSClassDeclaration?, ScreenshotTestType?> {
        val testElements = resolver.getSymbolsWithAnnotation(ShowkaseScreenshot::class.qualifiedName!!)
            .toList()
            .ensureConsistentOrdering()
            .filterIsInstance<KSClassDeclaration>()
            .toSet()
        val screenshotTestType =
            showkaseValidator.validateShowkaseTestElement(testElements)
        return testElements.singleOrNull() to screenshotTestType
    }

    private fun writeShowkaseFiles(
        rootElement: KSClassDeclaration,
        currentShowkaseBrowserProperties: ShowkaseBrowserProperties,
    ): ShowkaseBrowserProperties {
        val generatedShowkaseMetadataOnClasspath =
            getShowkaseCodegenMetadataOnClassPath(currentResolver)
        val classpathComponentsWithoutParameter = generatedShowkaseMetadataOnClasspath.filter {
            it.type == ShowkaseGeneratedMetadataType.COMPONENTS_WITHOUT_PARAMETER
        }
        val classpathComponentsWithParameter = generatedShowkaseMetadataOnClasspath.filter {
            it.type == ShowkaseGeneratedMetadataType.COMPONENTS_WITH_PARAMETER
        }
        val classpathColors =
            generatedShowkaseMetadataOnClasspath.filter {
                it.type == ShowkaseGeneratedMetadataType.COLOR
            }
        val classpathTypography =
            generatedShowkaseMetadataOnClasspath.filter {
                it.type == ShowkaseGeneratedMetadataType.TYPOGRAPHY
            }

        val classpathShowkaseBrowserProperties = ShowkaseBrowserProperties(
            componentsWithoutPreviewParameters = classpathComponentsWithoutParameter,
            componentsWithPreviewParameters = classpathComponentsWithParameter,
            colors = classpathColors,
            typography = classpathTypography
        )
        val allShowkaseBrowserProperties =
            currentShowkaseBrowserProperties + classpathShowkaseBrowserProperties
        writeShowkaseBrowserFiles(rootElement, allShowkaseBrowserProperties)

        return allShowkaseBrowserProperties
    }

    private fun writeScreenshotTestFiles(
        screenshotTestElement: KSClassDeclaration,
        screenshotTestType: ScreenshotTestType,
        rootElement: KSClassDeclaration?,
        showkaseBrowserProperties: ShowkaseBrowserProperties,
    ) {
        val testClassName = screenshotTestElement.simpleName.asString()
        val screenshotTestPackageName = screenshotTestElement.packageName.asString()

        // Parse the showkase root class that was specified in @ShowkaseScreenshot
        val specifiedRootClassTypeElement = getSpecifiedRootTypeElement(screenshotTestElement)

        // Get the package of the specified root module. We need this to ensure that we use the
        // Showkase.getMetadata metadata from that package.
        val rootModulePackageName = specifiedRootClassTypeElement.packageName.asString()

        val showkaseTestMetadata = if (rootElement != null &&
            specifiedRootClassTypeElement.simpleName.asString() == rootElement.simpleName.asString()
        ) {
            // If the specified root element is the being processed in the current processing round,
            // use it directly instead of looking for it in the class path. This is because it won't
            // be availabe in the classpath just yet.
            ShowkaseTestMetadata(
                componentsSize = showkaseBrowserProperties.componentsWithoutPreviewParameters.size,
                showkaseBrowserProperties.colors.size,
                showkaseBrowserProperties.typography.size,
            )
        } else {
            getShowkaseRootCodegenOnClassPath(specifiedRootClassTypeElement)?.let { showkaseRootCodegenAnnotation ->
                // Else if we were able to find the specified root element in the classpath, we will use
                // the metadata from there instead.
                ShowkaseTestMetadata(
                    componentsSize = showkaseRootCodegenAnnotation.numComposablesWithoutPreviewParameter,
                    colorsSize = showkaseRootCodegenAnnotation.numColors,
                    typographySize = showkaseRootCodegenAnnotation.numTypography
                )
            } ?: throw ShowkaseProcessorException(
                "Showkase was not able to find the root class that you" +
                        "passed to @ShowkaseScreenshot. Make sure that you have configured Showkase correctly.",
                screenshotTestElement
            )
        }

        writeShowkaseScreenshotTestFile(
            screenshotTestType,
            showkaseTestMetadata.componentsSize,
            showkaseTestMetadata.colorsSize,
            showkaseTestMetadata.typographySize,
            screenshotTestPackageName,
            rootModulePackageName,
            testClassName,
        )
    }

    private fun getSpecifiedRootTypeElement(screenshotTestElement: KSClassDeclaration): KSClassDeclaration {
        val rootShowkaseClassType = screenshotTestElement.requireAnnotation(ShowkaseScreenshot::class)
            .let { ann ->
                val arg = ann.arguments.firstOrNull { it.name?.asString() == "rootShowkaseClass" }
                    ?: ann.defaultArguments.firstOrNull { it.name?.asString() == "rootShowkaseClass" }
                arg?.value as? com.google.devtools.ksp.symbol.KSType
            }
        return (rootShowkaseClassType?.declaration as? KSClassDeclaration)
            ?: throw ShowkaseProcessorException(
                "Unable to get rootShowkaseClass in ShowkaseScreenshot annotation",
                screenshotTestElement
            )
    }

    @OptIn(KspExperimental::class)
    private fun getShowkaseCodegenMetadataOnClassPath(resolver: Resolver):
            Set<ShowkaseGeneratedMetadata> {
        return resolver.getDeclarationsFromPackage(CODEGEN_PACKAGE_NAME)
            .filterIsInstance<KSClassDeclaration>()
            .toList()
            .ensureConsistentOrdering()
            .flatMap { it.declarations.toList() }
            .mapNotNull { element ->
                val codegenMetadataAnnotation =
                    element.getAnnotation(ShowkaseCodegenMetadata::class)
                when {
                    codegenMetadataAnnotation == null -> null
                    else -> element to codegenMetadataAnnotation
                }
            }
            .map {
                it.second.toShowkaseGeneratedMetadata(it.first)
            }
            .toSet()
    }

    private fun KSAnnotation.toShowkaseGeneratedMetadata(element: KSAnnotated): ShowkaseGeneratedMetadata {
        val (_, previewParameterClassType) = getCodegenMetadataTypes()

        // The box is needed to get all Class values, primitives can be accessed dirctly
        val type = ShowkaseMetadataType.valueOf(getAsString("showkaseMetadataType"))

        return ShowkaseGeneratedMetadata(
            element = element,
            propertyName = getAsString("generatedPropertyName"),
            propertyPackage = getAsString("packageName"),
            type = when (type) {
                ShowkaseMetadataType.COLOR -> ShowkaseGeneratedMetadataType.COLOR
                ShowkaseMetadataType.TYPOGRAPHY -> ShowkaseGeneratedMetadataType.TYPOGRAPHY
                ShowkaseMetadataType.COMPONENT -> if (previewParameterClassType != null) {
                    ShowkaseGeneratedMetadataType.COMPONENTS_WITH_PARAMETER
                } else {
                    ShowkaseGeneratedMetadataType.COMPONENTS_WITHOUT_PARAMETER
                }
            },
            group = getAsString("showkaseGroup"),
            name = getAsString("showkaseName"),
            isDefaultStyle = getAsBoolean("isDefaultStyle"),
            tags = getAsStringList("tags"),
            extraMetadata = getAsStringList("extraMetadata")
        )
    }

    private fun getShowkaseRootCodegenOnClassPath(
        specifiedRootClassTypeElement: KSClassDeclaration
    ): ShowkaseRootCodegen? {
        val qName = "${specifiedRootClassTypeElement.qualifiedName?.asString()}$CODEGEN_AUTOGEN_CLASS_NAME"
        return currentResolver
            .getClassDeclarationByName(currentResolver.getKSNameFromString(qName))
            ?.findAnnotationBySimpleName(ShowkaseRootCodegen::class.simpleName!!)
            ?.let { xAnnotation ->
                ShowkaseRootCodegen(
                    numComposablesWithoutPreviewParameter =
                        xAnnotation.getAsInt("numComposablesWithoutPreviewParameter"),
                    numComposablesWithPreviewParameter = xAnnotation.getAsInt("numComposablesWithPreviewParameter"),
                    numColors = xAnnotation.getAsInt("numColors"),
                    numTypography = xAnnotation.getAsInt("numTypography")
                )
            }
    }

    private fun writeShowkaseBrowserFiles(
        rootElement: KSClassDeclaration,
        allShowkaseBrowserProperties: ShowkaseBrowserProperties,
    ) {
        if (allShowkaseBrowserProperties.isEmpty()) return
        val rootModuleClassName = rootElement.simpleName.asString()
        val rootModulePackageName = rootElement.packageName.asString()

        showkaseValidator.validateShowkaseComponents(allShowkaseBrowserProperties)

        ShowkaseBrowserWriter(codeGenerator).apply {
            generateShowkaseBrowserFile(
                allShowkaseBrowserProperties,
                rootModulePackageName,
                rootModuleClassName
            )
        }

        ShowkaseExtensionFunctionsWriter(codeGenerator).apply {
            generateShowkaseExtensionFunctions(
                rootModulePackageName = rootModulePackageName,
                rootModuleClassName = rootModuleClassName,
                rootElement = rootElement
            )
        }
    }

    @Suppress("LongParameterList")
    private fun writeShowkaseScreenshotTestFile(
        screenshotTestType: ScreenshotTestType,
        componentsSize: Int,
        colorsSize: Int,
        typographySize: Int,
        screenshotTestPackageName: String,
        rootModulePackageName: String,
        testClassName: String,
    ) {
        when (screenshotTestType) {
            // We only handle composables without preview parameter for screenshots. This is because
            // there's no way to get information about how many previews are dynamically generated using
            // preview parameter as it happens on run time and our codegen doesn't get enough information
            // to be able to predict how many extra composables the preview parameters extrapolate to.
            // TODO(vinaygaba): Add screenshot testing support for composabable with preview
            //  parameters as well
            ScreenshotTestType.SHOWKASE -> {
                ShowkaseScreenshotTestWriter(codeGenerator).apply {
                    generateScreenshotTests(
                        componentsSize,
                        colorsSize,
                        typographySize,
                        screenshotTestPackageName,
                        rootModulePackageName,
                        testClassName
                    )
                }
            }

            ScreenshotTestType.PAPARAZZI_SHOWKASE -> {
                PaparazziShowkaseScreenshotTestWriter(codeGenerator).apply {
                    generateScreenshotTests(
                        screenshotTestPackageName,
                        rootModulePackageName,
                        testClassName
                    )
                }
            }
        }
    }

    private data class ShowkaseTestMetadata(
        val componentsSize: Int,
        val colorsSize: Int,
        val typographySize: Int,
    )

    companion object {
        internal const val COMPOSABLE_SIMPLE_NAME = "Composable"
        internal const val PREVIEW_CLASS_NAME = "androidx.compose.ui.tooling.preview.Preview"
        internal const val PREVIEW_SIMPLE_NAME = "Preview"
        internal const val PREVIEW_PARAMETER_SIMPLE_NAME = "PreviewParameter"
        internal const val TYPE_STYLE_CLASS_NAME = "androidx.compose.ui.text.TextStyle"
        internal const val CODEGEN_PACKAGE_NAME = "com.airbnb.android.showkase"
    }
}

internal data class ShowkaseGeneratedMetadata(
    val propertyName: String,
    val propertyPackage: String,
    val type: ShowkaseGeneratedMetadataType,
    val element: KSAnnotated,
    val group: String,
    val name: String,
    // This property is only used for components
    val isDefaultStyle: Boolean = false,
    val tags: List<String> = emptyList(),
    val extraMetadata: List<String> = emptyList()
)

internal enum class ShowkaseGeneratedMetadataType {
    COMPONENTS_WITH_PARAMETER,
    COMPONENTS_WITHOUT_PARAMETER,
    COLOR,
    TYPOGRAPHY
}

internal enum class ScreenshotTestType {
    SHOWKASE,
    PAPARAZZI_SHOWKASE
}
