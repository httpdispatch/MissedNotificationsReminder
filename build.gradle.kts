// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.0.1")
        classpath(kotlin("gradle-plugin", version = "1.4.0"))
    }
    // Exclude the lombok version that the android plugin depends on.
    configurations.classpath {
        exclude(group = "com.android.tools.external.lombok")
    }
}

allprojects {
    repositories {
        google()
        mavenCentral()
        jcenter()
        maven (url ="https://jitpack.io" )
    }
}
// Kotlin DSL
tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().all {
    kotlinOptions.freeCompilerArgs += listOf(
            "-Xopt-in=kotlin.RequiresOptIn",
            "-Xopt-in=kotlin.OptIn"
    )
}
tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}