plugins {
    id("java")
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.paper.api)

    implementation(libs.caffeine)
    implementation(libs.fastboard)
    implementation(libs.bundles.mongoEcosystem)
    implementation(libs.bundles.cloudEcosystem)
}

tasks {
    shadowJar {
        archiveFileName.set("Factions.jar")

        relocate("fr.mrmicky.fastboard", "dev.balafini.libs.fastboard")
        relocate("com.fasterxml.jackson", "dev.balafini.libs.jackson")
        relocate("org.mongojack", "dev.balafini.libs.mongojack")
        relocate("org.incendo.cloud", "dev.balafini.libs.cloud")
        relocate("com.github.benmanes.caffeine", "dev.balafini.libs.caffeine")
        relocate("com.mongodb", "dev.balafini.libs.mongodb")

        minimize()
    }

    build {
        dependsOn(shadowJar)
    }
}