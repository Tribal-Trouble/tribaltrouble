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

// --- Distribution & Packaging ---

val dist = layout.buildDirectory.dir("dist")
val distCommon = dist.map { it.dir("common") }
val mainJarFile = tasks.jar.flatMap { it.archiveFileName }

val stageDist by tasks.registering(Sync::class) {
    group = "distribution"
    description = "Assemble all files for jpackage into dist/common"
    dependsOn(":assets:textures", ":assets:geometry", "revision")

    into(distCommon)

    // The tt jar (contains compiled classes + static/ and i18n/ resources)
    from(tasks.jar)

    // All runtime dependency jars (common, LWJGL, steamworks4j, jorbis, etc.)
    from(configurations.runtimeClasspath)

    // Built textures (loose on classpath for Class.getResource)
    from(layout.buildDirectory.dir("textures")) {
        into("textures")
    }

    // Built geometry (loose on classpath for Class.getResource)
    from(layout.buildDirectory.dir("geometry")) {
        into("geometry")
    }

    // Revision number
    from(layout.buildDirectory.file("revision_number"))

    // Registration binary
    from(rootProject.projectDir.resolve("binaries/registration"))
}

val packageWindows by tasks.registering(Exec::class) {
    group = "distribution"
    description = "Build Windows app-image via jpackage"
    dependsOn(stageDist)
    onlyIf { System.getProperty("os.name").lowercase().contains("windows") }

    val destDir = dist.map { it.dir("windows") }

    doFirst {
        destDir.get().asFile.resolve("TribalTrouble").deleteRecursively()
    }

    workingDir(project.projectDir)
    executable("jpackage")
    args(
        "--name", "TribalTrouble",
        "--dest", destDir.get().asFile.absolutePath,
        "--input", distCommon.get().asFile.absolutePath,
        "--main-jar", mainJarFile.get(),
        "--main-class", "com.oddlabs.tt.Main",
        "--java-options",
        "-ea -Djdk.crypto.KeyAgreement.legacyKDF=true -Xmx512m -cp \$APPDIR\\;\$APPDIR\\*",
        "--type", "app-image",
        "--icon", project.projectDir.resolve("tribaltrouble.ico").absolutePath,
    )
}

// macOS jpackage helper
fun registerMacPackage(
    taskName: String,
    destSubdir: String,
    jpackageType: String,
): TaskProvider<Exec> = tasks.register<Exec>(taskName) {
    group = "distribution"
    description = "Build macOS $jpackageType into $destSubdir via jpackage"
    dependsOn(stageDist)
    onlyIf { System.getProperty("os.name").lowercase().contains("mac") }

    val destDir = dist.map { it.dir(destSubdir) }

    workingDir(project.projectDir)
    executable("jpackage")
    args(
        "--name", "TribalTrouble",
        "--dest", destDir.get().asFile.absolutePath,
        "--input", distCommon.get().asFile.absolutePath,
        "--main-jar", mainJarFile.get(),
        "--main-class", "com.oddlabs.tt.Main",
        "--java-options",
        "-ea -XstartOnFirstThread -cp \$APPDIR:\$APPDIR/* -Djdk.crypto.KeyAgreement.legacyKDF=true",
        "--type", jpackageType,
    )
}

val packageMacX86 = registerMacPackage("packageMacX86", "macosx-x86", "dmg")
val packageMacArm64 = registerMacPackage("packageMacArm64", "macosx-arm64", "app-image")
val packageMacX86App = registerMacPackage("packageMacX86App", "macosx-x86-app", "app-image")

val packageLinux by tasks.registering {
    group = "distribution"
    description = "Build Linux AppImage via jlink + appimagetool"
    dependsOn(stageDist)
    onlyIf { System.getProperty("os.name").lowercase().contains("linux") }

    val linuxDist = dist.map { it.dir("linux") }
    val appDir = linuxDist.map { it.dir("TribalTrouble.AppDir") }

    doLast {
        val appDirFile = appDir.get().asFile
        val linuxDistFile = linuxDist.get().asFile

        // Clean previous build
        appDirFile.deleteRecursively()
        appDirFile.mkdirs()
        appDirFile.resolve("usr/lib").mkdirs()

        // Copy AppImage metadata
        val appImageSrc = project.projectDir.resolve("linux/TribalTrouble.AppDir")
        listOf("AppRun", "icon.png", "TribalTrouble.desktop").forEach { f ->
            appImageSrc.resolve(f).copyTo(appDirFile.resolve(f), overwrite = true)
        }
        appDirFile.resolve("AppRun").setExecutable(true)

        // Copy dist/common into AppDir/common
        distCommon.get().asFile.copyRecursively(appDirFile.resolve("common"), overwrite = true)

        // Copy appimagetool
        val toolSrc = project.projectDir.resolve("linux/appimagetool-x86_64.AppImage")
        val toolDest = linuxDistFile.resolve("appimagetool-x86_64.AppImage")
        toolSrc.copyTo(toolDest, overwrite = true)
        toolDest.setExecutable(true)

        // Run jlink to create minimal JRE
        val jlink = ProcessBuilder(
            "jlink",
            "--output", appDirFile.resolve("usr/lib/jre").absolutePath,
            "--add-modules",
            "java.base,java.desktop,java.prefs,java.rmi,java.sql,jdk.unsupported",
            "--compress=2",
            "--no-header-files",
            "--no-man-pages",
            "--strip-debug",
        ).directory(linuxDistFile).inheritIO().start()
        if (jlink.waitFor() != 0) throw GradleException("jlink failed")

        // Build AppImage
        val appimage = ProcessBuilder(
            "./appimagetool-x86_64.AppImage", "TribalTrouble.AppDir"
        ).directory(linuxDistFile).inheritIO().start()
        if (appimage.waitFor() != 0) throw GradleException("appimagetool failed")

        linuxDistFile.resolve("TribalTrouble-x86_64.AppImage").setExecutable(true)
    }
}
