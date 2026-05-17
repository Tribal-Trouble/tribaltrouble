import com.smushytaco.lwjgl_gradle.Module

plugins {
    application
    id("com.smushytaco.lwjgl3")
}

lwjgl {
    version = "3.4.1"
    implementation(
        Module.CORE,
        Module.GLFW,
        Module.OPENAL,
        Module.OPENGL,
        Module.STB,
        Module.TINYFD
    )
}

application {
    mainClass.set("com.oddlabs.tt.Main")
    val args = mutableListOf(
        "-ea", "-esa",
        "--enable-native-access=ALL-UNNAMED",
        "-Dcom.oddlabs.tt.developer=true",
        "-Djdk.crypto.KeyAgreement.legacyKDF=true",
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
    implementation("com.code-disaster.steamworks4j:steamworks4j:1.10.0")
}

// steamworks4j lacks module-info, so it stays on the classpath as the unnamed module.
tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.addAll(listOf("--add-reads", "com.oddlabs.tt=ALL-UNNAMED"))
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

// --- BuildInfo generation ---
// Writes BuildInfo.java with VERSION = MAJOR.MINOR.PATCH.
// PATCH = commits since the bump commit that introduced the current MAJOR.MINOR.
// Must stay in sync with the `version` job in .github/workflows/gradle.yml.

val generatedBuildInfoDir = layout.buildDirectory.dir("generated/sources/buildinfo/java/main")

val generateBuildInfo by tasks.registering {
    val outputDir = generatedBuildInfoDir
    val baseVersion = project.version.toString()
    val gitDir = project.rootDir
    outputs.dir(outputDir)
    outputs.upToDateWhen { false }  // git history changes between commits; always recompute

    doLast {
        val patch = runCatching {
            val anchor = ProcessBuilder(
                "git", "log", "--format=%H",
                "-S", "version = \"$baseVersion\"",
                "--", "build.gradle.kts"
            )
                .directory(gitDir)
                .redirectErrorStream(true)
                .start()
                .inputStream.bufferedReader()
                .readText()
                .trim()
                .lineSequence()
                .firstOrNull()
                .orEmpty()
            if (anchor.isEmpty()) {
                "0"
            } else {
                ProcessBuilder("git", "rev-list", "--count", "$anchor..HEAD")
                    .directory(gitDir)
                    .redirectErrorStream(true)
                    .start()
                    .inputStream.bufferedReader()
                    .readText()
                    .trim()
                    .ifEmpty { "0" }
            }
        }.getOrDefault("0")

        val effectiveVersion = "$baseVersion.$patch"
        val packageDir = outputDir.get().asFile.resolve("com/oddlabs/tt")
        packageDir.mkdirs()
        packageDir.resolve("BuildInfo.java").writeText(
            """
            package com.oddlabs.tt;

            public final class BuildInfo {
                public static final String VERSION = "$effectiveVersion";
                private BuildInfo() {}
            }

            """.trimIndent()
        )
    }
}

sourceSets.main {
    java.srcDir(generateBuildInfo)
}

tasks.run.configure {
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

    from(tasks.jar)
    from(configurations.runtimeClasspath)

    from(project(":assets").layout.buildDirectory.dir("textures")) {
        into("textures")
    }
    from(project(":assets").layout.buildDirectory.dir("geometry")) {
        into("geometry")
    }
    from(layout.buildDirectory.file("revision_number"))
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
val packageMacArm64 = registerMacPackage("packageMacArm64", "macosx-arm64", "dmg")
val packageMacArm64App = registerMacPackage("packageMacArm64App", "macosx-arm64-app", "app-image")
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

        appDirFile.deleteRecursively()
        appDirFile.mkdirs()
        appDirFile.resolve("usr/lib").mkdirs()

        val appImageSrc = project.projectDir.resolve("linux/TribalTrouble.AppDir")
        listOf("AppRun", "icon.png", "TribalTrouble.desktop").forEach { f ->
            appImageSrc.resolve(f).copyTo(appDirFile.resolve(f), overwrite = true)
        }
        appDirFile.resolve("AppRun").setExecutable(true)

        distCommon.get().asFile.copyRecursively(appDirFile.resolve("common"), overwrite = true)

        val toolSrc = project.projectDir.resolve("linux/appimagetool-x86_64.AppImage")
        val toolDest = linuxDistFile.resolve("appimagetool-x86_64.AppImage")
        toolSrc.copyTo(toolDest, overwrite = true)
        toolDest.setExecutable(true)

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

        val appimage = ProcessBuilder(
            "./appimagetool-x86_64.AppImage", "TribalTrouble.AppDir"
        ).directory(linuxDistFile).inheritIO().start()
        if (appimage.waitFor() != 0) throw GradleException("appimagetool failed")

        linuxDistFile.resolve("TribalTrouble-x86_64.AppImage").setExecutable(true)
    }
}
