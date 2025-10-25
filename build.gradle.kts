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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    
    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
    }
}
