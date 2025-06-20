import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.kotlin.dsl.withType

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.hysong"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()

}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    implementation(project(":_sdk:Foundation"))
    implementation(project(":_sdk:liblks"))

    // Lombok, used only during compile time
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    testCompileOnly("org.projectlombok:lombok:1.18.36")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")
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

    archiveBaseName.set("KSSuite")
    archiveVersion.set("")
    archiveClassifier.set("")
    destinationDirectory.set(
        project.layout.buildDirectory.dir("../../../builds/libraries")
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
    description = "Copies the built KSSuite.jar to the shared Storage/libraries directory"
    from(layout.buildDirectory.file("../../../builds/libraries/KSSuite.jar"))
    into(layout.projectDirectory.dir("../../Storage/defaults_shared/libraries"))
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