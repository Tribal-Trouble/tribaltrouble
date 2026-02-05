import com.smushytaco.lwjgl_gradle.Module

plugins {
    id("com.smushytaco.lwjgl3")
}

lwjgl {
    // Strongly recommended: set LWJGL version explicitly
    version = "3.4.1"

    // Add LWJGL modules + the correct native artifacts
    implementation(
        Module.CORE,
        Module.OPENGL,
        Module.STB)
}

dependencies {
    implementation(project(":common"))
}
