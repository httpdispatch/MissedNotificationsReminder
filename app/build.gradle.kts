import org.jetbrains.kotlin.config.KotlinCompilerVersion
import java.io.FileInputStream
import java.util.*

plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("android.extensions")
    kotlin("kapt")
}
// Manifest version information!
val versionMajor = 1
val versionMinor = 4
val versionPatch = 1
val versionBuild = 0 // bump for dogfood builds, public betas, etc.

val gitSha = "git rev-parse --short HEAD".runCommand(project.rootDir).trim()
val gitTimestamp = "git log -n 1 --format=%at".runCommand(project.rootDir).trim()
// whether the build environment is Travis CI
val isTravis = "true" == System.getenv("TRAVIS")
val preDexEnabled = "true" == System.getProperty("pre-dex", "true")
// get the keystore properties file
val propsFile = rootProject.file("keystore.properties")

// method to generate version code depend on min sdk version
fun getVersionCode(minSdkVersion: Int) =
        2000000000 + versionMajor * 10000000 + versionMinor * 100000 + versionPatch * 1000 + versionBuild * 100 + minSdkVersion

android {
    compileSdkVersion(Constants.COMPILE_SDK_VERSION)
    buildToolsVersion = Constants.BUILD_TOOLS_VERSION
    buildFeatures {
        viewBinding = true
        dataBinding = true
    }

    dexOptions {
        // Skip pre-dexing when running on Travis CI or when disabled via -Dpre-dex=false.
        preDexLibraries = preDexEnabled && !isTravis
    }

    signingConfigs {
        create("release") {
            when {
                propsFile.exists() -> {
                    // properties file exists.
                    // read signing key from properties file
                    val props = Properties()
                    props.load(FileInputStream(propsFile))
                    storeFile = rootProject.file(props.getProperty("storeFile"))
                    storePassword = props.getProperty("storePassword")
                    keyAlias = props.getProperty("keyAlias")
                    keyPassword = props.getProperty("keyPassword")
                }
                System.console() != null -> {
                    // console is available, ask user to input key information manually
                    storeFile = rootProject.file(System.console().readLine("\nKeystore file path: "))
                    storePassword = System.console().readLine("\nKeystore password: ")
                    keyAlias = System.console().readLine("\nKey alias: ")
                    keyPassword = System.console().readLine("\nKey password: ")
                }
                else -> {
                    // missing keystore information, and no console is available. Will cause error
                    // during build
                    storeFile = file("missing.keystore")
                    storePassword = ""
                    keyAlias = ""
                    keyPassword = ""
                }
            }
        }
    }

    defaultConfig {
        applicationId = "com.app.missednotificationsreminder"
        targetSdkVersion(Constants.TARGET_SDK_VERSION)
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}"
        multiDexEnabled = true

        buildConfigField("String", "GIT_SHA", "\"${gitSha}\"")
        buildConfigField("String", "BUILD_TIME", "\"${buildTime()}\"")
        buildConfigField("long", "GIT_TIMESTAMP", "${gitTimestamp}L")

        signingConfig = signingConfigs.getByName("release")

        testInstrumentationRunner = "com.app.missednotificationsreminder.ApplicationTestRunner"
    }
    buildTypes {
        getByName("debug") {
            // the debug application will have separate suffix in package so both debug and release
            // can be installed at the same time
            applicationIdSuffix = ".debug"
        }

        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")

            // generate more meaningful app file name in format
            // <Project_Name>-production-v<Version_Name>.apk.
            // Example: MissingNOtificationsReminder-release-v1.0.0.apk
            applicationVariants.all {
                val variant = this
                variant.outputs.onEach { output ->
                    output as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                    var newName = output.outputFileName
                    newName = newName.replace("app", rootProject.name)
                    newName = newName.replace("release", "release-v" + variant.mergedFlavor.versionName)
                    output.outputFileName = newName
                }
            }
        }
    }
    flavorDimensions("service", "api")
    productFlavors {
        create("accessibility") {
            dimension = "service"
            versionCode = 0
        }
        create("notificationListener") {
            dimension = "service"
            versionCode = 1
        }
        create("v14") {
            minSdkVersion(14)
            versionCode = 1
            dimension = "api"
        }
        create("v18") {
            minSdkVersion(18)
            versionCode = 2
            dimension = "api"
        }
        create("v27") {
            minSdkVersion(27)
            dimension = "api"
            versionCode = 3
        }
    }

    lintOptions {
        textReport = true
        textOutput("stdout")
        fatal("UnusedResources")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    applicationVariants.all {
        val variant = this
        variant.outputs
                .map { it as com.android.build.gradle.internal.api.BaseVariantOutputImpl }
                .forEach { output ->
                    // get the version code of each flavor
                    val serviceVersion = variant.productFlavors[0].versionCode!!
                    val apiVersion = variant.productFlavors[1].versionCode!!

                    output as com.android.build.gradle.internal.api.ApkVariantOutputImpl
                    // set the composite code
                    output.versionCodeOverride = getVersionCode(apiVersion * 10 + serviceVersion)
                    output.versionNameOverride = "${versionMajor}.${versionMinor}.${versionPatch}.${versionBuild}.${apiVersion}${serviceVersion}"
                }
    }
}

configurations.onEach {
    it.resolutionStrategy {
    }
}

dependencies {
    implementation(kotlin("stdlib-jdk7", KotlinCompilerVersion.VERSION))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.COROUTINES_VERSION}")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:${Versions.COROUTINES_VERSION}")

    implementation("androidx.multidex:multidex:2.0.1")
    implementation("androidx.legacy:legacy-support-v4:1.0.0")
    implementation("androidx.annotation:annotation:1.1.0")
    implementation("androidx.appcompat:appcompat:1.0.2")
    implementation("androidx.recyclerview:recyclerview:1.1.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("com.google.android.material:material:1.0.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:${Versions.LIFECYCLE_VERSION}")
    implementation("androidx.lifecycle:lifecycle-service:${Versions.LIFECYCLE_VERSION}")

    implementation("com.jakewharton.timber:timber:4.7.1")
    implementation("androidx.navigation:navigation-fragment-ktx:2.2.2")
    implementation("androidx.navigation:navigation-ui-ktx:2.2.2")

    debugImplementation("com.jakewharton.madge:madge:1.1.4")
    debugImplementation("com.jakewharton.scalpel:scalpel:1.1.2")
    implementation("com.jakewharton.threetenabp:threetenabp:1.2.4")
    implementation("com.squareup.okio:okio:2.6.0")
    debugImplementation("com.mattprecious.telescope:telescope:2.2.0@aar")

    // Dagger
    implementation("com.google.dagger:dagger:${Versions.DAGGER}")
    kapt("com.google.dagger:dagger-compiler:${Versions.DAGGER}")
    implementation("com.google.dagger:dagger-android-support:${Versions.DAGGER}")
    kapt("com.google.dagger:dagger-android-processor:${Versions.DAGGER}")
    // Using Dagger in androidTest and Robolectric too
    kaptAndroidTest("com.google.dagger:dagger-compiler:${Versions.DAGGER}")
    kaptTest("com.google.dagger:dagger-compiler:${Versions.DAGGER}")

    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.4")
    releaseImplementation("com.squareup.leakcanary:leakcanary-android-no-op:1.6.3")

    implementation("com.squareup.picasso:picasso:2.71828")

    implementation("io.reactivex:rxjava:1.3.8")
    implementation("io.reactivex:rxandroid:1.2.1")
    implementation("com.tbruyelle.rxpermissions:rxpermissions:0.7.0@aar")
    implementation("com.jakewharton.rxbinding:rxbinding:1.0.1")

    implementation("com.f2prateek.rx.preferences:rx-preferences:1.0.2")

    implementation("com.appyvet:materialrangebar:1.3")
    implementation("com.wdullaer:materialdatetimepicker:4.2.3")

    implementation("com.evernote:android-job:1.4.2")

    androidTestImplementation("junit:junit:4.13")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.2.0")
    androidTestImplementation("androidx.test:runner:1.2.0")
    androidTestImplementation("androidx.test:rules:1.2.0")
    // Espresso-contrib for DatePicker, RecyclerView, Drawer actions, Accessibility checks, CountingIdlingResource
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.2.0") {
        exclude(group = "com.android.support", module = "appcompat")
        exclude(group = "com.android.support", module = "support-v4")
        exclude(module = "recyclerview-v7")
    }

    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.COROUTINES_VERSION}")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:${Versions.COROUTINES_VERSION}")
    testImplementation("junit:junit:4.13")
    testImplementation("org.mockito:mockito-core:3.3.3")
    testImplementation("com.google.truth:truth:1.0.1")
}