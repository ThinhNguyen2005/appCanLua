@file:Suppress("UnstableApiUsage")
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
    plugins {
        // Bạn nên khai báo phiên bản Android Gradle Plugin (AGP) ở đây nếu chưa có
        // Ví dụ: id("com.android.application") version "8.2.0" apply false

        id("com.google.gms.google-services") version "4.4.4" apply false
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "CanLua"
include(":app")