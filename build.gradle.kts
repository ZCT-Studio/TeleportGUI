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
        "完整更新日志请见 https://github.com/$githubRepository/blob/master/changelog.md"
    }

val isPrimaryPublisher = mod.isFabric

publishMods {
    version.set(releaseVersion)
    changelog.set(releaseChangelog)
    displayName.set("${findProperty("mod.name") ?: "TeleportGUI"} ${releaseVersion}")

    providers.environmentVariable("RELEASE_TYPE").orNull?.let { raw ->
        type.set(
            when (raw.uppercase()) {
                "ALPHA" -> ALPHA
                "BETA" -> BETA
                else -> STABLE
            }
        )
    }

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

    if (mod.isFabric) {
        modrinth {
            requires("fabric-api", "architectury-api")
            optional("geyser", "floodgate")
        }
    }
    if (mod.isNeoForge) {
        modrinth {
            requires("architectury-api")
            optional("geyser", "floodgate")
        }
    }
}
