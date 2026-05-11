import com.vanniktech.maven.publish.JavaLibrary
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.SourcesJar

plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("com.vanniktech.maven.publish")
}

kotlin {
    jvmToolchain(21)
}

mavenPublishing {
    configure(JavaLibrary(JavadocJar.Javadoc(), SourcesJar.Sources()))
}
