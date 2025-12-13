group = "com.oddlabs.tribaltrouble"
version = "1.0.0"

repositories {
    mavenCentral()
}

val fontRenderer  by configurations.creating

dependencies {
    implementation(project(":tools"))

    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    fontRenderer(project(":tools"))
}


val fontInfoDir = "${layout.buildDirectory.get()}/resources/font"
val fontTexDir = "${layout.buildDirectory.get()}/font"
val fontTexClasspath = "/textures/font"

tasks.register("renderTahomaFont", JavaExec::class) {
    group = "build"
    description = "Renders Tahoma TTF font to PNG texture and font metadata."
    mainClass.set("com.oddlabs.fontutil.FontRenderer")
    classpath = fontRenderer + files("resources")

    val ttfFile = File("${project.projectDir}/font/Tahoma.ttf")
    inputs.file(ttfFile)
    outputs.files(
        "$fontInfoDir/tahoma_13.font",
        "$fontTexDir/tahoma_13.png"
    )

    doFirst {
        File(fontInfoDir).mkdirs()
        File(fontTexDir).mkdirs()
    }

    args = listOf(
        "${project.projectDir}/font/Tahoma.ttf",
        "13",
        "1024",
        "1200",
        fontInfoDir,
        fontTexDir,
        fontTexClasspath
    )

    onlyIf { ttfFile.exists() }
}

tasks.register("renderImpactFont", JavaExec::class) {
    group = "build"
    description = "Renders Impact TTF font to PNG texture and font metadata."
    mainClass.set("com.oddlabs.fontutil.FontRenderer")
    classpath = fontRenderer + files("resources")

    val ttfFile = File("${project.projectDir}/font/Impact.ttf")
    inputs.file(ttfFile)
    outputs.files(
        "$fontInfoDir/impact_24.font",
        "$fontTexDir/impact_24.png"
    )

    doFirst {
        File(fontInfoDir).mkdirs()
        File(fontTexDir).mkdirs()
    }

    args = listOf(
        "${project.projectDir}/font/Impact.ttf",
        "24",
        "1024",
        "600",
        fontInfoDir,
        fontTexDir,
        fontTexClasspath
    )

    onlyIf { ttfFile.exists() }
}

tasks.register("renderFonts") {
    group = "build"
    description = "Renders all TTF fonts as PNG textures and font metadata."
    dependsOn("renderTahomaFont", "renderImpactFont")
}

tasks.test {
    useJUnitPlatform()
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

fileTree("textures") {
    include("**/*.png")
    exclude("font/**")
}.forEach { png ->
    val rel = png.relativeTo(file("textures")).path
    when {
        rel.startsWith("pixelperfect") -> convertTexture("${png.nameWithoutExtension}_pixelperfect", png, "-flip", "-format", "image", "gui")
        rel.startsWith("gui") -> convertTexture("${png.nameWithoutExtension}_gui", png, "-flip", "-format", "dds", "gui")
        rel.startsWith("pointer") -> convertTexture("${png.nameWithoutExtension}_pointer", png, "-flip", "-format", "image", "gui")
        rel.startsWith("effects") -> convertTexture("${png.nameWithoutExtension}_effects", png, "-flip", "-mipmaps", "-format", "dds", "effects")
        rel.startsWith("models") -> convertTexture(rel.replace("/", "_").replace(".png", "_models"), png, "-flip", "-gamma", "0.45454545454545453", "-mipmaps", "-gamma", "2.2", "-format", "dds", "models")
        rel.startsWith("teamdecals") -> convertTexture(rel.replace("/", "_").replace(".png", "_teamdecals"), png, "-half", "-flip", "-mipmaps", "-format", "dds", "models")
    }
}

val tahomaPng = File("$fontTexDir/tahoma_13.png")
val convertTahoma = convertTexture("tahoma_13_font", tahomaPng, "-flip", "-format", "dds", "font")
convertTahoma.configure {
    dependsOn(tasks.named("renderTahomaFont"))
}

val impactPng = File("$fontTexDir/impact_24.png")
val convertImpact = convertTexture("impact_24_font", impactPng, "-flip", "-format", "dds", "font")
convertImpact.configure {
    dependsOn(tasks.named("renderImpactFont"))
}

val textures = tasks.register("textures") {
    dependsOn(tasks.matching { it.name.startsWith("convert_") })
    inputs.dir("textures").withPathSensitivity(PathSensitivity.RELATIVE)
    outputs.dir("build/textures")
}

tasks.processResources {
    inputs.files(geometry, textures)
    from("build/geometry") { into("geometry") }
    from("build/textures") { into("textures") }
    from(fontInfoDir) { into("font") }
}
