repositories {
    maven("https://packages.confluent.io/maven/")
    mavenCentral()
}

plugins {
    kotlin("jvm")
    id("internal-publishing")
}

dependencies {
    implementation(deps.okio.jvm)
    implementation(deps.protobuf.java)
    implementation(deps.schemaRegistryClient)
    implementation(deps.googleApisCommonProtos)
    implementation(deps.kafkaClients)
    implementation(project(":wire-descriptor"))
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-core-jvm:1.2.2")
    implementation("io.confluent:kafka-protobuf-types:6.2.0")
    implementation("io.confluent:kafka-protobuf-provider:6.2.0")
    implementation("io.confluent:kafka-protobuf-serializer:6.2.0")

    testImplementation(deps.assertj)
    testImplementation(deps.junit)
    testImplementation(deps.protobuf.javaUtil)
    testImplementation(project(":wire-test-utils"))
}

val test by tasks.getting(Test::class) {
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
    }
}

val jar by tasks.getting(Jar::class) {
    manifest {
        attributes("Automatic-Module-Name" to "wire-kafka")
    }
}
