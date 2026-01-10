group = "com.oddlabs.tribaltrouble"
version = "1.0.0"

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

tasks.register("renderInterLightFont", JavaExec::class) {
    group = "build"
    description = "Renders Inter Light TTF font to PNG texture and font metadata."
    jvmArgs("-ea", "-Xmx512m", "-Djava.awt.headless=true")
    mainClass.set("com.oddlabs.fontutil.FontRenderer")
    classpath = fontRenderer + files("resources")

    val ttfFile = File("${project.projectDir}/font/Inter-Light.ttf")
    inputs.file(ttfFile)
    outputs.files(
        "$fontInfoDir/inter-light_13.font",
        "$fontTexDir/inter-light_13.png"
    )

    doFirst {
        File(fontInfoDir).mkdirs()
        File(fontTexDir).mkdirs()
    }

    args = listOf(
        ttfFile.toString(),
        "13",
        "2048",
        "1200",
        "2",
        fontInfoDir,
        fontTexDir,
        fontTexClasspath
    )

    onlyIf { ttfFile.exists() }
}

tasks.register("renderInterTightBlackFont", JavaExec::class) {
    group = "build"
    description = "Renders Inter Tight Black TTF font to PNG texture and font metadata."
    jvmArgs("-ea", "-Xmx512m", "-Djava.awt.headless=true")
    mainClass.set("com.oddlabs.fontutil.FontRenderer")
    classpath = fontRenderer + files("resources")

    val ttfFile = File("${project.projectDir}/font/InterTight-Black.ttf")
    inputs.file(ttfFile)
    outputs.files(
        "$fontInfoDir/intertight-black_28.font",
        "$fontTexDir/intertight-black_28.png"
    )

    doFirst {
        File(fontInfoDir).mkdirs()
        File(fontTexDir).mkdirs()
    }

    args = listOf(
        ttfFile.toString(),
        "28",
        "2048",
        "600",
        "2",
        fontInfoDir,
        fontTexDir,
        fontTexClasspath
    )

    onlyIf { ttfFile.exists() }
}

tasks.register("renderFonts") {
    group = "build"
    description = "Renders all TTF fonts as PNG textures and font metadata."
    dependsOn("renderInterLightFont", "renderInterTightBlackFont")
}

tasks.test {
    useJUnitPlatform()
}

val geometry = tasks.register<JavaExec>("geometry") {
    classpath(configurations.runtimeClasspath)
    mainClass.set("com.oddlabs.converter.ConvertToBinary")
    args("geometry.xml", "geometry", "build/geometry")
    jvmArgs("-ea", "-Xmx512m", "-Djava.awt.headless=true")
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

val inter_light_png = File("$fontTexDir/inter-light_13.png")
val convertInterLight = convertTexture("inter-light_13_font", inter_light_png, "-flip", "-format", "dds", "font")
convertInterLight.configure {
    dependsOn(tasks.named("renderInterLightFont"))
}

val intertight_black_png = File("$fontTexDir/intertight-black_28.png")
val convertInterTightBlack = convertTexture("intertight-black_28_font", intertight_black_png, "-flip", "-format", "dds", "font")
convertInterTightBlack.configure {
    dependsOn(tasks.named("renderInterTightBlackFont"))
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
