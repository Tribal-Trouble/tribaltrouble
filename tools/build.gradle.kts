plugins {
    java
}

sourceSets {
    main {
        java.srcDirs("classes")
        java.exclude("com/oddlabs/svnutil/**")
    }
}

dependencies {
    implementation(project(":common"))
    implementation(fileTree("../common/lib/java") {
        include(
            "lwjgl.jar",
            "lwjgl-opengl.jar",
        )
    })
}
