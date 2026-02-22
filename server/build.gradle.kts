plugins {
    java
}

sourceSets {
    main {
        java.srcDirs("classes")
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
