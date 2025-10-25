plugins {
    application
}

application {
    mainClass.set("com.oddlabs.truetype.Test")
    applicationDefaultJvmArgs = listOf(
        "-ea", "-esa",
        "-Djava.library.path=${project(":common").projectDir}/lib/native",
        "-Dcom.oddlabs.tt.developer=true",
        "-Dorg.lwjgl.util.Debug=true",
        "-Xmx80m"
    )
}

dependencies {
    implementation(project(":common"))
}

sourceSets.main {
    java.srcDir("classes")
    resources {
        srcDir("static")
        srcDir(project(":common").file("static"))
    }
}
