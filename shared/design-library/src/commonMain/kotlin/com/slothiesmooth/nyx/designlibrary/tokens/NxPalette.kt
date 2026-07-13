package com.slothiesmooth.nyx.designlibrary.tokens

enum class NxPalette(val displayName: String, val dark: Boolean) {
    Umbra("Umbra", true),
    Eclipse("Eclipse", true),
    Dusk("Dusk", true),
    Moonlight("Moonlight", false),
    Dawn("Dawn", false);

    val colors: NxColors
        get() = when (this) {
            Umbra -> NxColorsUmbra
            Eclipse -> NxColorsEclipse
            Dusk -> NxColorsDusk
            Moonlight -> NxColorsMoonlight
            Dawn -> NxColorsDawn
        }

    companion object {
        val DefaultDark = Umbra
        val DefaultLight = Moonlight
    }
}
