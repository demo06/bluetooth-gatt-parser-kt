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
