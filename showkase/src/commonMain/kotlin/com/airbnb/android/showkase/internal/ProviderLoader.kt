package com.airbnb.android.showkase.internal

import com.airbnb.android.showkase.models.ShowkaseProvider

/**
 * Reflectively loads the generated `<rootModuleCanonicalName>Codegen` class and instantiates it as
 * a [ShowkaseProvider]. Works on JVM-backed targets (Android, Desktop). On iOS/Native this throws
 * because Kotlin/Native cannot do classpath lookups by name — iOS consumers must instead call
 * `ShowkaseBrowser(provider = MyRootCodegen())` directly.
 */
internal expect fun loadShowkaseProvider(classKey: String): ShowkaseProvider
