import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.compose")
}

dependencies {
    commonMainImplementation(project(":common"))
    commonMainImplementation(compose.runtime)
    jsMainImplementation(npm("@js-joda/core","5.6.1"))
    jsMainImplementation(npm("css-loader", "6.8.1"))
    jsMainImplementation(npm("style-loader", "3.3.3"))
}

kotlin {
    js {
        binaries.executable()
        browser {
            webpackTask {
                output.library = "${project.group}-${project.name}"
                mainOutputFileName = "${project.group}-${project.name}.js"
            }
        }
    }
    sourceSets {
        // add the all files to resources of the web-client
        val jsMain by getting {
            listOf(":gui", ":common-data").forEach {projName ->
                val proj = project(projName)
                val kotlinExtension = proj.extensions.getByName("kotlin") as KotlinMultiplatformExtension
                listOf("commonMain", "jsMain").forEach {
                    val res = kotlinExtension.sourceSets.getByName(it).resources
                    resources.srcDir(res)
                }
            }
        }
    }
}

compose.experimental {
    web.application{}
}


val workerTask = tasks.register<Copy>("copyAglEditorWorkerJs") {
    dependsOn(":js-worker:jsBrowserProductionWebpack")
    dependsOn(":js-worker:jsBrowserDistribution")
    dependsOn("jsProcessResources")
    from("$buildDir/../js-worker/dist/js/productionExecutable") {
        include("js-worker.js")
        include("js-worker.js.map")
    }
    into(file("$buildDir/processedResources/js/main"))

}

val workerTaskDev = tasks.register<Copy>("copyAglEditorWorkerJsDev") {
    dependsOn(":js-worker:jsBrowserDevelopmentWebpack")
    dependsOn(":js-worker:jsBrowserDevelopmentExecutableDistribution")
    dependsOn("jsProcessResources")
    from("$buildDir/../js-worker/dist/js/developmentExecutable") {
        include("js-worker.js")
        include("js-worker.js.map")
    }
    into(file("$buildDir/processedResources/js/main"))

}

tasks.getByName("jsBrowserDevelopmentRun").dependsOn(workerTaskDev)
tasks.getByName("jsBrowserDevelopmentWebpack").dependsOn(workerTaskDev)
tasks.getByName("jsDevelopmentExecutableCompileSync").dependsOn(workerTaskDev)
tasks.getByName("jsBrowserDevelopmentExecutableDistributeResources").dependsOn(workerTaskDev)

tasks.getByName("jsBrowserProductionRun").dependsOn(workerTask)
tasks.getByName("jsBrowserProductionWebpack").dependsOn(workerTask)
tasks.getByName("jsProductionExecutableCompileSync").dependsOn(workerTask)
tasks.getByName("jsBrowserProductionExecutableDistributeResources").dependsOn(workerTask)
tasks.getByName("jsJar").dependsOn(workerTask)