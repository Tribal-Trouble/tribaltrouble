 import net.ltgt.gradle.errorprone.errorprone

plugins {
    java
    id("net.ltgt.errorprone") version "5.1.0" apply false
    id("net.ltgt.nullaway") version "3.0.0" apply false
    id("com.smushytaco.lwjgl3") version "1.0.2" apply false
}

allprojects {
    group = "com.oddlabs.tribaltrouble"
    version = "2.0"
}

subprojects {
    apply(plugin = "java")
    apply(plugin = "net.ltgt.errorprone")

    dependencies {
        implementation("org.jspecify:jspecify:1.0.0")
        "errorprone"("com.google.errorprone:error_prone_core:2.48.0")
        "errorprone"("com.uber.nullaway:nullaway:0.13.1")
    }

    java {
        sourceCompatibility = JavaVersion.VERSION_26
        targetCompatibility = JavaVersion.VERSION_26
        modularity.inferModulePath.set(true)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.errorprone {
            option("NullAway:AnnotatedPackages", "com.oddlabs")

            // Errorprone has decided it wants to throw a class cast exception
            disableAllChecks = false

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
                "EffectivelyPrivate", "ArrayRecordComponent", "StringSplitter", "InterruptedInCatchBlock" )
        }
    }
}
