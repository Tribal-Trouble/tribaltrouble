plugins {
    application
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.oddlabs.tt.Main")
    applicationDefaultJvmArgs = listOf(
        "-ea", "-esa", "-Xcheck:jni",
        "-Djava.library.path=${project(":common").projectDir}/build/libs/native",
        "-Dcom.oddlabs.tt.developer=true",
        "-Dorg.lwjgl.util.Debug=true",
        "-Xmx80m"
    )
}

dependencies {
    implementation(project(":common"))
    implementation(project(":assets"))
    implementation("org.jcraft:jorbis:0.0.17")
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
