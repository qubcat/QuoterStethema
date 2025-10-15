plugins { java }
repositories { mavenCentral() }
dependencies {
    implementation("org.telegram:telegrambots:6.+")
    implementation("com.google.code.gson:gson:2.+")
}
java { toolchain { languageVersion.set(JavaLanguageVersion.of(17)) } }
