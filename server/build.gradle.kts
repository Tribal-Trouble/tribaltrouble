plugins {
    java
}

dependencies {
    implementation(project(":common"))
    implementation(fileTree("../common/lib/java") { include("*.jar") })
}

sourceSets {
    main {
        java {
            srcDir("classes")
        }
    }
}

tasks.register<Jar>("bugreporter") {
    dependsOn("classes")
    archiveFileName.set("bugreport.jar")
    from(project(":common").sourceSets["main"].output) {
        include("com/oddlabs/net/**/*class")
        include("com/oddlabs/util/**/*class")
        include("com/oddlabs/event/**/*class")
        include("com/oddlabs/bugreport/**/*class")
    }
    from(sourceSets["main"].output) {
        include("com/oddlabs/bugreportserver/**/*class")
    }
}

tasks.register<Jar>("router") {
    dependsOn("classes")
    archiveFileName.set("router.jar")
    from(project(":common").sourceSets["main"].output) {
        include("com/oddlabs/net/**/*class")
        include("com/oddlabs/util/**/*class")
        include("com/oddlabs/event/**/*class")
        include("com/oddlabs/router/**/*class")
    }
    from(sourceSets["main"].output) {
        include("com/oddlabs/routerserver/**/*class")
    }
}

tasks.register<Jar>("matchmaker") {
    dependsOn("classes")
    archiveFileName.set("matchmaking.jar")
    from(project(":common").sourceSets["main"].output) {
        include("com/oddlabs/net/**/*class")
        include("com/oddlabs/util/**/*class")
        include("com/oddlabs/event/**/*class")
        include("com/oddlabs/matchmaking/**/*class")
        include("com/oddlabs/registration/**/*class")
    }
    from(sourceSets["main"].output) {
        include("com/oddlabs/matchserver/**/*class")
    }
    from("../common/static") {
        include("public_reg_key")
    }
}

tasks.register("all") {
    dependsOn("router", "matchmaker", "bugreporter")
}
