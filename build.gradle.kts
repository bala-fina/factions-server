plugins {
    id("java")
}

allprojects {
    group = "dev.balafini"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }
}

subprojects {
    plugins.withType<JavaPlugin> {
        tasks.withType<JavaCompile>().configureEach {
            options.encoding = "UTF-8"
        }
        java {
            toolchain.languageVersion.set(JavaLanguageVersion.of(21))
        }
    }
}