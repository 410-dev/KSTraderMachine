
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.bundling.Jar

plugins {
    id("java")
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "me.hysong"
version = "1.0-SNAPSHOT"

// Ensure that all submodules are evaluated (required for multi-module builds)
evaluationDependsOnChildren()

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.10.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    // Proprietary submodule dependencies (these should point to your subprojects)
    implementation(project(":_sdk:Foundation"))
    implementation(project(":_sdk:liblks"))
    implementation(project(":_sdk:KSTraderAPI"))
    implementation(project(":_sdk:Graphite"))
    implementation(project(":_sdk:KSSocket"))
    implementation(project(":_services:KSNotificationServer"))

    implementation(files("lib/JsonCoder.jar"))


    // External dependencies
    implementation("com.google.code.gson:gson:2.13.0")

    // Lombok, used only during compile time
    compileOnly("org.projectlombok:lombok:1.18.36")
    annotationProcessor("org.projectlombok:lombok:1.18.36")
    testCompileOnly("org.projectlombok:lombok:1.18.36")
    testAnnotationProcessor("org.projectlombok:lombok:1.18.36")

    // Other external components
    implementation("io.github.wuhewuhe:bybit-java-api:1.2.7")
}

tasks.test {
    useJUnitPlatform()
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
    withSourcesJar()
    withJavadocJar()
}

// Standard thin JAR configuration (optional)
tasks.withType<Jar> {
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to "me.hysong.kynesystems.apps.kstradermachine.Application"
        )
    }
}

tasks.withType<ShadowJar> {
    // 1. Sets the Main-Class in the JAR's MANIFEST.MF:
    //    This tells the Java runtime where to start execution when you run `java -jar ...`
    manifest {
        attributes(
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Main-Class" to "me.hysong.kynesystems.apps.kstradermachine.Application" // Crucial for executable JAR
        )
    }
    // 2. Set target directory for the fat JAR:
    archiveFileName.set("appbuild.jar")
    archiveClassifier.set("")
    destinationDirectory.set(project.layout.projectDirectory.asFile)


    // 4. Includes the main project's compiled classes:
    from(sourceSets.main.get().output)


    // 5. Includes compiled classes only from subprojects starting with "_sdk":
    subprojects.forEach { subproj ->
        // Check if the subproject name starts with "_sdk"
        if (subproj.name.startsWith("_")) {
            // Apply the rest of the logic only if the name matches
            subproj.pluginManager.withPlugin("java") {
                val sourceSets = subproj.extensions.findByType(SourceSetContainer::class.java)
                if (sourceSets != null) {
                    // Add the compiled output of the 'main' source set from this _sdk subproject
                    from(sourceSets.getByName("main").output)
                }
            }
        }
    }

    mergeServiceFiles()
}


val copyDriver by tasks.registering(Copy::class) {
    group = "distribution"
    description = "Copies the built appbuild.jar to the shared Storage directory"
    from(layout.projectDirectory.file("appbuild.jar"))
    into(layout.projectDirectory.dir("Storage"))
    // if you want to overwrite
    duplicatesStrategy = DuplicatesStrategy.INCLUDE
}

tasks.named("shadowJar") {
    finalizedBy(copyDriver)
}

tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
