import com.diffplug.spotless.LineEnding
import java.util.*

plugins {
    val kotlinVersion = "2.0.0"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.github.johnrengelman.shadow") version "8.1.1"
    id("com.diffplug.spotless") version "6.22.0"
    id("org.jetbrains.kotlinx.kover") version "0.8.1"
    id("maven-publish")
    application
}

repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}

sourceSets {
    create("tstypes") {
        compileClasspath += sourceSets.main.get().output
        runtimeClasspath += sourceSets.main.get().output
    }
}

val tstypesImplementation: Configuration by
    configurations.getting { extendsFrom(configurations.implementation.get()) }

dependencies {
    val mtsAsn1Version = "45cc560"
    val javalinVersion = "6.1.6"
    val coroutinesVersion = "1.8.1"
    val kotlinxSerializationVer = "1.7.0"

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVer")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVer")
    implementation("com.github.handymenny.mts-asn1:mts-asn1-converter:$mtsAsn1Version")
    implementation("com.github.handymenny.mts-asn1:mts-asn1-per:$mtsAsn1Version")
    implementation("com.github.handymenny.mts-asn1:mts-asn1-kotlinx-json:$mtsAsn1Version")
    implementation("org.slf4j:slf4j-nop:2.0.13")
    implementation("com.github.ajalt.clikt:clikt:4.4.0")

    implementation("io.javalin:javalin:$javalinVersion")
    testImplementation("io.javalin:javalin-testtools:$javalinVersion")
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.3")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("io.mockk:mockk:1.13.11")

    tstypesImplementation("dev.adamko.kxstsgen:kxs-ts-gen-core:0.2.4")

    implementation("com.github.handymenny.pkts:pkts-core:8f78357")
}

group = "parser"

val properties =
    Properties().apply {
        load(rootProject.file("src/main/resources/application.properties").reader())
    }

version = properties["project.version"]!!

description = "uecapabilityparser"

tasks {
    // Disable default dist zip
    distZip { enabled = false }
    // Disable default dist tar
    distTar { enabled = false }
    // Disable shadow dist tar (zip is enough)
    shadowDistTar { enabled = false }
    // Disable shadow default startScript
    startShadowScripts { enabled = false }
    // Disable default startScript
    startScripts { enabled = false }

    test {
        // Enable Junit test
        useJUnitPlatform()
        // generate kover report
        finalizedBy(named("koverXmlReport"))
    }

    shadowJar {
        // slf4j-nop to silence slf4j warning
        val keepDependencies = listOf("org.slf4j:slf4j-nop:.*")
        // Enable shadow minify
        minimize { keepDependencies.forEach { exclude(dependency(it)) } }
    }

    register("genTsTypes", JavaExec::class) {
        mainClass.set("it.smartphonecombo.uecapabilityparser.TsTypesGenerator")
        classpath = sourceSets["tstypes"].runtimeClasspath
    }
}

application { mainClass.set("it.smartphonecombo.uecapabilityparser.cli.Main") }

kotlin { jvmToolchain(11) }

spotless {
    format("misc") {
        target("*.md", ".gitignore", "**/*.csv")
        trimTrailingWhitespace()
        indentWithSpaces()
        endWithNewline()
    }

    kotlin { ktfmt().kotlinlangStyle() }

    kotlinGradle { ktfmt().kotlinlangStyle() }
}

distributions {
    named("shadow") {
        val projectName = project.name
        val projectVersion = project.version

        distributionBaseName.set(projectName)
        contents {
            from("src/main/dist")
            from("src/main/resources/swagger/openapi.json")
            from("uecapabilityparser.d.ts")
            from("LICENSE")
            from("README.md")
            eachFile {
                // Move all files to root and rename jar to uecapabilityparser.jar
                this.path =
                    this.path
                        .replace("${projectName}-${projectVersion}/", "")
                        .replace(""".*\.jar""".toRegex(), "${projectName}.jar")
            }
        }
    }
}

spotless {
    // Workaround: https://github.com/diffplug/spotless/issues/1644
    lineEndings = LineEnding.UNIX
}

publishing { publications { create<MavenPublication>("maven") { from(components["java"]) } } }
