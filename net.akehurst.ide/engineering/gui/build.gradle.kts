import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile

plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

dependencies {
    commonMainApi(project(":user-inf"))

    //commonMainImplementation("net.akehurst.language.editor:agl-editor-api:1.9.22")

    commonMainImplementation(libs.nak.compose.code.editor)
    commonMainImplementation(libs.nal.agl.editor.compose)

    // to load resources from app filesystem
    // & use of korlibs.image.color.Colors mapping
    commonMainApi(libs.korge.core)

    commonMainImplementation(compose.ui)
    commonMainImplementation(compose.foundation)
    commonMainImplementation(compose.material3)

    jvm11MainImplementation(compose.desktop.currentOs)

    jsMainImplementation(libs.nal.agl.language.service)
    jsMainImplementation("org.jetbrains.kotlin-wrappers:kotlin-js:1.0.0-pre.690")

    jsMainImplementation(libs.nak.html.builder)
    jsMainImplementation(npm(name="@fortawesome/fontawesome-free", version="6.5.1"))

    //compose for js editor is not working so have to use html and codemirror instead
    //jsMainImplementation(libs.nal.agl.editor.codemirror)
    //jsMainImplementation(libs.nak.codemirror)

    //jsMainImplementation(libs.nal.agl.editor.ace)
    //jsMainImplementation(npm("ace-builds", libs.versions.ace.get()))
    //jsMainImplementation(npm("net.akehurst.language.editor-kotlin-ace-loader", "1.5.1"))
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

