// Separate configuration for the asset tools classpath
val assetTools: Configuration by configurations.creating

dependencies {
    assetTools(project(":tools"))
}

// --- Font rendering ---

val fontInfoDir = rootProject.projectDir.resolve("tt/static/font")
val fontTexDir = rootProject.projectDir.resolve("tt/textures/font")
val fontTexClasspath = "/textures/font"

fun registerFont(name: String, fontName: String, fontSize: Int, numChars: Int) =
    tasks.register<JavaExec>("createFont_$name") {
        group = "assets"
        description = "Render $fontName $fontSize font"
        classpath = assetTools
        mainClass.set("com.oddlabs.fontutil.FontRenderer")
        jvmArgs("-ea")
        args(fontName, fontSize.toString(), "1024", numChars.toString(),
             fontInfoDir.absolutePath, fontTexDir.absolutePath, fontTexClasspath)
        outputs.dir(fontInfoDir)
        outputs.dir(fontTexDir)
    }

val createFontTahoma = registerFont("tahoma", "Tahoma", 13, 32000)
val createFontImpact = registerFont("impact", "Impact", 24, 384)

val createFonts = tasks.register("createFonts") {
    group = "assets"
    description = "Render all fonts"
    dependsOn(createFontTahoma, createFontImpact)
}

// --- Asset pipeline ---

val srcTextures = rootProject.projectDir.resolve("tt/textures")
val buildTextures = rootProject.projectDir.resolve("tt/build/textures")
val srcGeometry = rootProject.projectDir.resolve("tt/geometry")
val buildGeometry = rootProject.projectDir.resolve("tt/build/geometry")

// Convert one texture file: Convert <infile> [args] <outdir>
// Last arg is output subdir, second-to-last is format extension, rest are converter args.
fun convertTexture(name: String, png: File, vararg convertArgs: String) =
    tasks.register<JavaExec>("convert_$name") {
        group = "assets"
        description = "Convert $name"
        classpath = assetTools
        mainClass.set("com.oddlabs.imageutil.Convert")
        jvmArgs("-ea", "-Djava.awt.headless=true")
        val ext = convertArgs[convertArgs.lastIndex - 1]
        val subdir = convertArgs.last()
        val outdir = buildTextures.resolve(subdir)
        args(png.absolutePath, *convertArgs.dropLast(1).toTypedArray(), outdir.absolutePath)
        inputs.file(png)
        outputs.file(outdir.resolve("${png.nameWithoutExtension}.$ext"))
    }

fileTree(srcTextures) {
    include("**/*.png")
}.forEach { png ->
    val rel = png.relativeTo(srcTextures).invariantSeparatorsPath
    when {
        rel.startsWith("pixelperfect/") -> convertTexture("${png.nameWithoutExtension}_pixelperfect", png, "-flip", "-format", "image", "gui")
        rel.startsWith("gui/")          -> convertTexture("${png.nameWithoutExtension}_gui", png, "-flip", "-format", "dxtn", "gui")
        rel.startsWith("pointer/")      -> convertTexture("${png.nameWithoutExtension}_pointer", png, "-flip", "-format", "image", "gui")
        rel.startsWith("effects/")      -> convertTexture("${png.nameWithoutExtension}_effects", png, "-flip", "-mipmaps", "-format", "dxtn", "effects")
        rel.startsWith("font/")         -> convertTexture("${png.nameWithoutExtension}_font", png, "-flip", "-format", "dxtn", "font")
        rel.startsWith("models/")       -> convertTexture(rel.replace("/", "_").replace(".png", "_models"), png, "-flip", "-gamma", "0.45454545454545453", "-mipmaps", "-gamma", "2.2", "-format", "dxtn", "models")
        rel.startsWith("teamdecals/")   -> convertTexture(rel.replace("/", "_").replace(".png", "_teamdecals"), png, "-half", "-flip", "-mipmaps", "-format", "dxtn", "models")
    }
}

val textures = tasks.register("textures") {
    group = "assets"
    description = "Convert all textures"
    dependsOn(tasks.matching { it.name.startsWith("convert_") })
}

val geometry = tasks.register<JavaExec>("geometry") {
    group = "assets"
    description = "Convert geometry XML to binary"
    classpath = assetTools
    mainClass.set("com.oddlabs.converter.ConvertToBinary")
    jvmArgs("-ea", "-Xmx512m")
    args("geometry.xml", srcGeometry.absolutePath, buildGeometry.absolutePath)
    inputs.dir(srcGeometry)
    outputs.dir(buildGeometry)
}

