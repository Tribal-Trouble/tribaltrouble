plugins {
    java
}

allprojects {
    group = "com.oddlabs"
    version = "1.0"
    repositories { mavenCentral() }
}

subprojects {
    plugins.apply("java")
    
    java {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
}
