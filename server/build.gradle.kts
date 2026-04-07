plugins {
    java
    application
}

application {
    applicationName = "matchmaker"
    mainClass.set("com.oddlabs.matchserver.MatchmakingServer")
    applicationDefaultJvmArgs = listOf("-Djdk.crypto.KeyAgreement.legacyKDF=true")
}

tasks.register<CreateStartScripts>("routerScripts") {
    mainClass.set("com.oddlabs.routerserver.RouterServer")
    applicationName = "router"
    classpath = tasks.named<CreateStartScripts>("startScripts").get().classpath
    outputDir = tasks.named<CreateStartScripts>("startScripts").get().outputDir
}

tasks.installDist {
    dependsOn("routerScripts")
}

distributions {
    main {
        distributionBaseName.set("server")
        contents {
            from("server.properties.template") {
                rename { "server.properties" }
            }
        }
    }
}

tasks.installDist {
    doLast {
        val root = destinationDir
        root.resolve("logs").mkdirs()

        root.resolve("bin").listFiles()?.forEach { script ->
            var content = script.readText()
            if (script.name.endsWith(".bat")) {
                content = content.replace("@rem Execute ", "cd /d \"%APP_HOME%\"\r\n\r\n@rem Execute ")
            } else {
                content = content.replace("exec ", "cd \"\$APP_HOME\"\n\nexec ")
            }
            script.writeText(content)
        }

        root.resolve("start-all").apply {
            writeText(
                """
                |#!/bin/bash
                |SCRIPT_DIR=${'$'}(cd "${'$'}(dirname "${'$'}0")" && pwd)
                |echo "Starting matchmaker..."
                |"${'$'}SCRIPT_DIR/bin/matchmaker" &
                |echo "Starting router..."
                |"${'$'}SCRIPT_DIR/bin/router" &
                |echo "Both servers started. PIDs: matchmaker=${'$'}!, router=${'$'}!"
                |wait
            """.trimMargin() + "\n"
            )
            setExecutable(true)
        }

        root.resolve("start-all.bat").apply {
            writeText(
                """
                |@echo off
                |echo Starting matchmaker...
                |start "" "%~dp0bin\matchmaker.bat"
                |echo Starting router...
                |start "" "%~dp0bin\router.bat"
                |echo Both servers started.
            """.trimMargin() + "\r\n"
            )
        }
    }
}

dependencies {
    implementation(project(":common"))
    implementation("commons-pool:commons-pool:1.2")
    implementation("commons-dbcp:commons-dbcp:1.2.1")
    implementation("commons-collections:commons-collections:3.1")
    implementation("com.mysql:mysql-connector-j:9.3.0")
    implementation("com.discord4j:discord4j-core:3.2.6")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.18.3")
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
