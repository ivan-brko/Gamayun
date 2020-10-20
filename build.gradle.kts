import com.google.protobuf.gradle.*

buildscript{
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath("com.google.protobuf:protobuf-gradle-plugin:0.8.11")
    }
}

repositories {
    mavenCentral()
    jcenter()
    maven("https://dl.bintray.com/arrow-kt/arrow-kt/")
}

plugins {
    kotlin("jvm") version "1.4.0"
    idea
    id("com.google.protobuf") version "0.8.8"
    kotlin("plugin.serialization") version "1.4.10"
    application
    kotlin("kapt") version "1.4.10"
    id("com.github.johnrengelman.shadow") version "6.0.0" //for fat-jar creation
}

application {
    mainClassName = "io.ktor.server.netty.EngineMain"
}

group = "org.gamayun"
version = "0.2"

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("io.ktor:ktor-server-netty:1.4.1")
//    implementation("ch.qos.logback:logback-classic:1.2.1")
    implementation("io.ktor:ktor-locations:1.4.1")
    implementation("io.ktor:ktor-auth:1.4.1")
    implementation("io.ktor:ktor-auth-jwt:1.4.1")
    implementation("io.ktor:ktor-serialization:1.4.1")
    implementation("com.moandjiezana.toml:toml4j:0.7.2")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.3.7")
    implementation("org.quartz-scheduler:quartz:2.3.2")
    implementation("io.github.microutils:kotlin-logging:1.8.3")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.13.3")
    implementation("org.litote.kmongo:kmongo-coroutine-serialization:4.0.3")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:0.20.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-runtime:1.0-M1-1.4.0-rc")
    implementation("org.kodein.di:kodein-di:7.0.0")
    implementation("org.simplejavamail:simple-java-mail:6.4.3")

    implementation("io.arrow-kt:arrow-core:0.10.5")
    implementation("io.arrow-kt:arrow-syntax:0.10.5")
    kapt ("io.arrow-kt:arrow-meta:0.10.5")

    implementation("com.google.protobuf:protobuf-java:3.6.1")
    implementation("com.google.protobuf:protobuf-java-util:3.6.1")
    implementation("io.grpc:grpc-kotlin-stub:0.1.4")
    implementation("io.grpc:grpc-stub:1.15.1")
    implementation("io.grpc:grpc-protobuf:1.15.1")
    implementation("io.grpc:grpc-netty:1.30.2")
    if (JavaVersion.current().isJava9Compatible) {
        // Workaround for @javax.annotation.Generated
        // see: https://github.com/grpc/grpc-java/issues/3633
        implementation("javax.annotation:javax.annotation-api:1.3.1")
    }
}

protobuf {
    protoc {
        // The artifact spec for the Protobuf Compiler
        artifact = "com.google.protobuf:protoc:3.6.1"
    }
    plugins {
        // Optional: an artifact spec for a protoc plugin, with "grpc" as
        // the identifier, which can be referred to in the "plugins"
        // container of the "generateProtoTasks" closure.
        id("grpc") {
            artifact = "io.grpc:protoc-gen-grpc-java:1.15.1"
        }

        id("grpckt"){
            artifact = "io.grpc:protoc-gen-grpc-kotlin:0.1.4"
        }
    }
    generateProtoTasks {
        ofSourceSet("main").forEach {
            it.plugins {
                // Apply the "grpc" plugin whose spec is defined above, without options.
                id("grpc")
                id("grpckt")
            }
        }
    }
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}