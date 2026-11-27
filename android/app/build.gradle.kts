import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.zip.ZipFile

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// SDL source location (SDL3 by default, or SDL2 if configured)
val sdlPath: String = project.findProperty("sdlPath") as? String
    ?: System.getenv("SDL3_PATH")
    ?: rootProject.file("SDL3").absolutePath

// Host-tools build directory (native CMake build with OPTION_TOOLS_ONLY=ON)
val hostBinaryDir: String = project.findProperty("hostBinaryDir") as? String
    ?: System.getenv("HOST_BINARY_DIR")
    ?: rootProject.file("../build-host").absolutePath

android {
    namespace = "org.openttd.openttd"
    compileSdk = 35

    defaultConfig {
        applicationId = "org.openttd.openttd"
        minSdk = 24
        targetSdk = 35
        versionCode = 170000
        versionName = "17.0"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                arguments += listOf(
                    "-DANDROID=1",
                    "-DCMAKE_BUILD_TYPE=Release",
                    "-DOPTION_DEDICATED=OFF",
                    "-DHOST_BINARY_DIR=$hostBinaryDir",
                    "-DSDL3_PATH=$sdlPath"
                )
                targets += listOf("openttd", "SDL3-shared")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isJniDebuggable = true
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    externalNativeBuild {
        cmake {
            path = file("../../CMakeLists.txt")
            version = "3.22.1"
        }
    }

    sourceSets {
        getByName("main") {
            // Include SDL Java / Kotlin sources if available in checkout
            val sdlJavaDirs = listOf(
                file("$sdlPath/android-project/app/src/main/java"),
                file("$sdlPath/android-project/app/src/main/kotlin")
            )
            for (dir in sdlJavaDirs) {
                if (dir.exists()) {
                    java.srcDirs(dir)
                }
            }
            assets.srcDirs("src/main/assets")
        }
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.savedstate:savedstate-ktx:1.2.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.10.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
}

// Task: Download OpenGFX2 Classic release archive if not already available
val downloadOpenGFX by tasks.registering {
    description = "Downloads official OpenGFX2 Classic base graphics if not already present"
    group = "assets"

    val basesetDir = file("src/main/assets/baseset")
    val targetTar = file("src/main/assets/baseset/OpenGFX2_Classic-0.8.1.tar")

    outputs.dir(basesetDir)

    doLast {
        basesetDir.mkdirs()
        // Remove older OpenGFX 0.6.0 tar if present to keep APK lean and avoid stale baseset selection
        file("src/main/assets/baseset/opengfx-0.6.0.tar").takeIf { it.exists() }?.delete()

        if (!targetTar.exists() || targetTar.length() == 0L) {
            val opengfx2Url = URI("https://github.com/OpenTTD/OpenGFX2/releases/download/0.8.1/OpenGFX2_Classic-0.8.1.tar").toURL()
            logger.lifecycle("Downloading OpenGFX2 Classic base graphics from $opengfx2Url ...")
            opengfx2Url.openStream().use { input ->
                Files.copy(input, targetTar.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }
            logger.lifecycle("OpenGFX2 Classic successfully installed in assets/baseset/.")
        }
    }
}

// Task: Download OpenSFX release archive if not already available
val downloadOpenSFX by tasks.registering {
    description = "Downloads official OpenSFX base sounds if not already present"
    group = "assets"

    val basesetDir = file("src/main/assets/baseset")
    val opensfxArchive = file("${layout.buildDirectory.get().asFile}/download/opensfx-1.0.3-all.zip")

    outputs.dir(basesetDir)

    doLast {
        val opensfxInstalled = basesetDir.listFiles()?.any {
            it.name.contains("opensfx", ignoreCase = true)
        } ?: false

        if (!opensfxInstalled) {
            opensfxArchive.parentFile.mkdirs()
            val opensfxUrl = URI("https://cdn.openttd.org/opensfx-releases/1.0.3/opensfx-1.0.3-all.zip").toURL()
            logger.lifecycle("Downloading OpenSFX base sounds from $opensfxUrl ...")
            opensfxUrl.openStream().use { input ->
                Files.copy(input, opensfxArchive.toPath(), StandardCopyOption.REPLACE_EXISTING)
            }

            logger.lifecycle("Extracting OpenSFX into assets/baseset/ ...")
            basesetDir.mkdirs()
            ZipFile(opensfxArchive).use { zip ->
                val entries = zip.entries()
                while (entries.hasMoreElements()) {
                    val entry = entries.nextElement()
                    if (!entry.isDirectory) {
                        val outFile = File(basesetDir, File(entry.name).name)
                        zip.getInputStream(entry).use { inStream ->
                            Files.copy(inStream, outFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                        }
                    }
                }
            }
            logger.lifecycle("OpenSFX base sounds successfully installed in assets/baseset/.")
        }
    }
}

// Task: Copy built-in media/baseset and OpenGFX into app/src/main/assets/baseset
val copyOpenGFXBaseGraphics by tasks.registering(Copy::class) {
    description = "Copies OpenGFX and OpenTTD base graphics files into app assets"
    group = "assets"
    dependsOn(downloadOpenGFX, downloadOpenSFX)

    // Source binary data/fonts from repo's media/baseset (compiled .obg/.obm/.obs come from CMake)
    from(rootProject.file("../media/baseset")) {
        include("*.grf", "*.dat", "*.ttf")
    }

    // Source from any local opengfx folder in project root if present
    val localOpenGFX = rootProject.file("../opengfx")
    if (localOpenGFX.exists()) {
        from(localOpenGFX) {
            include("**/*.obg", "**/*.grf", "**/*.tar")
        }
    }

    into(layout.projectDirectory.dir("src/main/assets/baseset"))
}

tasks.named("preBuild") {
    dependsOn(copyOpenGFXBaseGraphics)
}

// Task: Copy compiled .lng language packs and compiled baseset metadata (.obg/.obm/.obs) from .cxx
val copyCompiledBaseset by tasks.registering(Copy::class) {
    description = "Copies compiled OpenTTD baseset files from .cxx into app assets"
    group = "assets"
    includeEmptyDirs = false

    from(file(".cxx")) {
        include("**/baseset/*")
        eachFile {
            relativePath = RelativePath(true, name)
        }
    }
    into(layout.projectDirectory.dir("src/main/assets/baseset"))
}

val copyLanguagePacks by tasks.registering(Copy::class) {
    description = "Copies compiled OpenTTD .lng language packs into app assets"
    group = "assets"
    includeEmptyDirs = false

    from(file(".cxx")) {
        include("**/lang/*.lng")
        eachFile {
            relativePath = RelativePath(true, name)
        }
    }
    into(layout.projectDirectory.dir("src/main/assets/lang"))
}

tasks.configureEach {
    if (name.startsWith("merge") && name.endsWith("Assets")) {
        dependsOn(copyLanguagePacks, copyCompiledBaseset)
    }
}

copyCompiledBaseset.configure {
    dependsOn(tasks.matching { it.name.startsWith("buildCMake") })
}

copyLanguagePacks.configure {
    dependsOn(tasks.matching { it.name.startsWith("buildCMake") })
}

