plugins {
    application
}

repositories {
    mavenCentral()
}

application {
    mainClass.set("com.oddlabs.tt.Main")
    applicationDefaultJvmArgs = listOf(
        "-ea", "-esa",
        "-Djava.library.path=${project(":common").projectDir}/build/libs/native",
        "-Dcom.oddlabs.tt.developer=true",
        "-Dorg.lwjgl.util.Debug=true",
        "-Xmx80m"
    )
}

dependencies {
    implementation(project(":common"))
    implementation(project(":tools"))
    implementation("org.jcraft:jorbis:0.0.17")
    implementation("io.github.memo33:jsquish:2.1.0")
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

val geometry = tasks.register<JavaExec>("geometry") {
    classpath(configurations.runtimeClasspath)
    mainClass.set("com.oddlabs.converter.ConvertToBinary")
    args("geometry.xml", "geometry", "build/geometry")
    jvmArgs("-ea", "-Xmx512m")
    inputs.file("geometry/geometry.xml")
    inputs.dir("geometry").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir("build/geometry")
}

fun convertTexture(name: String, png: File, vararg convertArgs: String) =
    tasks.register<JavaExec>("convert_$name") {
        classpath(configurations.runtimeClasspath)
        mainClass.set("com.oddlabs.imageutil.Convert")
        val ext = convertArgs[convertArgs.lastIndex - 1]
        val subdir = convertArgs.last()
        val outdir = "build/textures/$subdir"
        args(png.absolutePath, *convertArgs.dropLast(1).toTypedArray(), file(outdir).absolutePath)
        jvmArgs("-ea", "-Djava.awt.headless=true")
        inputs.file(png)
        outputs.file("$outdir/${png.nameWithoutExtension}.$ext")
    }

fileTree("textures") { include("**/*.png") }.forEach { png ->
    val rel = png.relativeTo(file("textures")).path
    when {
        rel.startsWith("pixelperfect") -> convertTexture("${png.nameWithoutExtension}_pixelperfect", png, "-flip", "-format", "image", "gui")
        rel.startsWith("gui") -> convertTexture("${png.nameWithoutExtension}_gui", png, "-flip", "-format", "dxtn", "gui")
        rel.startsWith("pointer") -> convertTexture("${png.nameWithoutExtension}_pointer", png, "-flip", "-format", "image", "gui")
        rel.startsWith("effects") -> convertTexture("${png.nameWithoutExtension}_effects", png, "-flip", "-mipmaps", "-format", "dxtn", "effects")
        rel.startsWith("font") -> convertTexture("${png.nameWithoutExtension}_font", png, "-flip", "-format", "dxtn", "font")
        rel.startsWith("models") -> convertTexture(rel.replace("/", "_").replace(".png", "_models"), png, "-flip", "-gamma", "0.45454545454545453", "-mipmaps", "-gamma", "2.2", "-format", "dxtn", "models")
        rel.startsWith("teamdecals") -> convertTexture(rel.replace("/", "_").replace(".png", "_teamdecals"), png, "-half", "-flip", "-mipmaps", "-format", "dxtn", "models")
    }
}

val textures = tasks.register("textures") {
    dependsOn(tasks.matching { it.name.startsWith("convert_") })
    inputs.dir("textures").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir("build/textures")
}

tasks.processResources {
    inputs.files(revision, geometry, textures)
    from("build/geometry") { into("geometry") }
    from("build/textures") { into("textures") }
}

tasks.run.configure {
    classpath = files(layout.buildDirectory) + sourceSets.main.get().runtimeClasspath
}
