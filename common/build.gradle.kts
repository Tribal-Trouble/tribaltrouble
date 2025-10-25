plugins {
    `java-library`
}

dependencies {
    api(fileTree("lib/java"))
}

sourceSets.main {
    java.srcDir("classes")
    resources.srcDir("static")
}
