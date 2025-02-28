import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension

dependencies {
    commonMainImplementation(project(":common"))


}
kotlin {

    jvm("jvm11") {
        mainRun {
            mainClass = "MainKt"
        }
    }
    sourceSets {
        // add the all files to resources resources folder next to app
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
