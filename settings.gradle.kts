pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Official Artifex MuPDF Android artifacts (AGPL build).
        maven { url = uri("https://maven.ghostscript.com") }
        // Tesseract4Android is published via JitPack.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "PDFSeal"
include(":app")
