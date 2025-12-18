import com.smushytaco.lwjgl_gradle.Module

plugins {
    application
    id("com.smushytaco.lwjgl3")
}

lwjgl {
    // Strongly recommended: set LWJGL version explicitly
    version = "3.3.6"

    // Add LWJGL modules + the correct native artifacts
    implementation(
        com.smushytaco.lwjgl_gradle.Module.CORE,
        com.smushytaco.lwjgl_gradle.Module.GLFW,
        com.smushytaco.lwjgl_gradle.Module.OPENAL,
        com.smushytaco.lwjgl_gradle.Module.OPENGL,
        com.smushytaco.lwjgl_gradle.Module.STB,
        Module.TINYFD)
}

application {
    mainClass.set("com.oddlabs.tt.Main")
    val args = mutableListOf(
        "-ea", "-esa",
        "-Dcom.oddlabs.tt.developer=true",
        "-Xms80m", "-Xmx512m"
    )
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        args.add("-XstartOnFirstThread")
}
    applicationDefaultJvmArgs = args
}

dependencies {
    implementation(project(":common"))
    implementation(project(":assets"))
    implementation("org.jcraft:jorbis:0.0.17")
}

val revision = tasks.register("revision") {
    val output = layout.buildDirectory.file("revision_number")
    outputs.file(output)
    outputs.upToDateWhen { false }
    doLast {
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText("gradle-build")
        }
    }
}

tasks.processResources {
    inputs.files(revision)
}

tasks.run.configure {
    classpath = files(layout.buildDirectory) + sourceSets.main.get().runtimeClasspath
}