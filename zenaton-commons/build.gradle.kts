plugins {
    application
    kotlin("jvm")
    id("org.jlleitschuh.gradle.ktlint") version "9.2.1"
    id("com.commercehub.gradle.plugin.avro") version "0.19.1"
}

application {
    mainClassName = "com.zenaton.MainKt"
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation("org.apache.avro:avro:1.9.+")
    implementation("com.fasterxml.jackson.core:jackson-databind:2.11.+")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin:2.11.+")

    testImplementation("org.jeasy:easy-random-core:4.2.+")
    testImplementation("io.kotest:kotest-runner-junit5-jvm:4.0.+")
    testImplementation("io.kotest:kotest-property-jvm:4.0.+")
    testImplementation("io.kotest:kotest-core-jvm:4.0.+")
    testImplementation("io.mockk:mockk:1.9.+")
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks {
    compileKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
    compileTestKotlin {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_1_8.toString()
    }
}

tasks {
    build {
        dependsOn("ktlintFormat")
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

avro {
    isCreateSetters.set(false)
    isCreateOptionalGetters.set(false)
    isGettersReturnOptional.set(false)
    fieldVisibility.set("PRIVATE")
    outputCharacterEncoding.set("UTF-8")
    stringType.set("String")
    templateDirectory.set(null as String?)
    isEnableDecimalLogicalType.set(true)
    dateTimeLogicalType.set("JSR310")
}
