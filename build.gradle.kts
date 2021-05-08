import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.32"
}

group = "com.unicorn"
version = "1.3.1"

repositories {
    mavenCentral()
    maven("https://oss.sonatype.org/content/repositories/snapshots")
    maven {
        name = "Kotlin Discord"
        url = uri("https://maven.kotlindiscord.com/repository/maven-public/")
    }
}

val exposedVersion: String by project
dependencies {
    testImplementation(kotlin("test-junit"))
    implementation("com.kotlindiscord.kord.extensions:kord-extensions:1.4.0-SNAPSHOT")
    implementation("io.github.microutils:kotlin-logging:2.0.6")
    implementation("org.slf4j:slf4j-simple:1.7.30")
    implementation("org.jetbrains.exposed", "exposed-core", "0.31.1")
    implementation("org.jetbrains.exposed", "exposed-dao", "0.31.1")
    implementation("org.jetbrains.exposed", "exposed-jdbc", "0.31.1")
    implementation("org.xerial:sqlite-jdbc:3.30.1")
    implementation(kotlin("script-runtime"))
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-client-java:1.5.4")
}

tasks.test {
    useJUnit()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions.jvmTarget = "9"
}
