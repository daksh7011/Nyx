package com.slothiesmooth.nyx.steganography

/**
 * A decoded raster image as a flat, row-major array of ARGB integers.
 *
 * Each entry packs one pixel as `0xAARRGGBB`. [pixels] length is always [width] * [height].
 */
class PixelImage(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
) {
    init {
        require(pixels.size == width * height) {
            "pixels size ${pixels.size} must equal width * height (${width * height})"
        }
    }
}
