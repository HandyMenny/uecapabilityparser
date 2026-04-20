import com.diffplug.gradle.spotless.SpotlessDiagnoseTask
import com.diffplug.spotless.LineEnding
import java.util.*

plugins {
    val kotlinVersion = "2.3.20"
    kotlin("jvm") version kotlinVersion
    kotlin("plugin.serialization") version kotlinVersion
    id("com.gradleup.shadow") version "9.4.1"
    id("com.diffplug.spotless") version "8.4.0"
    id("org.jetbrains.kotlinx.kover") version "0.9.8"
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
    val mtsAsn1Version = "5bd77b9"
    val javalinVersion = "7.2.0"
    val coroutinesVersion = "1.10.2"
    val kotlinxSerializationVer = "1.11.0"

    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:$coroutinesVersion")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$kotlinxSerializationVer")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-protobuf:$kotlinxSerializationVer")
    implementation("com.github.handymenny.mts-asn1:mts-asn1-converter:$mtsAsn1Version")
    implementation("com.github.handymenny.mts-asn1:mts-asn1-per:$mtsAsn1Version")
    implementation("com.github.handymenny.mts-asn1:mts-asn1-kotlin-per:$mtsAsn1Version")
    implementation("com.github.handymenny.mts-asn1:mts-asn1-kotlinx-json:$mtsAsn1Version")
    implementation("org.slf4j:slf4j-nop:2.0.17")
    implementation("com.github.ajalt.clikt:clikt:5.1.0")
    implementation("io.javalin:javalin:$javalinVersion")
    implementation("com.github.handymenny.pkts:pkts-core:8f78357")

    testImplementation("io.javalin:javalin-testtools:$javalinVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesVersion")
    testImplementation("io.mockk:mockk:1.14.9")
    testImplementation("org.junit.jupiter:junit-jupiter:6.0.3")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    tstypesImplementation("dev.adamko.kxstsgen:kxs-ts-gen-core:0.2.4")
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
        // Raise max heap size to 1G (default 512 MB)
        maxHeapSize = "1G"
    }

    shadowJar {
        // slf4j-nop to silence slf4j warning, mordant-jvm-jna-jvm for clikt
        val keepDependencies =
            listOf("org.slf4j:slf4j-nop:.*", "com.github.ajalt.mordant:mordant-jvm-jna-jvm:.*")
        // Enable shadow minify
        minimize { keepDependencies.forEach { exclude(dependency(it)) } }
    }

    register("genTsTypes", JavaExec::class) {
        mainClass.set("it.smartphonecombo.uecapabilityparser.TsTypesGenerator")
        classpath = sourceSets["tstypes"].runtimeClasspath
    }
}

application { mainClass.set("it.smartphonecombo.uecapabilityparser.cli.Main") }

kotlin { jvmToolchain(17) }

spotless {
    format("misc") {
        target("*.md", ".gitignore", "**/*.csv")
        trimTrailingWhitespace()
        leadingTabsToSpaces()
        endWithNewline()
    }

    kotlin { ktfmt().kotlinlangStyle() }

    kotlinGradle { ktfmt().kotlinlangStyle() }

    // all src files have linux line endings
    lineEndings = LineEnding.UNIX
}

tasks.withType<SpotlessDiagnoseTask>().configureEach {
    notCompatibleWithConfigurationCache(
        "Spotless Diagnose doesn't support the configuration cache."
    )
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

publishing { publications { create<MavenPublication>("maven") { from(components["java"]) } } }
