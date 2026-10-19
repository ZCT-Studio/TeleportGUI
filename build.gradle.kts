import gg.meza.stonecraft.mod

plugins {
    id("gg.meza.stonecraft")
}

base {
    archivesName.set("TeleportGUI-${mod.loader}")
}

repositories {
    mavenCentral()
    maven("https://repo.opencollab.dev/main/")
    maven("https://repo.opencollab.dev/maven-snapshots/")
}

dependencies {
    val archVersion = findProperty("architectury_version") as String? ?: "22.0.5"

    val loader = mod.loader
    if (loader == "fabric") {
        implementation("dev.architectury:architectury-fabric:$archVersion")
    } else if (loader == "neoforge") {
        implementation("dev.architectury:architectury-neoforge:$archVersion")
    }

    implementation("dev.architectury:architectury:$archVersion")

    // SQLite for optional persistent storage backend
    implementation("org.xerial:sqlite-jdbc:3.46.0.0")

    // SnakeYAML for mod settings YAML parsing
    implementation("org.yaml:snakeyaml:2.2")

    // Optional Bedrock support (Geyser/Floodgate/Cumulus) — added manually when present on the server.
    // Disabled here due to OpenCollab TLS issues; code uses Class.forName detection.
}

modSettings {
    var mod_license = findProperty("mod.license") ?: ""
    var mod_authors = findProperty("mod.authors") ?: ""

    var semver_mc_version_range = findProperty("semver_mc_version_range") ?: mod.minecraftVersion
    var maven_mc_version_range = findProperty("maven_mc_version_range") ?: "[${mod.minecraftVersion}]"

    variableReplacements = mapOf(
        "license" to mod_license,
        "authors" to mod_authors,

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
