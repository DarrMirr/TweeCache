plugins {
    java
    id("com.github.johnrengelman.shadow") version "7.0.0"
    id("me.champeau.jmh") version "0.6.6"
    `maven-publish`
}

group = "com.github.darrmirr"
version = project.property("project.version")!!

repositories {
    // Apache Calcite dependency with modifications for library needs
    maven { url = uri(projectDir.toPath().resolve("repository/local")) }
    mavenLocal()
    mavenCentral()
}

dependencies {
    // https://mvnrepository.com/artifact/com.github.ben-manes.caffeine/caffeine
    implementation("com.github.ben-manes.caffeine:caffeine:2.8.8")
    // https://mvnrepository.com/artifact/org.jdbi/jdbi3-core
    implementation("org.jdbi:jdbi3-core:3.18.1") {
        // exclude com.github.ben-manes.caffeine:caffeine:2.8.0 due to add manually last patched 2.8.8 version
        exclude(group="com.github.ben-manes", module="caffeine")
    }
    // https://mvnrepository.com/artifact/org.codehaus.janino/janino
    implementation("org.codehaus.janino:janino:3.1.6")
    // https://mvnrepository.com/artifact/org.apache.calcite/calcite-core
    implementation("org.apache.calcite:calcite-core:1.28.0-snapshot")
    // https://mvnrepository.com/artifact/com.zaxxer/HikariCP
    implementation("com.zaxxer:HikariCP:4.0.3")
    // https://mvnrepository.com/artifact/org.slf4j/slf4j-api
    implementation("org.slf4j:slf4j-api:1.7.32")


    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    // https://mvnrepository.com/artifact/org.hamcrest/hamcrest-all
    testImplementation("org.hamcrest:hamcrest-all:1.3")
    // https://mvnrepository.com/artifact/org.openjdk.jmh/jmh-generator-annprocess
    testImplementation("org.openjdk.jmh:jmh-generator-annprocess:1.33")
}

tasks.getByName<Test>("test") {
    useJUnitPlatform()
}

tasks.jar {
    manifest {
        attributes(mapOf("Implementation-Title" to project.name,
                         "Implementation-Version" to project.version,
                         "Implementation-URL" to project.property("project.url"),
                         "Implementation-Vendor" to project.property("project.developers")))
    }
    finalizedBy ( tasks.shadowJar )
}

// task to build fat jar (jar with dependencies)
tasks {
    shadowJar {
        manifest {
            attributes(mapOf("Implementation-Title" to project.name,
                             "Implementation-Version" to project.version,
                             "Implementation-URL" to project.property("project.url"),
                             "Implementation-Vendor" to project.property("project.developers")))
        }
    }
}

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.property("group").toString()
            artifactId = project.name
            version = project.property("project.version").toString()

            from(components["java"])
        }
    }
}