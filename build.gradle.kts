plugins {
    java
}

allprojects {
    group = "com.oddlabs.tribaltrouble"
    version = "1.0"
}

subprojects {
    apply(plugin = "java")

    java {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}
