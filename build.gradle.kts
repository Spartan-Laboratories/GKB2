import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.jetbrains.kotlin.backend.wasm.lower.excludeDeclarationsFromCodegen

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
    maven("https://repo.spring.io/snapshot")
    maven("C:/Users/spartak/Documents/Programming/libraries")
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
                //PRIMARY LIB
                implementation("com.spartanlabs:WebTools:LATEST"){
                    exclude(group="com.spartanlabs",module="GeneralTools")
                }
                implementation("com.spartanlabs:GeneralTools"){
                    version{
                        strictly("LATEST")
                    }
                }
                implementation("net.dv8tion:JDA:5.0.0-beta.2")

                //Webscraping:
                implementation("org.jsoup:jsoup:1.15.4")
                implementation("com.mashape.unirest:unirest-java:1.4.9")// https://mvnrepository.com/artifact/com.mashape.unirest/unirest-java

                /*  Spring framework is needed for spring-based dependency injection of commands
                    Responder class reader is also spring based
                 */
                implementation("org.springframework:spring-context:5.3.29")

                // RSS Feed Reader
                implementation("com.rometools:rome:1.18.0")

                implementation(kotlin("reflect"))
            }
        }
        val jvmTest by getting{
            dependencies{
                implementation(kotlin("test"))
            }
        }
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
