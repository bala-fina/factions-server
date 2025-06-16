plugins {
    id("java")
    alias(libs.plugins.shadow) apply false
}

allprojects {
    group = "dev.balafini"
    version = "1.0-SNAPSHOT"

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
        maven(url = "https://oss.sonatype.org/content/repositories/snapshots")
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