plugins {
    `java-library`
}

val natives by configurations.creating

dependencies {
    val lwjglVersion = "2.9.3"
    api("org.lwjgl.lwjgl:lwjgl:$lwjglVersion")
    api("org.lwjgl.lwjgl:lwjgl_util:${lwjglVersion}")
    natives("org.lwjgl.lwjgl:lwjgl-platform:$lwjglVersion:natives-windows")
    natives("org.lwjgl.lwjgl:lwjgl-platform:$lwjglVersion:natives-linux")
    natives("org.lwjgl.lwjgl:lwjgl-platform:$lwjglVersion:natives-osx")
}

val unpackNatives = tasks.register<Copy>("unpackNatives") {
    group = "build"
    description = "Unpack native libraries"
    destinationDir = file("build/libs/native")
    from(natives.map { zipTree(it) }) {
        include("*.dylib", "*.so", "*.dll")
    }
    into(destinationDir)
}

tasks.named("processResources") {
    dependsOn(unpackNatives)
}