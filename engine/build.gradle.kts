import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import java.io.BufferedReader
import java.io.InputStreamReader

plugins {
    kotlin("jvm") version "1.3.70"
    id("com.github.johnrengelman.shadow") version "5.2.0"
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
}

group = "com.zenaton.engine"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.pulsar:pulsar-client:2.5.+")
    implementation("org.apache.pulsar:pulsar-functions-api:2.5.+")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.10.+")
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = "1.8"
    }
}

tasks {
    named<ShadowJar>("shadowJar") {
        mergeServiceFiles()
    }
}

tasks {
    build {
        dependsOn(shadowJar)
        finalizedBy(buildFinalizer)
    }
}

val buildFinalizer by tasks.registering {
    doLast {
        updatePulsarFunction("com.zenaton.engine.workflows.functions.State", "workflows")
    }
}

fun updatePulsarFunction(className: String, topic: String) {
    println("Updating $className in $topic")
    val cmd = arrayOf("docker-compose", "exec", "-T", "pulsar", "bin/pulsar-admin", "functions", "update", "--jar", "/zenaton/engine/build/engine-1.0-SNAPSHOT-all.jar", "--classname", className, "--inputs", topic)
    val p = Runtime.getRuntime().exec(cmd)
    val output = getOutput(p)
    val error = getError(p)
    var line: String? = ""
    while (output?.readLine().also { line = it } != null) println(line)
    while (error?.readLine().also { line = it } != null) println(line)
    p.waitFor()
}

fun getOutput(p: Process): BufferedReader? {
    return BufferedReader(InputStreamReader(p.inputStream))
}

fun getError(p: Process): BufferedReader? {
    return BufferedReader(InputStreamReader(p.errorStream))
}