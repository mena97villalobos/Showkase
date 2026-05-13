package com.airbnb.android.showkase.processor.models

import com.airbnb.android.showkase.annotation.ScreenshotCaptureConfig
import com.airbnb.android.showkase.annotation.ScreenshotCaptureType
import com.airbnb.android.showkase.annotation.ScreenshotConfig
import com.airbnb.android.showkase.annotation.ShowkaseColor
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import com.airbnb.android.showkase.annotation.ShowkaseDialog
import com.airbnb.android.showkase.annotation.ShowkaseMultiPreviewCodegenMetadata
import com.airbnb.android.showkase.annotation.ShowkaseTypography
import com.airbnb.android.showkase.processor.ShowkaseProcessor.Companion.PREVIEW_PARAMETER_SIMPLE_NAME
import com.airbnb.android.showkase.processor.ShowkaseProcessor.Companion.PREVIEW_SIMPLE_NAME
import com.airbnb.android.showkase.processor.exceptions.ShowkaseProcessorException
import com.airbnb.android.showkase.processor.logging.ShowkaseValidator
import com.airbnb.android.showkase.processor.utils.annotationDeclaration
import com.airbnb.android.showkase.processor.utils.argByNameOrNull
import com.airbnb.android.showkase.processor.utils.findAnnotationBySimpleName
import com.airbnb.android.showkase.processor.utils.getAnnotation
import com.airbnb.android.showkase.processor.utils.getAnnotations
import com.airbnb.android.showkase.processor.utils.getAsBoolean
import com.airbnb.android.showkase.processor.utils.getAsEnum
import com.airbnb.android.showkase.processor.utils.getAsInt
import com.airbnb.android.showkase.processor.utils.getAsIntList
import com.airbnb.android.showkase.processor.utils.getAsString
import com.airbnb.android.showkase.processor.utils.getAsStringList
import com.airbnb.android.showkase.processor.utils.getAsType
import com.airbnb.android.showkase.processor.utils.getAsTypeList
import com.airbnb.android.showkase.processor.utils.requireAnnotation
import com.airbnb.android.showkase.processor.utils.requireAnnotationBySimpleName
import com.google.devtools.ksp.symbol.ClassKind
import com.google.devtools.ksp.symbol.KSAnnotated
import com.google.devtools.ksp.symbol.KSAnnotation
import com.google.devtools.ksp.symbol.KSClassDeclaration
import com.google.devtools.ksp.symbol.KSDeclaration
import com.google.devtools.ksp.symbol.KSFunctionDeclaration
import com.google.devtools.ksp.symbol.KSPropertyDeclaration
import com.google.devtools.ksp.symbol.KSType
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.ksp.toClassName
import com.squareup.kotlinpoet.ksp.toTypeName
import java.util.Locale

@Suppress("LongParameterList")
internal sealed class ShowkaseMetadata {
    abstract val element: KSAnnotated
    abstract val packageName: String
    abstract val packageSimpleName: String
    abstract val elementName: String
    abstract val showkaseName: String
    abstract val showkaseGroup: String
    abstract val showkaseKDoc: String
    abstract val enclosingClassName: ClassName?
    abstract val insideWrapperClass: Boolean
    abstract val insideObject: Boolean

    /** A fully qualified prefix for use when de-duplicating components. **/
    val fqPrefix: String
        get() = enclosingClassName?.let { "${it}_$elementName" } ?: "${packageName}_$elementName"

    data class Component(
        override val element: KSAnnotated,
        override val packageName: String,
        override val packageSimpleName: String,
        override val elementName: String,
        override val showkaseName: String,
        override val showkaseGroup: String,
        override val showkaseKDoc: String,
        override val enclosingClassName: ClassName? = null,
        override val insideWrapperClass: Boolean = false,
        override val insideObject: Boolean = false,
        val componentIndex: Int? = null,
        val showkaseWidthDp: Int? = null,
        val showkaseHeightDp: Int? = null,
        val previewParameterProviderType: TypeName? = null,
        val previewParameterName: String? = null,
        val showkaseStyleName: String? = null,
        val isDefaultStyle: Boolean = false,
        val tags: List<String> = emptyList(),
        val extraMetadata: List<String> = emptyList(),
        val screenshotConfig: ScreenshotConfig = ScreenshotConfig.SingleStaticImage,
        val isDialog: Boolean = false,
        val dialogButtonText: String = "",
        val dialogHideButtonText: String = "",
    ) : ShowkaseMetadata()

    data class Color(
        override val element: KSAnnotated,
        override val packageSimpleName: String,
        override val packageName: String,
        override val elementName: String,
        override val showkaseName: String,
        override val showkaseGroup: String,
        override val showkaseKDoc: String,
        override val enclosingClassName: ClassName? = null,
        override val insideWrapperClass: Boolean = false,
        override val insideObject: Boolean = false,
    ) : ShowkaseMetadata()

    data class Typography(
        override val element: KSAnnotated,
        override val packageSimpleName: String,
        override val packageName: String,
        override val elementName: String,
        override val showkaseName: String,
        override val showkaseGroup: String,
        override val showkaseKDoc: String,
        override val enclosingClassName: ClassName? = null,
        override val insideWrapperClass: Boolean = false,
        override val insideObject: Boolean = false,
    ) : ShowkaseMetadata()
}

internal enum class ShowkaseFunctionType {
    TOP_LEVEL,
    INSIDE_CLASS,
    INSIDE_OBJECT,
    INSIDE_COMPANION_OBJECT,
}

internal fun ShowkaseFunctionType.insideObject() = this == ShowkaseFunctionType.INSIDE_OBJECT ||
        this == ShowkaseFunctionType.INSIDE_COMPANION_OBJECT

internal enum class ShowkaseMetadataType {
    COMPONENT,
    COLOR,
    TYPOGRAPHY
}

internal fun KSAnnotation.getCodegenMetadataTypes(): Pair<KSType?, KSType?> {
    return Pair(
        getAsTypeList("enclosingClass").firstOrNull(),
        getAsTypeList("previewParameterClass").firstOrNull()
    )
}

private fun Int.parseAnnotationProperty() = when (this) {
    -1 -> null
    else -> this
}

internal fun getShowkaseMetadata(
    element: KSFunctionDeclaration,
    showkaseValidator: ShowkaseValidator
): List<ShowkaseMetadata.Component?> {
    val showkaseAnnotations = element.getAnnotations(ShowkaseComposable::class)
    val elementName = element.simpleName.asString()

    val commonMetadata = element.extractCommonMetadata(showkaseValidator)
    val previewParameterMetadata = element.getPreviewParameterMetadata()

    return showkaseAnnotations.mapNotNull { annotation ->
        // If this component was configured to be skipped, return early
        if (annotation.getAsBoolean("skip")) return@mapNotNull null

        val showkaseName = getShowkaseName(annotation.getAsString("name"), elementName)
        val showkaseGroup = getShowkaseGroup(
            annotation.getAsString("group"),
            commonMetadata.enclosingClass,
        )
        val isDefaultStyle = annotation.getAsBoolean("defaultStyle")
        val showkaseStyleName = getShowkaseStyleName(annotation.getAsString("styleName"), isDefaultStyle)
        val tags = annotation.getAsStringList("tags")
        val extraMetadata = annotation.getAsStringList("extraMetadata")
        val screenshotConfig = screenshotConfigFrom(annotation)
        ShowkaseMetadata.Component(
            packageSimpleName = commonMetadata.moduleName,
            packageName = commonMetadata.packageName,
            enclosingClassName = commonMetadata.enclosingClassName,
            elementName = elementName,
            showkaseName = showkaseName,
            showkaseGroup = showkaseGroup,
            showkaseStyleName = showkaseStyleName,
            showkaseWidthDp = annotation.getAsInt("widthDp").parseAnnotationProperty(),
            showkaseHeightDp = annotation.getAsInt("heightDp").parseAnnotationProperty(),
            insideObject = commonMetadata.showkaseFunctionType.insideObject(),
            insideWrapperClass = commonMetadata.showkaseFunctionType == ShowkaseFunctionType.INSIDE_CLASS,
            element = element,
            showkaseKDoc = commonMetadata.kDoc,
            previewParameterName = previewParameterMetadata?.first,
            previewParameterProviderType = previewParameterMetadata?.second,
            isDefaultStyle = isDefaultStyle,
            componentIndex = showkaseAnnotations.indexOf(annotation),
            tags = tags,
            extraMetadata = extraMetadata,
            screenshotConfig = screenshotConfig,
        )
    }
}

internal fun getShowkaseDialogMetadata(
    element: KSFunctionDeclaration,
    showkaseValidator: ShowkaseValidator
): List<ShowkaseMetadata.Component?> {
    val showkaseDialogAnnotations = element.getAnnotations(ShowkaseDialog::class)
    val elementName = element.simpleName.asString()

    val commonMetadata = element.extractCommonMetadata(showkaseValidator)
    val previewParameterMetadata = element.getPreviewParameterMetadata()

    return showkaseDialogAnnotations.mapNotNull { annotation ->
        if (annotation.getAsBoolean("skip")) return@mapNotNull null

        val showkaseName = getShowkaseName(annotation.getAsString("name"), elementName)
        val showkaseGroup = getShowkaseGroup(
            annotation.getAsString("group"),
            commonMetadata.enclosingClass,
        )
        val isDefaultStyle = annotation.getAsBoolean("defaultStyle")
        val showkaseStyleName =
            getShowkaseStyleName(annotation.getAsString("styleName"), isDefaultStyle)
        val tags = annotation.getAsStringList("tags")
        val extraMetadata = annotation.getAsStringList("extraMetadata")
        val screenshotConfig = screenshotConfigFrom(annotation)
        ShowkaseMetadata.Component(
            packageSimpleName = commonMetadata.moduleName,
            packageName = commonMetadata.packageName,
            enclosingClassName = commonMetadata.enclosingClassName,
            elementName = elementName,
            showkaseName = showkaseName,
            showkaseGroup = showkaseGroup,
            showkaseStyleName = showkaseStyleName,
            showkaseWidthDp = annotation.getAsInt("widthDp").parseAnnotationProperty(),
            showkaseHeightDp = annotation.getAsInt("heightDp").parseAnnotationProperty(),
            insideObject = commonMetadata.showkaseFunctionType.insideObject(),
            insideWrapperClass = commonMetadata.showkaseFunctionType == ShowkaseFunctionType.INSIDE_CLASS,
            element = element,
            showkaseKDoc = commonMetadata.kDoc,
            previewParameterName = previewParameterMetadata?.first,
            previewParameterProviderType = previewParameterMetadata?.second,
            isDefaultStyle = isDefaultStyle,
            componentIndex = showkaseDialogAnnotations.indexOf(annotation),
            tags = tags,
            extraMetadata = extraMetadata,
            screenshotConfig = screenshotConfig,
            isDialog = true,
            dialogButtonText = annotation.getAsString("buttonText"),
            dialogHideButtonText = annotation.getAsString("hideButtonText"),
        )
    }
}

private fun screenshotConfigFrom(annotation: KSAnnotation): ScreenshotConfig {
    // KSP on Kotlin/Native does not populate nested-annotation default arguments. If the
    // annotation was used without explicitly supplying `screenshotCaptureConfig = …`, fall back
    // to the declared default of `ScreenshotConfig.SingleStaticImage`.
    val screenshotCaptureConfig = annotation.argByNameOrNull(
        ShowkaseComposable::screenshotCaptureConfig.name
    ) as? KSAnnotation ?: return ScreenshotConfig.SingleStaticImage
    val screenshotCaptureType = ScreenshotCaptureType.valueOf(
        screenshotCaptureConfig.getAsEnum<ScreenshotCaptureType>(ScreenshotCaptureConfig::type.name).name
    )
    val gifDurationMillis =
        screenshotCaptureConfig.getAsInt(ScreenshotCaptureConfig::durationMillis.name)
    val gifFramerate =
        screenshotCaptureConfig.getAsInt(ScreenshotCaptureConfig::framerate.name)
    val animationOffsetsMillis =
        screenshotCaptureConfig.getAsIntList(ScreenshotCaptureConfig::offsetsMillis.name)

    val screenshotConfig = when (screenshotCaptureType) {
        ScreenshotCaptureType.SingleStaticImage -> ScreenshotConfig.SingleStaticImage
        ScreenshotCaptureType.MultipleImagesAtOffsets -> ScreenshotConfig.MultipleImagesAtOffsets(
            offsetMillis = animationOffsetsMillis,
        )

        ScreenshotCaptureType.SingleAnimatedImage -> ScreenshotConfig.SingleAnimatedImage(
            durationMillis = gifDurationMillis,
            framerate = gifFramerate,
        )
    }
    return screenshotConfig
}

internal fun KSFunctionDeclaration.extractCommonMetadata(showkaseValidator: ShowkaseValidator): CommonMetadata {
    return extractCommonMetadataFromDeclaration(this, showkaseValidator)
}

internal fun KSPropertyDeclaration.extractCommonMetadata(showkaseValidator: ShowkaseValidator): CommonMetadata {
    return extractCommonMetadataFromDeclaration(this, showkaseValidator)
}

private fun extractCommonMetadataFromDeclaration(
    declaration: KSDeclaration,
    showkaseValidator: ShowkaseValidator,
): CommonMetadata {
    val parent = declaration.parentDeclaration
    val showkaseFunctionType = getShowkaseFunctionType(declaration, parent)
    val packageName = declaration.packageName.asString()

    return CommonMetadata(
        packageName = packageName,
        moduleName = packageName.substringAfterLast("."),
        kDoc = declaration.docString.orEmpty().trim(),
        showkaseFunctionType = showkaseFunctionType,
        enclosingClass = getEnclosingClass(showkaseFunctionType, parent)
    ).also {
        showkaseValidator.validateEnclosingClass(it.enclosingClass)
    }
}

internal data class CommonMetadata(
    val packageName: String,
    val moduleName: String,
    val kDoc: String,
    val showkaseFunctionType: ShowkaseFunctionType,
    val enclosingClass: KSClassDeclaration?,
) {
    val enclosingClassName: ClassName? = enclosingClass?.toClassName()
}

@Suppress("LongParameterList", "LongMethod")
internal fun getShowkaseMetadataFromPreview(
    element: KSFunctionDeclaration,
    showkaseValidator: ShowkaseValidator,
): List<ShowkaseMetadata.Component?> {
    val previewAnnotations = element.requireAnnotationBySimpleName(PREVIEW_SIMPLE_NAME)
    val elementName = element.simpleName.asString()

    val showkaseComosableAnnotation = element.getAnnotation(ShowkaseComposable::class)
    // If this component was configured to be skipped, return early
    if (showkaseComosableAnnotation != null && showkaseComosableAnnotation.getAsBoolean("skip")) {
        return listOf() // Will be mapped out
    }
    return previewAnnotations.mapIndexed { index, annotation ->
        val commonMetadata = element.extractCommonMetadata(showkaseValidator)
        val showkaseName = getShowkaseName(
            annotation.getAsString("name"),
            elementName
        )
        val showkaseGroup = getShowkaseGroup(
            annotation.getAsString("group"),
            commonMetadata.enclosingClass,
        )

        val width = annotation.getAsInt("widthDp")
        val height = annotation.getAsInt("heightDp")

        val previewParameterMetadata = element.getPreviewParameterMetadata()

        ShowkaseMetadata.Component(
            packageSimpleName = commonMetadata.moduleName,
            packageName = commonMetadata.packageName,
            enclosingClassName = commonMetadata.enclosingClassName,
            elementName = elementName,
            showkaseKDoc = commonMetadata.kDoc,
            showkaseName = showkaseName,
            showkaseGroup = showkaseGroup,
            showkaseWidthDp = if (width == -1) null else width,
            showkaseHeightDp = if (height == -1) null else height,
            insideWrapperClass = commonMetadata.showkaseFunctionType == ShowkaseFunctionType.INSIDE_CLASS,
            insideObject = commonMetadata.showkaseFunctionType.insideObject(),
            element = element,
            previewParameterName = previewParameterMetadata?.first,
            previewParameterProviderType = previewParameterMetadata?.second,
            componentIndex = index,
        )
    }
}

internal fun getShowkaseMetadataFromCustomAnnotation(
    element: KSFunctionDeclaration,
    showkaseValidator: ShowkaseValidator,
    annotationName: String,
): List<ShowkaseMetadata.Component> {
    val customAnnotation = element.requireAnnotationBySimpleName(annotationName)
    val elementName = element.simpleName.asString()

    val previewAnnotations = customAnnotation.map {
        it.annotationDeclaration().requireAnnotationBySimpleName(PREVIEW_SIMPLE_NAME)
    }.flatten()

    val showkaseComosableAnnotation = element.getAnnotation(ShowkaseComposable::class)
    // If this component was configured to be skipped, return early
    if (showkaseComosableAnnotation != null && showkaseComosableAnnotation.getAsBoolean("skip")) {
        return listOf() // Will be mapped out
    }
    return previewAnnotations.mapIndexed { index, annotation ->
        val commonMetadata = element.extractCommonMetadata(showkaseValidator)

        val annotationNameParam = annotation.getAsString("name")
        val annotationHasName = annotationNameParam.isNotEmpty()
        val showkaseNameFromAnnotation = if (annotationHasName) annotationNameParam else index

        val showkaseName = "$elementName - $showkaseNameFromAnnotation"
        val showkaseGroup = getShowkaseGroup(
            annotation.getAsString("group"),
            commonMetadata.enclosingClass,
        )

        val width = annotation.getAsInt("widthDp")
        val height = annotation.getAsInt("heightDp")

        val previewParameterMetadata = element.getPreviewParameterMetadata()

        ShowkaseMetadata.Component(
            packageSimpleName = commonMetadata.moduleName,
            packageName = commonMetadata.packageName,
            enclosingClassName = commonMetadata.enclosingClassName,
            elementName = elementName,
            showkaseKDoc = commonMetadata.kDoc,
            showkaseName = showkaseName,
            showkaseGroup = showkaseGroup,
            showkaseWidthDp = if (width == -1) null else width,
            showkaseHeightDp = if (height == -1) null else height,
            insideWrapperClass = commonMetadata.showkaseFunctionType == ShowkaseFunctionType.INSIDE_CLASS,
            insideObject = commonMetadata.showkaseFunctionType.insideObject(),
            element = element,
            previewParameterName = previewParameterMetadata?.first,
            previewParameterProviderType = previewParameterMetadata?.second,
            componentIndex = index,
        )
    }
}

internal fun getShowkaseMetadata(
    xElement: KSFunctionDeclaration,
    customPreviewMetadata: ShowkaseMultiPreviewCodegenMetadata,
    elementIndex: Int,
    index: Int,
    showkaseValidator: ShowkaseValidator,
): ShowkaseMetadata.Component {
    val commonMetadata = xElement.extractCommonMetadata(showkaseValidator)
    val previewParamMetadata = xElement.getPreviewParameterMetadata()
    val isInsideObject =
        commonMetadata.showkaseFunctionType == ShowkaseFunctionType.INSIDE_OBJECT
    val heightDp = if (customPreviewMetadata.showkaseHeight == -1) {
        null
    } else {
        customPreviewMetadata.showkaseHeight
    }
    val widthDp = if (customPreviewMetadata.showkaseWidth == -1) {
        null
    } else {
        customPreviewMetadata.showkaseWidth
    }
    val elementName = xElement.simpleName.asString()

    return ShowkaseMetadata.Component(
        element = xElement,
        elementName = elementName,
        packageName = commonMetadata.packageName,
        packageSimpleName = commonMetadata.moduleName,
        showkaseName = "$elementName - ${customPreviewMetadata.previewName} - $elementIndex",
        insideObject = commonMetadata.showkaseFunctionType.insideObject(),
        previewParameterName = previewParamMetadata?.first,
        previewParameterProviderType = previewParamMetadata?.second,
        showkaseGroup = getShowkaseGroup(
            customPreviewMetadata.previewGroup,
            commonMetadata.enclosingClass
        ),
        showkaseKDoc = commonMetadata.kDoc,
        enclosingClassName = commonMetadata.enclosingClassName,
        componentIndex = elementIndex + index,
        insideWrapperClass = isInsideObject,
        showkaseHeightDp = heightDp,
        showkaseWidthDp = widthDp,
    )
}

private fun KSFunctionDeclaration.getPreviewParameterMetadata(): Pair<String, TypeName>? {
    val previewParameterPair = getPreviewParameterAnnotation()
    return previewParameterPair?.let {
        it.first to it.second.getAsType("provider").toTypeName()
    }
}

private fun KSFunctionDeclaration.getPreviewParameterAnnotation(): Pair<String, KSAnnotation>? {
    return parameters.mapNotNull { parameter ->
        val previewParamAnnotation = parameter.findAnnotationBySimpleName(PREVIEW_PARAMETER_SIMPLE_NAME)
        previewParamAnnotation?.let {
            (parameter.name?.asString() ?: "") to previewParamAnnotation
        }
    }.firstOrNull()
}

internal fun getShowkaseColorMetadata(
    element: KSPropertyDeclaration,
    showkaseValidator: ShowkaseValidator
): ShowkaseMetadata {
    val showkaseColorAnnotation = element.requireAnnotation(ShowkaseColor::class)
    // TODO(vinaygaba): Color properties aren't working properly with companion objects. This is
    // because the properties are generated outside the companion object in java land(as opposed to
    // inside the companion class for functions). Need to investigate more.
    val commonMetadata = element.extractCommonMetadata(showkaseValidator)
    val elementName = element.simpleName.asString()
    val showkaseName = getShowkaseName(showkaseColorAnnotation.getAsString("name"), elementName)
    val showkaseGroup = getShowkaseGroup(showkaseColorAnnotation.getAsString("group"), commonMetadata.enclosingClass)

    return ShowkaseMetadata.Color(
        element = element,
        showkaseName = showkaseName,
        showkaseGroup = showkaseGroup,
        showkaseKDoc = commonMetadata.kDoc,
        elementName = elementName,
        packageSimpleName = commonMetadata.moduleName,
        packageName = commonMetadata.packageName,
        enclosingClassName = commonMetadata.enclosingClassName,
        insideWrapperClass = commonMetadata.showkaseFunctionType == ShowkaseFunctionType.INSIDE_CLASS,
        insideObject = commonMetadata.showkaseFunctionType.insideObject()
    )
}

internal fun getShowkaseTypographyMetadata(
    element: KSPropertyDeclaration,
    showkaseValidator: ShowkaseValidator
): ShowkaseMetadata {
    val showkaseTypographyAnnotation = element.requireAnnotation(ShowkaseTypography::class)

    val commonMetadata = element.extractCommonMetadata(showkaseValidator)
    val elementName = element.simpleName.asString()
    // TODO(vinaygaba): Typography properties aren't working properly with companion objects.
    // This is because the properties are generated outside the companion object in java land(as
    // opposed to inside the companion class for functions). Need to investigate more.
    val showkaseName = getShowkaseName(showkaseTypographyAnnotation.getAsString("name"), elementName)
    val showkaseGroup =
        getShowkaseGroup(showkaseTypographyAnnotation.getAsString("group"), commonMetadata.enclosingClass)

    return ShowkaseMetadata.Typography(
        element = element,
        showkaseName = showkaseName,
        showkaseGroup = showkaseGroup,
        showkaseKDoc = commonMetadata.kDoc,
        elementName = elementName,
        packageSimpleName = commonMetadata.moduleName,
        packageName = commonMetadata.packageName,
        enclosingClassName = commonMetadata.enclosingClassName,
        insideWrapperClass = commonMetadata.showkaseFunctionType == ShowkaseFunctionType.INSIDE_CLASS,
        insideObject = commonMetadata.showkaseFunctionType.insideObject()
    )
}

internal fun getShowkaseFunctionType(
    declaration: KSDeclaration,
    parent: KSDeclaration?
): ShowkaseFunctionType {
    return when {
        parent !is KSClassDeclaration -> ShowkaseFunctionType.TOP_LEVEL
        parent.isCompanionObject -> ShowkaseFunctionType.INSIDE_COMPANION_OBJECT
        parent.classKind == ClassKind.OBJECT -> ShowkaseFunctionType.INSIDE_OBJECT
        parent.classKind == ClassKind.CLASS || parent.classKind == ClassKind.INTERFACE ->
            ShowkaseFunctionType.INSIDE_CLASS
        else -> throw ShowkaseProcessorException(
            "Function is declared in a way that is not supported by Showkase.",
            declaration
        )
    }
}

internal fun getEnclosingClass(
    showkaseFunctionType: ShowkaseFunctionType,
    parent: KSDeclaration?,
): KSClassDeclaration? = when (showkaseFunctionType) {
    ShowkaseFunctionType.TOP_LEVEL -> null
    ShowkaseFunctionType.INSIDE_CLASS, ShowkaseFunctionType.INSIDE_OBJECT -> parent as KSClassDeclaration
    // Get the class that holds the companion object instead of using the intermediate element
    // that's used to represent the companion object.
    ShowkaseFunctionType.INSIDE_COMPANION_OBJECT ->
        (parent as KSClassDeclaration).parentDeclaration as? KSClassDeclaration
}

internal fun getShowkaseName(
    showkaseNameFromAnnotation: String,
    elementName: String
) = when {
    showkaseNameFromAnnotation.isBlank() -> elementName.capitalize(Locale.getDefault())
    else -> showkaseNameFromAnnotation
}

internal fun getShowkaseGroup(
    showkaseGroupFromAnnotation: String,
    enclosingClass: KSClassDeclaration?,
) = when {
    showkaseGroupFromAnnotation.isNotBlank() -> showkaseGroupFromAnnotation
    showkaseGroupFromAnnotation.isBlank() && enclosingClass != null ->
        enclosingClass.simpleName.asString().capitalize(Locale.getDefault())

    else -> "Default Group"
}

internal fun getShowkaseStyleName(
    showkaseStyleFromAnnotation: String,
    isDefaultStyle: Boolean,
) = when {
    showkaseStyleFromAnnotation.isNotBlank() -> showkaseStyleFromAnnotation.replaceFirstChar { it.uppercase() }
    // If style name is not specified but its the default style, just provide the default style name
    isDefaultStyle -> "Default Style"
    else -> null
}
