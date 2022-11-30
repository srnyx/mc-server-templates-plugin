description = "PluginDownloader"
version = "1.1.2"
group = "xyz.srnyx"

plugins {
    java
}

repositories {
    maven("https://hub.spigotmc.org/nexus/content/repositories/snapshots/") // org.spigotmc:spigot-api
    maven("https://oss.sonatype.org/content/repositories/snapshots/") // org.spigotmc:spigot-api
    mavenCentral() // org.spigotmc:spigot-api
}

dependencies {
    compileOnly("org.spigotmc", "spigot-api", "1.16.5-R0.1-SNAPSHOT")
    compileOnly("org.jetbrains", "annotations", "23.0.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
}

tasks {
    compileJava {
        options.encoding = "UTF-8"
    }

    processResources {
        inputs.property("version", project.version)
        filesMatching("**/plugin.yml") {
            expand("version" to project.version)
        }
    }
}