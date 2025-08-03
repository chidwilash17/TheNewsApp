// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral() // Good practice to have this too
    }
    dependencies {
        val navVersion = "2.7.5" // This is correct for Safe Args classpath
        classpath("androidx.navigation:navigation-safe-args-gradle-plugin:$navVersion")
    }
}

plugins {
    id("com.android.application") version "8.1.1" apply false // Defines version for app module
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false // Defines version for app module
    id("com.google.devtools.ksp") version "1.9.0-1.0.13" apply false // Defines version for app module
}

