import org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackOutput

val version_agl:String by project
dependencies {
    jsMainImplementation("net.akehurst.language.editor:agl-editor-browser-worker:$version_agl")
}

kotlin {
    js("js",IR) {
        binaries.executable()
        useEsModules()
        tasks.withType<KotlinJsCompile>().configureEach {
            kotlinOptions {
                useEsClasses = true
            }
        }
        browser {
            webpackTask {
                output.libraryTarget = KotlinWebpackOutput.Target.SELF
            }
        }
    }
}