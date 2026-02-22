import com.smushytaco.lwjgl_gradle.Module

plugins {
    `java-library`
    id("com.smushytaco.lwjgl3")
}

lwjgl {
    version = "3.3.6"
    implementation(
        Module.CORE,
        Module.GLFW,
        Module.OPENGL,
        Module.OPENAL,
        Module.JAWT,
    )
}

sourceSets {
    main {
        java.srcDirs("classes")
        resources.srcDirs("static")
    }
}

dependencies {
    api(fileTree("lib/java") {
        include("javasvn.jar", "jorbis.jar", "jsquish.jar", "png.jar")
    })
    // Exclude xerces and xml-apis — they override the JDK's built-in XML parser
    // and reference org.w3c.dom.ls.DocumentLS which was removed in modern JDKs.
    api("commons-pool:commons-pool:1.2") {
        exclude(group = "xerces")
        exclude(group = "xml-apis")
    }
    api("commons-dbcp:commons-dbcp:1.2.1") {
        exclude(group = "xerces")
        exclude(group = "xml-apis")
    }
    api("commons-collections:commons-collections:3.1")
}
