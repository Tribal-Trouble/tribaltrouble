 import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("net.ltgt.errorprone") version "4.3.0" apply false
    id("net.ltgt.nullaway") version "2.3.0" apply false
    id("com.smushytaco.lwjgl3") version "1.0.1" apply false
}

allprojects {
    group = "com.oddlabs.tribaltrouble"
    version = "1.0"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "net.ltgt.errorprone")

    dependencies {
        implementation("org.jspecify:jspecify:1.0.0")
        "errorprone"("com.google.errorprone:error_prone_core:2.46.0")
        "errorprone"("com.uber.nullaway:nullaway:0.12.15")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }

    tasks.withType<JavaCompile>().configureEach {
        options.errorprone {
            option("NullAway:AnnotatedPackages", "com.oddlabs")

            // Errorprone has decided it wants to throw a class cast exception
            disableAllChecks = true

            disable( "NullAway", "IntLongMath", "MissingCasesInEnumSwitch", "ImmutableEnumChecker",
                "NarrowingCompoundAssignment", "InstanceOfAndCastMatchWrongType",
                "TimeUnitConversionChecker", "UnusedNestedClass", "SameNameButDifferent", "AssignmentExpression",
                "NullablePrimitive", "ObjectToString", "FallThrough", "ByteBufferBackingArray",
                "InputStreamSlowMultibyteRead", "BadComparable", "CatchAndPrintStackTrace",
                "ModifyCollectionInEnhancedForLoop", "StringCaseLocaleUsage", "IdentityBinaryExpression",
                "EqualsHashCode", "MissingSummary", "JavaUtilDate", "DoNotCallSuggester",
                "MutablePublicArray", "InconsistentCapitalization", "OperatorPrecedence",
                "TypeParameterUnusedInFormals", "PatternMatchingInstanceof", "DefaultCharset", "EmptyCatch",
                "MissingOverride", "NarrowCalculation", "EqualsUnsafeCast", "StatementSwitchToExpressionSwitch",
                "EnumOrdinal", "JdkObsolete", "UnnecessaryParentheses", "UnusedMethod", "UnusedVariable",
                "EffectivelyPrivate")
        }
    }
}
