plugins {
    kotlin("jvm") version "2.2.21"   // или актуальная
    kotlin("plugin.serialization") version "1.9.22"
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib"))
    implementation("org.knowm.xchart:xchart:3.8.5")
    implementation("io.nats:jnats:2.17.6")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation(project(":interdomain"))
}

application {
    mainClass.set("CtVmKt")
}
