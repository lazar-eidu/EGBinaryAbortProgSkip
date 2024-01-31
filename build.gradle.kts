import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.util.Properties

val localPropertiesFile = project.rootProject.file("local.properties")
val localProperties = Properties()
if (localPropertiesFile.canRead())
    localProperties.load(localPropertiesFile.inputStream())

buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.guardsquare:proguard-gradle:7.2.2")
    }
}

plugins {
    kotlin("jvm") version "1.7.0"
//    kotlin("plugin.serialization") version Versions.kotlin
    id("java-library")
    id("org.jlleitschuh.gradle.ktlint") version "10.3.0"
    id("com.palantir.git-version") version "0.15.0"
    id("com.github.johnrengelman.shadow").version("7.1.2")
    id("io.github.http-builder-ng.http-plugin") version "0.1.1"
    id("org.jetbrains.kotlin.plugin.serialization") version "1.6.21"
}

repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        // TODO update with repository identifier
        // url = uri("https://maven.pkg.github.com/EIDU/personalization-plugin-starter")
        url = uri("https://maven.pkg.github.com/lazar-eidu/EGBinaryAbortProgSkip")
        credentials {
            username = System.getenv("GITHUB_READPACKAGES_USER")
                ?: localProperties.getProperty("githubReadPackagesUser")
            password = System.getenv("GITHUB_READPACKAGES_TOKEN")
                ?: localProperties.getProperty("githubReadPackagesToken")
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_7
    targetCompatibility = JavaVersion.VERSION_1_7
}

dependencies {
    // Normally, compileOnly would be enough, because we don't need to package this library in our JAR. However,
    // that would cause Proguard to remove some references to the classes it can't find.
    implementation("com.eidu:personalization-plugin-interface:1.1.0")
    // KotlinX Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core:1.5.1")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")

    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk", "mockk", "1.12.4")
    testImplementation("com.willowtreeapps.assertk:assertk-jvm:0.25")
}

val gitVersion: groovy.lang.Closure<String> by extra

val pluginId = project.name
val pluginClass = "com.eidu.personalization.PluginImplementation"
val pluginProvider = "EIDU GmbH"
val pluginVersion = gitVersion().split('-').last()

val test by tasks.getting(Test::class) {
    useJUnit { }
}

tasks.withType<ShadowJar> {
    archiveBaseName.set("shadow")
    archiveClassifier.set("")
    archiveVersion.set("")

    exclude("META-INF/**", "**/*.kotlin_metadata", "**/*.kotlin_builtins")

    manifest {
        attributes["Plugin-Class"] = pluginClass
        attributes["Plugin-Id"] = pluginId
        attributes["Plugin-Version"] = pluginVersion
        attributes["Plugin-Provider"] = pluginProvider
    }
}

tasks.register<proguard.gradle.ProGuardTask>("proguard") {
    dependsOn("shadowJar")

    configuration("proguard-rules.pro")

    injars(tasks.named("shadowJar"))
    outjars("build/libs/stripped.jar")

    val javaHome = System.getProperty("java.home")
    libraryjars(
        mapOf(
            "jarfilter" to "!**.jar",
            "filter" to "!module-info.class"
        ),
        "$javaHome/jmods/java.base.jmod",
    )
}

task<Exec>("dex") {
    dependsOn("proguard")

    val buildToolsPath = (
        System.getenv("ANDROID_HOME") ?: localProperties.getProperty("sdk.dir")
        ) + "/build-tools/30.0.3"

    commandLine(
        "$buildToolsPath/dx",
        "--dex",
        "--output=build/libs/plugin.jar",
        "build/libs/stripped.jar"
    )
}

tasks.named("build") {
    finalizedBy(tasks.named("dex"))
}
