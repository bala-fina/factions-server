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