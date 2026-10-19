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

    implementation("org.xerial:sqlite-jdbc:3.46.0.0")

    implementation("org.yaml:snakeyaml:2.2")

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
