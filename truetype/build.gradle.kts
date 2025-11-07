plugins {
    java
}

dependencies {
    implementation(project(":common"))
}

tasks.register("run", JavaExec::class) {
    group = "application"
    description = "Runs the truetype Test"
    mainClass.set("com.oddlabs.truetype.Test")
    classpath = sourceSets["main"].runtimeClasspath
    jvmArgs = listOf(
        "-ea", "-esa",
        "-Djava.library.path=${project(":common").projectDir}/build/libs/native",
        "-Dcom.oddlabs.tt.developer=true",
        "-Dorg.lwjgl.util.Debug=true",
        "-Xmx80m"
    )
}
