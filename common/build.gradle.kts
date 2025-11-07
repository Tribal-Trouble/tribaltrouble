plugins {
    `java-library`
}

val natives by configurations.creating

val lwjglVersion = "2.9.3"

dependencies {
    api("org.jspecify:jspecify:1.0.0")
    api("org.joml:joml:1.10.8")
    api("org.lwjgl.lwjgl:lwjgl:$lwjglVersion")
    api("org.lwjgl.lwjgl:lwjgl_util:${lwjglVersion}")
    natives("org.lwjgl.lwjgl:lwjgl-platform:$lwjglVersion:natives-windows")
    natives("org.lwjgl.lwjgl:lwjgl-platform:$lwjglVersion:natives-linux")
    natives("org.lwjgl.lwjgl:lwjgl-platform:$lwjglVersion:natives-osx")

    testImplementation(platform("org.junit:junit-bom:6.0.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.junit.jupiter:junit-jupiter-params")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
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