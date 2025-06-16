plugins {
    id("java")
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.paper.api)

    implementation(libs.caffeine)
    implementation(libs.bundles.mongoEcosystem)
    implementation(libs.bundles.cloudEcosystem)
}

tasks {
    shadowJar {
        archiveClassifier.set("")

        // --- THE FIX IS HERE ---
        // Relocate Jackson to prevent conflicts with the server's built-in version.
        // This is the direct solution to the NoSuchFieldError.
        relocate("com.fasterxml.jackson", "dev.balafini.libs.jackson")

        // It's also best practice to relocate MongoJack itself.
        relocate("org.mongojack", "dev.balafini.libs.mongojack")

        // Your existing relocations are also correct.
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