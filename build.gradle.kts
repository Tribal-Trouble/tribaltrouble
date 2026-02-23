plugins {
    java
    id("com.smushytaco.lwjgl3") version "1.0.1" apply false
}

allprojects {
    group = "com.oddlabs.tribaltrouble"
    version = "1.0"
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

val gjfJar = rootProject.file("ci/google-java-format-1.28.0-all-deps.jar")

fun registerFormatTask(name: String, vararg extraArgs: String) =
    tasks.register<JavaExec>(name) {
        group = "formatting"
        classpath = files(gjfJar)
        mainClass.set("com.google.googlejavaformat.java.Main")
        jvmArgs(
            "--add-exports", "jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED",
            "--add-exports", "jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED",
        )
        doFirst {
            val fileList = layout.buildDirectory.file("$name-files.txt").get().asFile
            fileList.parentFile.mkdirs()
            fileList.writeText(subprojects.flatMap { sub ->
                sub.fileTree("classes") { include("**/*.java") }.files.map { it.absolutePath }
            }.joinToString("\n"))
            args(*extraArgs, "@${fileList.absolutePath}")
        }
    }

registerFormatTask("format", "--replace", "--aosp").configure {
    description = "Format all Java files using google-java-format"
}

registerFormatTask("formatCheck", "--dry-run", "--set-exit-if-changed", "--aosp").configure {
    description = "Check if any Java files need formatting"
}
