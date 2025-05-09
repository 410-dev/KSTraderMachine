import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.hysong"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    // Project‑local SDKs
    implementation(project(":_sdk:Foundation"))
    implementation(project(":_sdk:KSTraderAPI"))

    // External libraries
    implementation("com.google.code.gson:gson:2.13.0")
    implementation("com.auth0:java-jwt:4.5.0")
    implementation("org.apache.httpcomponents:httpclient:4.5.14")

    // Lombok (compile‑only + annotation processing)
    compileOnly("org.projectlombok:lombok:1.18.38")
    annotationProcessor("org.projectlombok:lombok:1.18.38")
    testCompileOnly("org.projectlombok:lombok:1.18.38")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.38")

    // Test framework
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}

tasks.test {
    useJUnitPlatform()
}

// Configure ShadowJar
tasks.withType<ShadowJar>().configureEach {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version
        )
    }

    archiveBaseName.set("UpBit")
    archiveVersion.set("")
    archiveClassifier.set("")
    destinationDirectory.set(
        project.layout.buildDirectory.dir("../../../builds/drivers")
    )

    from(sourceSets.main.get().output)
    subprojects.forEach { subproj ->
        subproj.pluginManager.withPlugin("java") {
            subproj.extensions
                .findByType(SourceSetContainer::class.java)
                ?.getByName("main")
                ?.let { from(it.output) }
        }
    }

    configurations = listOf(project.configurations.getByName("runtimeClasspath"))

    mergeServiceFiles()
}

// 1) Define a copy task
val copyDriver by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Copies the built UpBit.jar to the shared Storage/drivers directory"
    from(layout.buildDirectory.file("../../../builds/drivers/UpBit.jar"))
    into(layout.projectDirectory.dir("../../Storage/drivers"))
    // if you want to overwrite
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

// 2) Wire it so that once shadowJar finishes, copyDriver runs
tasks.named("shadowJar") {
    finalizedBy(copyDriver)
}

// 3) Ensure build still depends on shadowJar
tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}