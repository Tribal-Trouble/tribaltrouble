import com.smushytaco.lwjgl_gradle.Module
import org.gradle.internal.os.OperatingSystem

group = "com.oddlabs.tribaltrouble"
version = "1.0.0"

plugins {
    id("com.smushytaco.lwjgl3")
}

lwjgl {
    version = "3.4.1"
    implementation(
        Module.CORE,
        Module.OPENGL,
        Module.STB)
}

// Configuration for running internal tools
val converter by configurations.creating
dependencies {
    converter(project(":common"))
    converter(project(":tools"))
}

// Find basisu executable on system path
val basisuPath: String? by lazy {
    val os = OperatingSystem.current()
    val cmd = if (os.isWindows) "basisu.exe" else "basisu"
    
    // Check project property first, then PATH
    project.findProperty("basisuPath")?.toString() ?: try {
        val process = ProcessBuilder(if (os.isWindows) listOf("where", cmd) else listOf("which", cmd)).start()
        process.inputStream.bufferedReader().readLine()?.trim()
    } catch (e: Exception) {
        null
    }
}

// Helper for group texture conversion (batch mode)
fun convertBatch(name: String, pngDir: Any, outSubdir: String, vararg convertArgs: String) =
    tasks.register<JavaExec>(name) {
        group = "build"
        mainClass.set("com.oddlabs.imageutil.Convert")
        classpath = converter
        
        val inDir = if (pngDir is Provider<*>) pngDir.get().toString() else pngDir.toString()
        val outDir = layout.buildDirectory.dir("textures/$outSubdir").get().asFile.absolutePath
        
        args(inDir, *convertArgs, outDir)
        jvmArgs("-esa", "-ea", "-Xmx512m", "-Djava.awt.headless=true", "--enable-native-access=ALL-UNNAMED")
        if (basisuPath != null) {
            jvmArgs("-Dbasisu.path=$basisuPath")
        }
        
        inputs.dir(pngDir)
        outputs.dir(layout.buildDirectory.dir("textures/$outSubdir"))
    }

// Helper for specific single texture conversion
fun convertTexture(name: String, png: Any, outSubdir: String, vararg convertArgs: String) =
    tasks.register<JavaExec>(name) {
        group = "build"
        mainClass.set("com.oddlabs.imageutil.Convert")
        classpath = converter
        
        val inFile = if (png is Provider<*>) png.get().toString() else png.toString()
        val inFileName = if (png is Provider<*>) {
            val p = png as Provider<*>
            val f = p.get()
            if (f is RegularFile) f.asFile.nameWithoutExtension else (f as File).nameWithoutExtension
        } else {
            File(png.toString()).nameWithoutExtension
        }
        
        val outDir = layout.buildDirectory.dir("textures/$outSubdir").get().asFile
        val outFile = File(outDir, "$inFileName.dds")
        
        args(inFile, *convertArgs, outFile.absolutePath)
        jvmArgs("-esa", "-ea", "-Xmx512m", "-Djava.awt.headless=true", "--enable-native-access=ALL-UNNAMED")
        if (basisuPath != null) {
            jvmArgs("-Dbasisu.path=$basisuPath")
        }
        
        inputs.file(png)
        outputs.file(outFile)
    }

// 1. Model Textures
val convertModelTextures = convertBatch("convertModelTextures",
    "textures/models", "models",
    "-flip", "-gamma", "0.45454545454545453", "-mipmaps", "-gamma", "2.2", "-format", "dds")

val convertTeamDecals = convertBatch("convertTeamDecals",
    "textures/teamdecals", "models",
    "-half", "-flip", "-mipmaps", "-format", "dds")

// 2. GUI Textures
val convertGui = convertBatch("convertGui", "textures/gui", "gui", "-flip", "-format", "dds")
val convertPixelPerfect = convertBatch("convertPixelPerfect", "textures/pixelperfect", "gui", "-flip", "-format", "dds")

// 3. Fonts
val fontInfoDir = layout.buildDirectory.dir("resources/font")
val fontTexClasspath = "/textures/font"

val renderTahomaFont = tasks.register<JavaExec>("renderTahomaFont") {
    group = "build"
    description = "Renders Tahoma TTF font to PNG texture and font metadata."
    mainClass.set("com.oddlabs.fontutil.FontRenderer")
    classpath = converter
    jvmArgs("-esa", "-ea", "-Xmx512m", "-Djava.awt.headless=true", "--enable-native-access=ALL-UNNAMED")

    val ttfFile = file("font/tahoma.ttf")
    val outDir = layout.buildDirectory.dir("font_png/tahoma")

    outputs.files(
        fontInfoDir.get().file("tahoma_13.font"),
        outDir.get().file("tahoma_13.png")
    )

    args = listOf(ttfFile.absolutePath, "13", "1024", "1200", "2", fontInfoDir.get().asFile.absolutePath, outDir.get().asFile.absolutePath, fontTexClasspath, "…–—•°™∞␡␈c←↑→↓⌃⇧⌥⌘□")
    onlyIf { ttfFile.exists() }
}

val convertTahomaFont = tasks.register<JavaExec>("convertTahomaFont") {
    group = "build"
    dependsOn(renderTahomaFont)
    mainClass.set("com.oddlabs.imageutil.Convert")
    classpath = converter
    val inFile = layout.buildDirectory.file("font_png/tahoma/tahoma_13.png").get().asFile
    val outFile = layout.buildDirectory.file("textures/font/tahoma_13.dds").get().asFile
    args(inFile.absolutePath, "-flip", "-format", "dds", outFile.absolutePath)
    jvmArgs("-esa", "-ea", "-Xmx512m", "-Djava.awt.headless=true", "--enable-native-access=ALL-UNNAMED")
    outputs.file(outFile)
    onlyIf { file("font/tahoma.ttf").exists() }
}

val renderImpactFont = tasks.register<JavaExec>("renderImpactFont") {
    group = "build"
    description = "Renders Impact TTF font to PNG texture and font metadata."
    mainClass.set("com.oddlabs.fontutil.FontRenderer")
    classpath = converter
    jvmArgs("-esa", "-ea", "-Xmx512m", "-Djava.awt.headless=true", "--enable-native-access=ALL-UNNAMED")

    val ttfFile = file("font/impact.ttf")
    val outDir = layout.buildDirectory.dir("font_png/impact")

    outputs.files(
        fontInfoDir.get().file("impact_24.font"),
        outDir.get().file("impact_24.png")
    )

    args = listOf(ttfFile.absolutePath, "24", "1024", "600", "2", fontInfoDir.get().asFile.absolutePath, outDir.get().asFile.absolutePath, fontTexClasspath, "…–—•°™∞␡␈c←↑→↓⌃⇧⌥⌘□")
    onlyIf { ttfFile.exists() }
}

val convertImpactFont = tasks.register<JavaExec>("convertImpactFont") {
    group = "build"
    dependsOn(renderImpactFont)
    mainClass.set("com.oddlabs.imageutil.Convert")
    classpath = converter
    val inFile = layout.buildDirectory.file("font_png/impact/impact_24.png").get().asFile
    val outFile = layout.buildDirectory.file("textures/font/impact_24.dds").get().asFile
    args(inFile.absolutePath, "-flip", "-format", "dds", outFile.absolutePath)
    jvmArgs("-esa", "-ea", "-Xmx512m", "-Djava.awt.headless=true", "--enable-native-access=ALL-UNNAMED")
    outputs.file(outFile)
    onlyIf { file("font/impact.ttf").exists() }
}

// Fallback fonts (Inter) - always built so the game works without Tahoma/Impact TTFs
val renderInterLightFont = tasks.register<JavaExec>("renderInterLightFont") {
    group = "build"
    description = "Renders Inter Light TTF font to PNG texture and font metadata."
    mainClass.set("com.oddlabs.fontutil.FontRenderer")
    classpath = converter
    jvmArgs("-esa", "-ea", "-Xmx512m", "-Djava.awt.headless=true", "--enable-native-access=ALL-UNNAMED")

    val ttfFile = file("font/Inter-Light.ttf")
    val outDir = layout.buildDirectory.dir("font_png/light")

    inputs.file(ttfFile)
    outputs.files(
        fontInfoDir.get().file("inter-light_13.font"),
        outDir.get().file("inter-light_13.png")
    )

    args = listOf(ttfFile.absolutePath, "13", "2048", "1200", "2", fontInfoDir.get().asFile.absolutePath, outDir.get().asFile.absolutePath, fontTexClasspath, "…–—•°™∞␡␈c←↑→↓⌃⇧⌥⌘□")
    onlyIf { ttfFile.exists() }
}

val convertInterLightFont = convertTexture("convertInterLightFont",
    layout.buildDirectory.dir("font_png/light").map { it.file("inter-light_13.png") }, "font", "-flip", "-format", "dds")
convertInterLightFont.configure { dependsOn(renderInterLightFont) }

val renderInterTightBlackFont = tasks.register<JavaExec>("renderInterTightBlackFont") {
    group = "build"
    description = "Renders Inter Tight Black TTF font to PNG texture and font metadata."
    mainClass.set("com.oddlabs.fontutil.FontRenderer")
    classpath = converter
    jvmArgs("-esa", "-ea", "-Xmx512m", "-Djava.awt.headless=true", "--enable-native-access=ALL-UNNAMED")

    val ttfFile = file("font/InterTight-Black.ttf")
    val outDir = layout.buildDirectory.dir("font_png/black")

    inputs.file(ttfFile)
    outputs.files(
        fontInfoDir.get().file("intertight-black_28.font"),
        outDir.get().file("intertight-black_28.png")
    )

    args = listOf(ttfFile.absolutePath, "28", "2048", "1200", "2", fontInfoDir.get().asFile.absolutePath, outDir.get().asFile.absolutePath, fontTexClasspath, "…–—•°™∞␡␈c←↑→↓⌃⇧⌥⌘□")
    onlyIf { ttfFile.exists() }
}

val convertInterTightBlackFont = convertTexture("convertInterTightBlackFont",
    layout.buildDirectory.dir("font_png/black").map { it.file("intertight-black_28.png") }, "font", "-flip", "-format", "dds")
convertInterTightBlackFont.configure { dependsOn(renderInterTightBlackFont) }

// 4. Geometry
val geometry = tasks.register<JavaExec>("geometry") {
    group = "build"
    description = "Converts XML geometry definitions to binary format."
    mainClass.set("com.oddlabs.converter.ConvertToBinary")
    classpath = converter
    val outDir = layout.buildDirectory.dir("geometry_bin")
    inputs.file("geometry/geometry.xml")
    inputs.dir("geometry")
    outputs.dir(outDir)
    args = listOf("geometry.xml", "geometry", outDir.get().asFile.absolutePath)
    jvmArgs("-esa", "-ea", "-Xmx512m", "-Djava.awt.headless=true", "--enable-native-access=ALL-UNNAMED")
}

val textures = tasks.register("textures") {
    dependsOn(convertModelTextures, convertTeamDecals,
              convertTahomaFont, convertImpactFont,
              convertInterLightFont, convertInterTightBlackFont,
              convertGui, convertPixelPerfect)
}

tasks.processResources {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    dependsOn(geometry, textures)
    
    from(layout.buildDirectory.dir("geometry_bin")) { into("geometry") }
    from(fontInfoDir) { into("font") }
    from(layout.buildDirectory.dir("textures/font")) { into("textures/font") }
    from(layout.buildDirectory.dir("textures/models")) { into("textures/models") }
    from(layout.buildDirectory.dir("textures/gui")) { into("textures/gui") }
    
    // RAW PNGs for Cursors (required by PointerInput)
    from("textures/pointer") { into("textures/gui") }
    
    // Essential static assets
    from("geometry") { into("geometry") }
    from("widget") { into("widget") }
    from("schemas") { into("schemas") }
    from("font") { into("font") }
    
    from("textures") {
        into("textures")
        exclude("models/**", "teamdecals/**", "gui/**", "pointer/**", "pixelperfect/**")
    }
}
