import com.smushytaco.lwjgl_gradle.Module

plugins {
    application
    id("com.smushytaco.lwjgl3")
}

lwjgl {
    // Strongly recommended: set LWJGL version explicitly
    version = "3.4.0"

    // Add LWJGL modules + the correct native artifacts
    implementation(
        Module.CORE,
        Module.GLFW,
        Module.OPENAL,
        Module.OPENGL,
        Module.STB,
        Module.TINYFD)
}

application {
    mainClass.set("com.oddlabs.tt.Main")
    val args = mutableListOf(
        "-ea", "-esa", //"-check:JNI",
        "-Dcom.oddlabs.tt.developer=true",
        "-Xms80m", "-Xmx512m"
//           , "-javaagent:/Users/mike/.m2/repository/org/lwjglx/lwjglx-debug/1.0.2/lwjglx-debug-1.0.2.jar=validate;trace;output=trace.log"
    )
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        args.add("-XstartOnFirstThread")
    }
    applicationDefaultJvmArgs = args
}

dependencies {
    implementation(project(":common"))
    implementation(project(":assets"))
//    implementation("org.lwjglx:lwjglx-debug:1.0.1")
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