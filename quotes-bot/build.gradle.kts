plugins { "java" }
repositories { mavenCentral() }
dependencies {
    implementation("org.telegram:telegrambots:6.+")
    implementation("com.google.code.gson:gson:2.+")
    implementation("org.slf4j:slf4j-simple:2.0.11")
}
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
