plugins {
    application
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("com.google.code.gson:gson:2.10.1")
}

sourceSets {
    main {
        java {
            srcDir("src")
        }
    }
}

application {
    mainClass = "nl.bramstout.mcworldexporter.MCWorldExporter"
}
