package com.slothiesmooth.nyx.designlibrary.tokens

enum class NxPalette(val displayName: String, val dark: Boolean) {
    Midnight("Midnight", true),
    Espresso("Espresso", true),
    Nardo("Nardo", true),
    Creame("Creame", false),
    Mist("Mist", false);

    val colors: NxColors
        get() = when (this) {
            Midnight -> NxColorsMidnight
            Espresso -> NxColorsEspresso
            Nardo -> NxColorsNardo
            Creame -> NxColorsCreame
            Mist -> NxColorsMist
        }

    companion object {
        val DefaultDark = Midnight
        val DefaultLight = Creame
    }
}
