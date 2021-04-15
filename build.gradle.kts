// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
        jcenter()
    }
    dependencies {
        classpath("com.android.tools.build:gradle:4.1.3")
        classpath(kotlin("gradle-plugin", version = "1.4.0"))
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:2.3.5")
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
        maven(url = "https://jitpack.io")
    }
}

tasks.register<Delete>("clean").configure {
    delete(rootProject.buildDir)
}

tasks.register<Zip>("zipBackup") {
    archiveFileName.set("backup_" +
            java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")) +
            ".zip")
    from(project.rootDir) {
        include("*")
        include("app/**")
        exclude("**/build")
        exclude("backup**")
    }
    destinationDirectory.set(project.rootDir)
}
