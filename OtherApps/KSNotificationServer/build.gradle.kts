plugins {
    id("java")
}

version = "1.0-SNAPSHOT"
group = "me.hysong"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation(project(":_sdk:Foundation"))
    implementation(project(":_sdk:Graphite"))
}

tasks.test {
    useJUnitPlatform()
}