// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    kotlin("jvm") version "1.9.10" apply false
    id("com.android.application") version "8.1.1" apply false
}

allprojects {
    repositories {
        // Supprime ces lignes :
        // google()
        // mavenCentral()
        // maven { url = uri("https://jitpack.io") }
    }
}