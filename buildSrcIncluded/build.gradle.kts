plugins {
    `kotlin-dsl`
    `java-gradle-plugin`
}
repositories {
    mavenCentral()
}
gradlePlugin {
    // Add fake plugin, if you don't have any
    plugins.register("class-loader-plugin") {
        id = "class-loader-plugin"
        implementationClass = "ClassLoaderPlugin"
    }
}