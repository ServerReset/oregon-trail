plugins {
    id("org.jetbrains.kotlin.jvm")
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

tasks.register<JavaExec>("balanceReport") {
    group = "verification"
    description = "Simulate many games and write docs/BALANCE.md"
    classpath = sourceSets["test"].runtimeClasspath
    mainClass.set("com.oregontrail.engine.BalanceReportKt")
    systemProperty("balance.out", rootProject.file("docs/BALANCE.md").absolutePath)
}

