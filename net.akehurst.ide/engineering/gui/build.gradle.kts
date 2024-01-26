import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile

plugins {
    id("org.jetbrains.compose")
}

val version_korge:String by project
val version_agl:String by project
dependencies {
    commonMainApi(project(":user-inf"))

    //commonMainImplementation("net.akehurst.language.editor:agl-editor-api:1.9.22")
    commonMainImplementation("net.akehurst.language.editor:agl-editor-compose:$version_agl")
    commonMainApi("com.soywiz.korge:korge-core:$version_korge") // to load resources from filesystem TODO improve

    commonMainImplementation(compose.ui)
    commonMainImplementation(compose.foundation)
    commonMainImplementation(compose.material3)
    commonMainImplementation("net.akehurst.kotlin.compose:code-editor:1.9.22")

    jvm8MainImplementation(compose.desktop.currentOs)

    jsMainImplementation("org.jetbrains.kotlin-wrappers:kotlin-js:1.0.0-pre.690")

}

kotlin {
    js(IR) {
        binaries.executable()
        useEsModules()
        tasks.withType<KotlinJsCompile>().configureEach {
            kotlinOptions {
                useEsClasses = true
            }
        }
    }
}

