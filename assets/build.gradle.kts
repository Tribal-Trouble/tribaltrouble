import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

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

// Texture groups: srcSubdir -> (outSubdir, operations...)
val textureGroups = mapOf(
    "pixelperfect" to listOf("gui", "-flip", "-format", "image"),
    "gui"          to listOf("gui", "-flip", "-format", "dxtn"),
    "pointer"      to listOf("gui", "-flip", "-format", "image"),
    "effects"      to listOf("effects", "-flip", "-mipmaps", "-format", "dxtn"),
    "font"         to listOf("font", "-flip", "-format", "dxtn"),
    "models"       to listOf("models", "-flip", "-gamma", "0.45454545454545453", "-mipmaps", "-gamma", "2.2", "-format", "dxtn"),
    "teamdecals"   to listOf("models", "-half", "-flip", "-mipmaps", "-format", "dxtn"),
)

val textures = tasks.register("textures") {
    group = "assets"
    description = "Convert all textures (parallel processes)"
    dependsOn(assetTools)

    inputs.dir(srcTextures)
    outputs.dir(buildTextures)

    doLast {
        // Build the list of conversions: [infile, ops..., outdir]
        data class Job(val infile: File, val args: List<String>)
        val jobs = mutableListOf<Job>()
        for ((srcSubdir, group) in textureGroups) {
            val outSubdir = group[0]
            val ops = group.drop(1)
            val outDir = buildTextures.resolve(outSubdir)
            outDir.mkdirs()
            srcTextures.resolve(srcSubdir).walkTopDown()
                .filter { it.extension == "png" }
                .forEach { png ->
                    jobs.add(Job(png, listOf(png.absolutePath) + ops + listOf(outDir.absolutePath)))
                }
        }

        // Resolve classpath once
        val cp = project.configurations["assetTools"].resolve().joinToString(File.pathSeparator) { it.absolutePath }
        val javaExe = org.gradle.internal.jvm.Jvm.current().javaExecutable.absolutePath
        val workers = Runtime.getRuntime().availableProcessors()

        println("Converting ${jobs.size} textures with $workers parallel processes")

        // Process in parallel using a fixed thread pool of ProcessBuilders
        val pool = Executors.newFixedThreadPool(workers)
        val failed = AtomicInteger(0)
        val done = AtomicInteger(0)
        val total = jobs.size

        jobs.forEach { job ->
            pool.submit(Runnable {
                val cmd = listOf(javaExe, "-ea", "-Djava.awt.headless=true", "-cp", cp,
                    "com.oddlabs.imageutil.Convert") + job.args
                val proc = ProcessBuilder(cmd)
                    .redirectErrorStream(true)
                    .start()
                val output = proc.inputStream.bufferedReader().readText()
                val exitCode = proc.waitFor()
                if (exitCode != 0) {
                    System.err.println("FAILED: ${job.infile.name}\n$output")
                    failed.incrementAndGet()
                }
                val n = done.incrementAndGet()
                if (n % 20 == 0 || n == total) {
                    println("  $n/$total")
                }
            })
        }

        pool.shutdown()
        pool.awaitTermination(10, TimeUnit.MINUTES)

        if (failed.get() > 0) {
            throw GradleException("${failed.get()} texture(s) failed to convert")
        }
    }
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
