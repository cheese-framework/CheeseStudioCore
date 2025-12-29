plugins {
    kotlin("jvm") version "2.0.20"
    id("com.gradleup.shadow") version "8.3.5"
    id("org.graalvm.buildtools.native") version "0.10.2"
}

group = "net.codeocean.cheese"
version = "1.0-SNAPSHOT"
tasks.jar {
    manifest {
        attributes["Main-Class"] = "net.codeocean.cheese.MainKt"
    }
}

repositories {
    mavenCentral()
    maven { setUrl("https://jitpack.io")   }
}

graalvmNative {

    binaries {
        named("main") {
            imageName.set("Hello-word")
            mainClass.set("net.codeocean.cheese.MainKt")
//            buildArgs.add("-O4")
            javaLauncher.set(javaToolchains.launcherFor {
                languageVersion.set(JavaLanguageVersion.of(21))
                vendor.set(JvmVendorSpec.matching("GraalVM"))

            })
        }
        named("test") {
            buildArgs.add("-O0")
        }
    }
    binaries.all {
        buildArgs.add("--verbose")
    }
}

dependencies {
    implementation("com.github.iyxan23:zipalign-java:1.2.1")
    implementation ("com.formdev:flatlaf:3.5.2") // 使用最新版本
    implementation ("com.fifesoft:rsyntaxtextarea:3.5.1") // 使用最新版本
    // https://mvnrepository.com/artifact/org.json/json
    implementation("org.json:json:20240303")

    implementation("org.apache.logging.log4j:log4j-core:2.24.1")
    implementation("org.apache.logging.log4j:log4j-api:2.24.1")
    // 如果你需要使用 Log4j 2 的 SLF4J 适配器，可以添加以下依赖：
    implementation("org.apache.logging.log4j:log4j-slf4j-impl:2.24.1")
    implementation(fileTree(file("libs")) {
        include("*.jar")
        include("*.aar")
    })
    implementation("com.google.code.gson:gson:2.10.1")
    implementation("org.yaml:snakeyaml:2.2")
    implementation("io.ktor:ktor-server-websockets:2.3.12")
    implementation("io.ktor:ktor-server-status-pages-jvm:2.3.12")
    implementation("io.ktor:ktor-server-netty:2.3.12")
    implementation("io.ktor:ktor-server-core:2.3.12")
    implementation("org.tomlj:tomlj:1.1.1")
    implementation ("com.fasterxml.jackson.module:jackson-module-kotlin:2.18.+")
    implementation("args4j:args4j:2.37")
    implementation("org.freemarker:freemarker:2.3.33")
    testImplementation(kotlin("test"))
    // https://mvnrepository.com/artifact/org.eclipse.jgit/org.eclipse.jgit
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.0.0.202409031743-r")

}

tasks.test {
    useJUnitPlatform()
}
kotlin {
    jvmToolchain(18)
}

tasks {
    shadowJar {
        // 设置自定义文件名
        archiveFileName.set("core.jar")

        // 设置输出目录
        destinationDirectory.set(file("build/libs"))

        // 包含所有运行时依赖
        configurations = listOf(project.configurations.runtimeClasspath.get())

        // 合并服务文件，解决 META-INF 冲突问题
        mergeServiceFiles()
    }
}
