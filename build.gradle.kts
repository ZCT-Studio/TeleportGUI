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

fun inferReleaseType(version: String): String {
    val v = version.lowercase()
    return when {
        "snapshot" in v || "alpha" in v -> "ALPHA"
        "beta" in v -> "BETA"
        else -> "STABLE"
    }
}

val publishType: String = providers.environmentVariable("PUBLISH_TYPE")
    .orElse(providers.provider { inferReleaseType(mod.version) })
    .get()

val releaseTag: String = providers.environmentVariable("PUBLISH_TAG")
    .orElse(providers.provider { "v${mod.version}" })
    .get()

val releaseVersion: String = providers.environmentVariable("PUBLISH_VERSION")
    .orElse(providers.provider { mod.version })
    .get()

val githubRepository: String = providers.environmentVariable("GITHUB_REPOSITORY")
    .orElse("ZCT-Studio/TeleportGUI")
    .get()

val releaseChangelog: String = providers.environmentVariable("PUBLISH_CHANGELOG_FILE")
    .map { path ->
        val f = file(path)
        if (path.isBlank() || !f.isFile) "" else f.readText()
    }
    .orElse("")
    .get()
    .ifBlank {
        "完整更新日志请见 https://github.com/$githubRepository/blob/master/CHANGELOG.MD"
    }

val isPrimaryPublisher = mod.isFabric

publishMods {
    version.set(releaseVersion)
    type.set(
        when (publishType.uppercase()) {
            "ALPHA" -> ALPHA
            "BETA" -> BETA
            else -> STABLE
        }
    )
    changelog.set(releaseChangelog)
    displayName.set("${findProperty("mod.name") ?: "TeleportGUI"} ${releaseVersion}")

    if (isPrimaryPublisher) {
        github {
            repository.set(githubRepository)
            commitish.set(providers.environmentVariable("GITHUB_REF_NAME").orElse("master"))
            tagName.set(releaseTag)
            accessToken.set(providers.environmentVariable("GITHUB_TOKEN")
                .orElse(providers.environmentVariable("GH_TOKEN")))

            val siblingJar = rootProject.layout.projectDirectory
                .dir("versions/26.3-neoforge/build/libs")
                .file("TeleportGUI-neoforge-${releaseVersion}+mc${mod.minecraftVersion}.jar")
            additionalFiles.from(siblingJar)

            tasks.named("publishGithub") {
                dependsOn(":26.3-neoforge:build")
            }
        }
    }

    modrinth {
        if (mod.isFabric) requires("fabric-api")
        projectId.set(
            (findProperty("mod.modrinth.project") ?: findProperty("mod.id") ?: "teleportgui").toString()
        )
        accessToken.set(providers.environmentVariable("MODRINTH_TOKEN"))
    }

    // CurseForge
    // require("fabric-api") 之类的依赖声明放在对应位置即可。
    // curseforge {
    //     if (mod.isFabric) requires("fabric-api")
    //     accessToken.set(providers.environmentVariable("CURSEFORGE_TOKEN"))
    // }
}
