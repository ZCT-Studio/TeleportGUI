import gg.meza.stonecraft.mod

plugins {
    id("gg.meza.stonecraft")
}

base {
    archivesName.set("TeleportGUI-${mod.loader}")
}

sourceSets {
    main {
        java.srcDir(rootProject.file("src/${mod.loader}/java"))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}

val authors = providers.gradleProperty("mod.authors")
    .orElse("")
    .get()
    .split(",")
    .map { it.trim().removeSurrounding("\"") }
    .filter { it.isNotEmpty() }

fun escapeAuthorsJson(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\b", "\\b")
        .replace("\u000C", "\\f")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace(Regex("[\u0000-\u001F]")) { match ->
            "\\u%04x".format(match.value[0].code)
        }

fun escapeAuthorsToml(value: String): String =
    value
        .replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\b", "\\b")
        .replace("\u000C", "\\f")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace(Regex("[\u0000-\u001F]")) { match ->
            "\\u%04X".format(match.value[0].code)
        }

val authors_json = authors.joinToString(
    separator = ", "
) { "\"${escapeAuthorsJson(it)}\"" }

val authors_toml = authors.joinToString(
    separator = ", "
) { escapeAuthorsToml(it) }


dependencies {
    val archVersion = findProperty("architectury_version") as String? ?: "22.0.5"

    val loader = mod.loader
    if (loader == "fabric") {
        implementation("dev.architectury:architectury-fabric:$archVersion")
    } else if (loader == "neoforge") {
        implementation("dev.architectury:architectury-neoforge:$archVersion")
    }
}

if (mod.isNeoforge) {
    val loom = extensions.getByType<net.fabricmc.loom.api.LoomGradleExtensionAPI>()
    loom.runs.configureEach {
        if (name == "client") {
            mainClass.set("net.neoforged.fml.startup.Client")
        }
    }
}

modSettings {
    var mod_id = findProperty("mod.id") ?: "example"
    var mod_license = findProperty("mod.license") ?: ""

    var architectury_version = findProperty("architectury_version") ?: "22.0.5"
    var semver_mc_version_range = findProperty("semver_mc_version_range") ?: mod.minecraftVersion
    var maven_mc_version_range = findProperty("maven_mc_version_range") ?: "[${mod.minecraftVersion}]"

    variableReplacements = mapOf(
        "id" to mod_id,
        "license" to mod_license,
        "authors_json" to authors_json,
        "authors_toml" to authors_toml,

        "architectury_version" to architectury_version,
        "semver_mc_version_range" to semver_mc_version_range,
        "maven_mc_version_range" to maven_mc_version_range,
    )
}

publishMods {
    modrinth {
        if (mod.isFabric) requires("fabric-api")
    }

    curseforge {
        if (mod.isFabric) requires("fabric-api")
    }
}
