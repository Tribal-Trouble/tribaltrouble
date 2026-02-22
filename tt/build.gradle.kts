import com.smushytaco.lwjgl_gradle.Module

plugins {
    application
    id("com.smushytaco.lwjgl3")
}

lwjgl {
    version = "3.3.6"
    implementation(
        Module.CORE,
        Module.GLFW,
        Module.OPENGL,
        Module.OPENAL,
    )
}

sourceSets {
    main {
        java.srcDirs("classes")
        resources.srcDirs("static", "i18n")
    }
}

application {
    mainClass.set("com.oddlabs.tt.Main")
    val args = mutableListOf(
        "-ea",
        "-Xms80m", "-Xmx512m",
        "-Djdk.crypto.KeyAgreement.legacyKDF=true",
    )
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        args.add("-XstartOnFirstThread")
    }
    applicationDefaultJvmArgs = args
}

dependencies {
    implementation(project(":common"))
    implementation("com.code-disaster.steamworks4j:steamworks4j:1.10.0")
}

// --- Revision ---

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
    dependsOn(":assets:textures", ":assets:geometry")
    classpath = files(layout.buildDirectory) + sourceSets.main.get().runtimeClasspath
}
