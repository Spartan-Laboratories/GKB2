import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("multiplatform")
    id("org.jetbrains.compose")
}

group = "com.spartanlabs"
version = "1.0-SNAPSHOT"

repositories {
    google()
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
}

kotlin {
    jvm {
        jvmToolchain(11)
        withJava()
    }
    sourceSets {
        val jvmMain by getting {
            dependencies {
                implementation(compose.desktop.currentOs)
                // BASICS
                implementation(kotlin("test"))
                api("ch.qos.logback:logback-classic:1.3.0-alpha13")

                //PRIMARY LIB
                api("net.dv8tion:JDA:5.0.0-beta.2")

                //Webscraping:
                //implementation("it.skrape:skrapeit:0-SNAPSHOT"){isChanging = true}
                implementation("it.skrape:skrapeit:1.1.5")
                implementation("org.jsoup:jsoup:1.15.4")
                implementation("com.mashape.unirest:unirest-java:1.4.9")// https://mvnrepository.com/artifact/com.mashape.unirest/unirest-java

                // Responder class reader
                implementation("org.springframework:spring-context:5.3.23")

                // RSS Feed Reader
                implementation("com.rometools:rome:1.18.0")
            }
        }
        val jvmTest by getting
    }
}

compose.desktop {
    application {
        mainClass = "MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "KotlinBottools"
            packageVersion = "1.0.0"
        }
    }
}
