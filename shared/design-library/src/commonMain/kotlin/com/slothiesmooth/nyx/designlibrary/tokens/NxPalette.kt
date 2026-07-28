package com.slothiesmooth.nyx.designlibrary.tokens

enum class NxPalette(val displayName: String, val dark: Boolean) {
    Midnight("Midnight", true),
    Espresso("Espresso", true),
    Nardo("Nardo", true),
    Cream("Cream", false),
    Mist("Mist", false);

    val colors: NxColors
        get() = when (this) {
            Midnight -> NxColorsMidnight
            Espresso -> NxColorsEspresso
            Nardo -> NxColorsNardo
            Cream -> NxColorsCream
            Mist -> NxColorsMist
        }

    companion object {
        val DefaultDark = Nardo
        val DefaultLight = Cream
    }
}
