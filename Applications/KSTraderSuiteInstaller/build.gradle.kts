plugins {
    id("java")
}

group = "org.example"
version = ""

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation(project(":_sdk:liblks"))
    implementation(project(":_sdk:Foundation"))
    implementation(project(":_sdk:Graphite"))
}

tasks.test {
    useJUnitPlatform()
}