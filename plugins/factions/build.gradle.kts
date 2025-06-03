plugins {
    id("java")
}

dependencies {
    compileOnly(libs.paper.api)
    implementation(libs.caffeine)
    implementation(libs.postgres)
    implementation(libs.hikari)
}