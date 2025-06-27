plugins {
    id("java")
    alias(libs.plugins.shadow)
}

dependencies {
    compileOnly(libs.paper.api)
    compileOnly(libs.lombok)

    implementation(libs.caffeine)
    implementation(libs.fastboard)
    implementation(libs.bundles.mongoEcosystem)

    annotationProcessor(libs.lombok)
}

tasks {
    shadowJar {
        archiveClassifier.set("")

        relocate("fr.mrmicky.fastboard", "dev.balafini.libs.fastboard")
        relocate("com.fasterxml.jackson", "dev.balafini.libs.jackson")
        relocate("org.mongojack", "dev.balafini.libs.mongojack")
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