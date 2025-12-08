plugins {
    kotlin("jvm") version "2.0.21"
    kotlin("plugin.serialization") version "2.0.21"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("io.nats:jnats:2.16.7")
}

application {
    mainClass.set("rocketflow.MainKt")
}

kotlin {
    jvmToolchain(21)
}
