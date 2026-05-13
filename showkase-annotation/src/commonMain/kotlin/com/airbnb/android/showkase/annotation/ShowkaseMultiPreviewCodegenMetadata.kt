package com.airbnb.android.showkase.annotation

@MustBeDocumented
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.FUNCTION)
annotation class ShowkaseMultiPreviewCodegenMetadata(
    val previewName: String,
    val previewGroup: String,
    val supportTypeQualifiedName: String,
    val packageName: String,
    val showkaseWidth: Int,
    val showkaseHeight: Int,
)
