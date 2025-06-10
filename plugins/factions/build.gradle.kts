plugins {
    id("java")
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.paper.api)

    compileOnly(project(":common"))
    implementation(libs.caffeine)
    implementation(libs.mongodb.driver.sync)
    implementation(libs.bundles.cloudEcosystem)
}

tasks {
    shadowJar {
        archiveClassifier.set("")

        relocate("org.incendo.cloud", "dev.balafini.libs.cloud")
        relocate("com.github.benmanes.caffeine", "dev.balafini.libs.caffeine")
        relocate("com.mongodb", "dev.balafini.libs.mongodb")
    }

    build {
        dependsOn(shadowJar)
    }

    jar {
        archiveClassifier.set("original")
    }
}