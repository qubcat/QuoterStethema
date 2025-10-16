plugins {
    id("java")
    id("application")
}

repositories { mavenCentral() }

dependencies {
    implementation("org.telegram:telegrambots:6.9.7.1")
    implementation("com.google.code.gson:gson:2.11.0")
    implementation("io.github.cdimascio:dotenv-java:3.0.0")
    implementation("org.slf4j:slf4j-simple:2.0.11")
    implementation("org.xerial:sqlite-jdbc:3.46.0.0") // ← SQLite
}

application {
    mainClass.set("App") // если пакет не указан; иначе "com.example.App"
}

java {
    toolchain { languageVersion.set(JavaLanguageVersion.of(23)) }
}
