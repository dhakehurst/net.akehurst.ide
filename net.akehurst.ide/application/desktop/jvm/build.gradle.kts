
dependencies {
    commonMainImplementation(project(":common"))


}
kotlin {

    jvm("jvm8") {
        mainRun {
            mainClass = "MainKt"
        }
    }

}
