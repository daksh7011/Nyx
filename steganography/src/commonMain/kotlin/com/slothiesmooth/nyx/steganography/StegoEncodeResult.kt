package com.slothiesmooth.nyx.steganography

/** Outcome of [Steganography.encode]. */
sealed interface StegoEncodeResult {

    /** The framed payload was embedded; [images] are the stego covers (in input order). */
    data class Success(val images: List<PixelImage>) : StegoEncodeResult

    /**
     * The framed payload does not fit in the supplied covers.
     *
     * [requiredBits] is the storage the framed payload needs; [availableBits] is what the covers
     * offer (2 usable bits per R/G/B channel). Nothing was mutated.
     */
    data class CapacityExceeded(val requiredBits: Long, val availableBits: Long) : StegoEncodeResult
}
