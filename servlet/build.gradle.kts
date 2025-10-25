plugins {
    java
    war
}

dependencies {
    implementation(project(":common"))
    implementation(fileTree("../common/lib/java") { include("*.jar") })
    compileOnly(fileTree("../common/lib/java") { include("servlet-api.jar") })
}

sourceSets {
    main {
        java {
            srcDir("classes")
        }
    }
}

tasks.register<War>("matchservlet") {
    dependsOn("classes")
    archiveFileName.set("matchservlet.war")
    webXml = file("descriptors/matchservlet/web.xml")
    from(sourceSets["main"].output) {
        include("com/oddlabs/matchservlet/**/*class")
        into("WEB-INF/classes")
    }
    from(project(":common").sourceSets["main"].output) {
        include("com/oddlabs/util/CryptUtils.class")
        include("com/oddlabs/registration/RegistrationKey.class")
        include("com/oddlabs/registration/RegistrationKeyFormatException.class")
        into("WEB-INF/classes")
    }
    metaInf {
        from("descriptors/matchservlet") {
            include("context.xml")
        }
    }
}

tasks.register<War>("regservlet") {
    dependsOn("classes")
    archiveFileName.set("oddlabs.war")
    webXml = file("regservlet/web.xml")
    from(project(":common").sourceSets["main"].output) {
        include("com/oddlabs/net/**/*class")
        include("com/oddlabs/util/**/*class")
        include("com/oddlabs/registration/**/*class")
        into("WEB-INF/classes")
    }
    from(sourceSets["main"].output) {
        include("com/oddlabs/regservlet/**/*class")
        into("WEB-INF/classes")
    }
    from("../common/static") {
        include("private_reg_key")
        into("WEB-INF/classes")
    }
    metaInf {
        from("regservlet") {
            include("context.xml")
        }
    }
}

tasks.register<War>("graphservlet") {
    dependsOn("classes")
    archiveFileName.set("graph.war")
    webXml = file("graphservlet/web.xml")
    from(sourceSets["main"].output) {
        include("com/oddlabs/graphservlet/**/*class")
        into("WEB-INF/classes")
    }
    metaInf {
        from("graphservlet") {
            include("context.xml")
        }
    }
}
