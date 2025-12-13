plugins {
    application
}

val natives by configurations.creating

val lwjglVersion = "2.9.3"

application {
    mainClass.set("com.oddlabs.tt.Main")
    applicationDefaultJvmArgs = listOf(
        "-ea", "-esa", // "-Xcheck:jni",
        "-Djava.library.path=build/libs/native",
        "-Dcom.oddlabs.tt.developer=true",
        "-Dorg.lwjgl.util.Debug=true",
        "-Xmx80m"
    )
}

dependencies {
    implementation(project(":common"))
    implementation(project(":assets"))
    implementation("org.jcraft:jorbis:0.0.17")
    implementation("org.lwjgl.lwjgl:lwjgl:$lwjglVersion")

    natives("org.lwjgl.lwjgl:lwjgl-platform:$lwjglVersion:natives-windows")
    natives("org.lwjgl.lwjgl:lwjgl-platform:$lwjglVersion:natives-linux")
    natives("org.lwjgl.lwjgl:lwjgl-platform:$lwjglVersion:natives-osx")
}

val revision = tasks.register("revision") {
    val output = layout.buildDirectory.file("revision_number")
    outputs.file(output)
    outputs.upToDateWhen { false }
    doLast {
        output.get().asFile.apply {
            parentFile.mkdirs()
            writeText("gradle-build")
        }
    }
}

tasks.processResources {
    inputs.files(revision)
}

tasks.run.configure {
    classpath = files(layout.buildDirectory) + sourceSets.main.get().runtimeClasspath
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