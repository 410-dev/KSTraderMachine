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
    implementation(project(":_sdk:KSSocket"))
    implementation(project(":_sdk:Graphite"))


    implementation(files("../../lib/JsonCoder.jar"))

    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")

    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")
}

tasks.test {
    useJUnitPlatform()
}