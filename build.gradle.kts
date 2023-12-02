import groovy.json.JsonBuilder
import groovy.xml.XmlParser
import java.util.HashMap

plugins {
    kotlin("jvm") version "1.9.20"
}

group = "funny.buildapp.bluetooth"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    implementation(gradleApi())
    implementation("org.jetbrains.kotlin:kotlin-stdlib:1.9.20")
    implementation("com.thoughtworks.xstream:xstream:1.4.20")
//    implementation("com.google.code.gson:gson:2.8.8")
//    implementation("org.slf4j:slf4j-api:1.7.32")
//    implementation("org.slf4j:slf4j-simple:1.7.32")
    implementation("commons-beanutils:commons-beanutils:1.9.4")
    implementation("com.google.guava:guava:23.0")
    testImplementation("org.mockito:mockito-all:1.10.19")
    testImplementation("junit:junit:4.8.2")
    testImplementation("org.powermock:powermock-module-junit4:1.7.3")
    testImplementation("org.powermock:powermock-api-mockito:1.7.3")

//    testImplementation(kotlin("test"))
}

//tasks.test {
//    useJUnitPlatform()
//}

kotlin {
    jvmToolchain(8)
}


tasks.register("GeneratorGattJson") {
    group = "Generator"
    description = "GattRegistryGenerator Task"
    doLast {
        generate(
            "${this.project.projectDir}/src/main/resources/gatt/characteristic/descriptor",
            "${this.project.buildDir}/resources/main/gatt/characteristic/descriptor/gatt_spec_registry.json"
        )
        generate(
            "${this.project.projectDir}/src/main/resources/gatt/characteristic/attribute",
            "${this.project.buildDir}/resources/main/gatt/characteristic/attribute/gatt_spec_registry.json"
        )
        generate(
            "${this.project.projectDir}/src/main/resources/gatt/descriptor",
            "${this.project.buildDir}/resources/main/gatt/descriptor/gatt_spec_registry.json"
        )
        generate(
            "${this.project.projectDir}/src/main/resources/gatt/attribute",
            "${this.project.buildDir}/resources/main/gatt/attribute/gatt_spec_registry.json"
        )
        generate(
            "${this.project.projectDir}/src/main/resources/gatt/characteristic",
            "${this.project.buildDir}/resources/main/gatt/characteristic/gatt_spec_registry.json"
        )
        generate(
            "${this.project.projectDir}/src/main/resources/gatt/service",
            "${this.project.buildDir}/resources/main/gatt/service/gatt_spec_registry.json"
        )
    }
}


fun generate(inputFolderName: String, registryFileName: String) {
    val registryFile = File(registryFileName)
    registryFile.parentFile.mkdirs()
    registryFile.createNewFile()
    val directory = File(inputFolderName)
    val parser = XmlParser()
    parser.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
    val registry = hashMapOf<Any?, Any?>()
    directory.listFiles()?.forEach {
        if (it.endsWith(".xml")) {
            val xml = parser.parse(it)
            val type = xml.attributes()["type"]
            if ("${type}.xml" != it.name) {
                throw IllegalStateException("GATT registry generation failed. 'type' attribute ($type) does not match to its file name (${it.name})")
            }
            registry[xml.attributes()["uuid"]] = xml.attributes()["type"]
        }
    }
    registryFile.writeText(JsonBuilder(registry).toPrettyString())
}
