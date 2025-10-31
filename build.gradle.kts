plugins {
    java
}

allprojects {
    group = "com.oddlabs.tribaltrouble"
    version = "1.0"
    repositories { mavenCentral() }
}

subprojects {
    plugins.apply("java")

    dependencies {
        implementation("org.jspecify:jspecify:1.0.0")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
}
