plugins {
    id 'org.jetbrains.kotlin.jvm' version '1.6.20'
    id 'application'
}

group = 'org.hierarchio'
version = '1.0-SNAPSHOT'

repositories {
    mavenCentral()
}

dependencies {
    testImplementation 'org.jetbrains.kotlin:kotlin-test'
}

test {
    useJUnitPlatform()
}

compileKotlin {
    kotlinOptions.jvmTarget = '1.8'
}

compileTestKotlin {
    kotlinOptions.jvmTarget = '1.8'
}

application {
    mainClassName = 'Server'
}

tasks.withType<Jar>() {
    manifest {
        attributes["Main-Class"] = "org.hierarchio.Server"
    }
    configurations["compileClasspath"].forEach { file: File ->
        from(zipTree(file.absoluteFile))
    }
}