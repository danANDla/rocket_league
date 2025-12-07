plugins {
    kotlin("jvm") version "2.2.21"   // или актуальная
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("io.nats:jnats:2.17.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation(project(":interdomain"))
}

application {
    mainClass.set("TimeAdvancerKt")
}