plugins {
    java
    application
}

sourceSets {
    main {
        java.srcDirs("classes")
    }
}

application {
    applicationName = "matchmaker"
    mainClass.set("com.oddlabs.matchserver.MatchmakingServer")
    applicationDefaultJvmArgs = listOf("-Djdk.crypto.KeyAgreement.legacyKDF=true")
}

// Second launch script for the router
tasks.register<CreateStartScripts>("routerScripts") {
    mainClass.set("com.oddlabs.routerserver.RouterServer")
    applicationName = "router"
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
}

tasks.installDist {
    dependsOn("routerScripts")
}

// Include extras in the distribution
distributions {
    main {
        contents {
            from("server.properties.template")
            from("README-server.md")
            from(".") {
                include("logs/.gitkeep")
                into("logs")
            }
        }
    }
}

// Create an empty logs dir and combo scripts in installDist output
tasks.installDist {
    doLast {
        destinationDir.resolve("logs").mkdirs()

        // start-all script (Unix)
        destinationDir.resolve("bin/start-all").apply {
            writeText("""
                |#!/bin/bash
                |SCRIPT_DIR=${'$'}(cd "${'$'}(dirname "${'$'}0")" && pwd)
                |echo "Starting matchmaker..."
                |"${'$'}SCRIPT_DIR/matchmaker" &
                |echo "Starting router..."
                |"${'$'}SCRIPT_DIR/router" &
                |echo "Both servers started. PIDs: matchmaker=${'$'}!, router=${'$'}!"
                |wait
            """.trimMargin() + "\n")
            setExecutable(true)
        }

        // start-all script (Windows)
        destinationDir.resolve("bin/start-all.bat").apply {
            writeText("""
                |@echo off
                |echo Starting matchmaker...
                |start "" "%~dp0matchmaker.bat"
                |echo Starting router...
                |start "" "%~dp0router.bat"
                |echo Both servers started.
            """.trimMargin() + "\r\n")
        }
    }
}

dependencies {
    implementation(project(":common"))
    implementation("com.discord4j:discord4j-core:3.2.6")
    implementation("commons-pool:commons-pool:1.2")
    implementation("commons-dbcp:commons-dbcp:1.2.1")
    implementation("commons-collections:commons-collections:3.1")
    implementation("com.mysql:mysql-connector-j:9.3.0")
}

tasks.register<JavaExec>("runMatchmaker") {
    group = "application"
    description = "Run the matchmaking server"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.oddlabs.matchserver.MatchmakingServer")
    jvmArgs("-Djdk.crypto.KeyAgreement.legacyKDF=true")
}

tasks.register<JavaExec>("runRouter") {
    group = "application"
    description = "Run the router server"
    classpath = sourceSets.main.get().runtimeClasspath
    mainClass.set("com.oddlabs.routerserver.RouterServer")
}
