plugins {
    application
}

application {
    mainClass.set("com.oddlabs.truetype.Test")
    applicationDefaultJvmArgs = listOf(
        "-ea", "-esa",
        "-Djava.library.path=${project(":common").projectDir}/build/libs/native",
        "-Dcom.oddlabs.tt.developer=true",
        "-Dorg.lwjgl.util.Debug=true",
        "-Xmx80m"
    )
}

dependencies {
    implementation(project(":common"))
}

