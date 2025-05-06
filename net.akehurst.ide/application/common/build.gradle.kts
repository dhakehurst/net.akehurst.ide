plugins {
    alias(libs.plugins.compose)
    alias(libs.plugins.kotlin.compose)
}

kotlin {

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                implementation(compose.runtime)
                implementation(compose.ui)
                implementation(compose.foundation)
                implementation(compose.material3)

                implementation(libs.nak.kotlinx.logging.common)
                implementation(libs.nal.agl.language.service)

                implementation(libs.nak.kotlinx.filesystem) //should come from user-api !


                implementation(project(":gui"))
            }
        }
    }
}