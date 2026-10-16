plugins {
    `java-library`
}

// Plain Java library shared by backend and ujp-sim: UJP request/response contract, no Spring.
dependencies {
    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}
