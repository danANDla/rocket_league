plugins {
    kotlin("jvm") version "2.2.21"   // или актуальная
    kotlin("plugin.serialization") version "1.9.22"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.nats:jnats:2.17.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
}