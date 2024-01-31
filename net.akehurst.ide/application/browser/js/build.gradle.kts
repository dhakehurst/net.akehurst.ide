import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

plugins {
    id("org.jetbrains.compose")
}

dependencies {
    commonMainImplementation(project(":common"))
    commonMainImplementation(compose.runtime)
    jsMainImplementation(npm("@js-joda/core","5.6.1"))
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
        val jsMain by getting {
            // add the example files to resources of the web-client
            val kotlinExtension = project(":common-data").extensions.getByName("kotlin") as KotlinMultiplatformExtension
            val res = kotlinExtension.sourceSets.getByName("commonMain").resources
            resources.srcDir(res)
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