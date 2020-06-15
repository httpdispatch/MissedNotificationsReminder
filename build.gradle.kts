// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        //maven {
        //  url "https://oss.sonatype.org/content/repositories/snapshots/"
        //}
        mavenCentral()
        jcenter()
        google()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.0.0")
        classpath(kotlin("gradle-plugin", version = Constants.kotlinVersion))
    }
    // Exclude the lombok version that the android plugin depends on.
    configurations.classpath {
        exclude(group = "com.android.tools.external.lombok")
    }
}

allprojects {
    repositories {
        //maven {
        //  url "https://oss.sonatype.org/content/repositories/snapshots/"
        //}
        mavenCentral()
        jcenter()
        maven(
                "https://maven.google.com"
        )
        google()
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}