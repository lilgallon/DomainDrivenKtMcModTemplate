import java.text.SimpleDateFormat
import java.util.Date

buildscript {
    repositories {
        maven("https://maven.minecraftforge.net")
        mavenCentral()
    }
    dependencies {
        classpath("net.minecraftforge.gradle:ForgeGradle:5.+")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinGradlePlugin")
    }
}

repositories {
    maven("https://thedarkcolour.github.io/KotlinForForge/")
}

plugins {
    java
    kotlin("jvm")
    id("net.minecraftforge.gradle") version forgeGradlePlugin
}

val library = configurations.create("library")
val inJar = configurations.create("inJar")
configurations.implementation.extendsFrom(inJar)
library.extendsFrom(inJar)

group = "$modGroup.forge"
version = modVersion

minecraft.runs.all {
    lazyToken("minecraft_classpath") {
        configurations["library"]
            .copyRecursive()
            .resolve()
            .joinToString(File.pathSeparator) { it.absolutePath }
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("thedarkcolour:kotlinforforge:$kotlinForForge")

    library("org.jetbrains.kotlin", "kotlin-stdlib-jdk8", kotlinVersion) {
        exclude("org.jetbrains", "annotations")
    }
    library("org.jetbrains.kotlin", "kotlin-reflect", kotlinVersion) {
        exclude("org.jetbrains", "annotations")
    }
    library("org.jetbrains.kotlinx", "kotlinx-coroutines-core", kotlinCoroutinesVersion) {
        exclude("org.jetbrains", "annotations")
    }
    library("org.jetbrains.kotlinx", "kotlinx-coroutines-jdk8", kotlinCoroutinesVersion) {
        exclude("org.jetbrains", "annotations")
    }
    library("org.jetbrains.kotlinx", "kotlinx-serialization-json", kotlinSerializationVersion) {
        exclude("org.jetbrains", "annotations")
    }

    minecraft("net.minecraftforge:forge:$minecraftVersion-$forgeVersion")

    inJar(project(":core")) {
        // You may want to exclude some libs that are already in the classpath if you have a duplication error when
        // running runClient gradle task
        // exclude("com.google.guava")
    }
}

val Project.minecraft: net.minecraftforge.gradle.common.util.MinecraftExtension
    get() = extensions.getByType()

minecraft.let {
    it.mappings("official", minecraftVersion)
    it.runs {
        create("client") {
            workingDirectory(project.file("run"))
            property("forge.logging.console.level", "debug")
            mods {
                create(modId) {
                    source(sourceSets.main.get())
                }
            }
        }
    }
}

tasks {
    val javaVersion = JavaVersion.valueOf("VERSION_$jvmTarget")
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
        if (JavaVersion.current().isJava9Compatible) {
            options.release.set(javaVersion.toString().toInt())
        }
    }

    compileKotlin {
        kotlinOptions.jvmTarget = jvmTarget
    }

    java {
        toolchain {
            languageVersion.set(JavaLanguageVersion.of(javaVersion.toString()))
        }
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }

    jar {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        from(
            inJar.map {
                if (it.isDirectory) it else zipTree(it)
            }
        )
        archiveBaseName.set("$modArchive-forge")
        manifest {
            attributes(
                mapOf(
                    "Implementation-Title" to project.name,
                    "Implementation-Version" to project.version,
                    "Implementation-Timestamp" to SimpleDateFormat("yyyy-MM-dd").format(Date())
                )
            )
        }
        finalizedBy("reobfJar")
    }
}
