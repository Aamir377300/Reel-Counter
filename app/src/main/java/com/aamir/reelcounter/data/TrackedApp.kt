package com.aamir.reelcounter.data

enum class TrackedApp {
    INSTAGRAM,
    YOUTUBE;

    companion object {
        fun fromPackage(packageName: String): TrackedApp? = when (packageName) {
            "com.instagram.android" -> INSTAGRAM
            "com.google.android.youtube" -> YOUTUBE
            else -> null
        }
    }
}
