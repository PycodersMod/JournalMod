import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.nio.charset.StandardCharsets
import java.util.Base64

plugins {
    id("eclipse")
    id("idea")
    id("net.minecraftforge.gradle") version("[6.0.24,6.2)")
}

val mod_id: String by extra
val mod_name: String by extra
val mod_version: String by extra
val mod_group_id: String by extra
val mod_authors: String by extra
val mod_description: String by extra
val minecraft_version: String by extra
val minecraft_version_range: String by extra
val forge_version: String by extra
val forge_version_range: String by extra
val loader_version_range: String by extra

val pycodersRunDir = file(providers.gradleProperty("pycodersRuntimeDir").orElse("../../runtime/legacy-import/JournalMod/run").get())
fun decodeArgs(name: String): List<String> = providers.gradleProperty(name).orNull?.takeIf { it.isNotEmpty() }?.split('.')?.map { if (it == "_") "" else String(Base64.getDecoder().decode(it), StandardCharsets.UTF_8) } ?: emptyList()
val pycodersGameArgs = decodeArgs("pycodersGameArgsB64")
val pycodersJavaArgs = decodeArgs("pycodersJavaArgsB64")
val pycodersUsername = providers.gradleProperty("pycodersUsername").orElse("Dev").get()

base {
    archivesName.set(mod_id)
    group = mod_group_id
    version = mod_version
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

minecraft {
    mappings("official", minecraft_version)
    
    copyIdeResources.set(true)
    
    runs {
        create("client") {
            workingDirectory(pycodersRunDir)
            args("--username", pycodersUsername)
            pycodersGameArgs.forEach { args(it) }
            pycodersJavaArgs.forEach { jvmArg(it) }
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("forge.enabledGameTestNamespaces", mod_id)
            
            mods {
                create(mod_id) {
                    source(sourceSets["main"])
                }
            }
        }
        
        create("server") {
            workingDirectory(pycodersRunDir)
            pycodersGameArgs.forEach { args(it) }
            pycodersJavaArgs.forEach { jvmArg(it) }
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            property("forge.enabledGameTestNamespaces", mod_id)
            
            mods {
                create(mod_id) {
                    source(sourceSets["main"])
                }
            }
        }
        
        create("data") {
            workingDirectory(pycodersRunDir)
            property("forge.logging.markers", "REGISTRIES")
            property("forge.logging.console.level", "debug")
            
            args("--mod", mod_id)
            args("--all")
            args("--output", file("src/generated/resources/").absolutePath)
            args("--existing", file("src/main/resources/").absolutePath)
            
            mods {
                create(mod_id) {
                    source(sourceSets["main"])
                }
            }
        }
    }
}

sourceSets["main"].resources {
    srcDir("src/generated/resources")
}

repositories {
    mavenCentral()
    maven { url = uri("https://maven.minecraftforge.net/") }
    maven { url = uri("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/") }
    maven { url = uri("https://maven.aliyun.com/repository/public/") }
}

dependencies {
    minecraft("net.minecraftforge:forge:$minecraft_version-$forge_version")
}

tasks.withType<ProcessResources> {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraft_version,
        "minecraft_version_range" to minecraft_version_range,
        "forge_version" to forge_version,
        "forge_version_range" to forge_version_range,
        "loader_version_range" to loader_version_range,
        "mod_id" to mod_id,
        "mod_name" to mod_name,
        "mod_license" to "MIT",
        "mod_version" to mod_version,
        "mod_authors" to mod_authors,
        "mod_description" to mod_description
    )
    
    inputs.properties(replaceProperties)
    
    filesMatching(listOf("META-INF/mods.toml", "pack.mcmeta")) {
        expand(replaceProperties)
    }
}

tasks.withType<Jar> {
    manifest {
        attributes(mapOf(
            "Specification-Title" to mod_id,
            "Specification-Vendor" to mod_authors,
            "Specification-Version" to "1",
            "Implementation-Title" to project.name,
            "Implementation-Version" to project.version,
            "Implementation-Vendor" to mod_authors,
            "Implementation-Timestamp" to LocalDateTime.now().toString()
        ))
    }
    
    finalizedBy("reobfJar")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
}

tasks.withType<Copy> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}
