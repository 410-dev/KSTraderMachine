import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    id("java")
}

group = "me.hysong"
version = "1.0-SNAPSHOT"

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
            "Main-Class" to "me.hysong.kynesystems.apps.ksmanualtrader.Application"
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
            "Main-Class" to "me.hysong.kynesystems.apps.ksmanualtrader.Application" // Crucial for executable JAR
        )
    }
    // 2. Set target directory for the fat JAR:
    archiveFileName.set("KSManualTrader.jar")
    archiveClassifier.set("")
    destinationDirectory.set(project.layout.projectDirectory.asFile)


    // 4. Includes the main project's compiled classes:
    from(sourceSets.main.get().output)


    // 5. Includes compiled classes only from subprojects starting with "_sdk":
    subprojects.forEach { subproj ->
        // Check if the subproject name starts with "_sdk"
        if (subproj.name.startsWith("_sdk")) {
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


tasks.named("build") {
    dependsOn(tasks.named("shadowJar"))
}
