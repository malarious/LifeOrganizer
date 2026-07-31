package com.geepscontrol.pixel

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PixelEngineTest {
    @Test
    fun `smart enlargement keeps pixel edges and palette`() {
        val source = PixelBitmap(2, 2, intArrayOf(RED, BLUE, BLUE, RED))

        val result = PixelEngine.resize(source, 4, 4, PixelResizeMode.SMART)

        assertEquals(RED, result.pixels[0])
        assertEquals(RED, result.pixels[1])
        assertEquals(BLUE, result.pixels[2])
        assertEquals(setOf(RED, BLUE), result.pixels.toSet())
    }

    @Test
    fun `pixelify obeys size palette and transparent pixels`() {
        val source =
            PixelBitmap(
                3,
                2,
                intArrayOf(
                    RED,
                    GREEN,
                    BLUE,
                    0,
                    WHITE,
                    BLACK,
                ),
            )

        val result = PixelEngine.pixelify(source, 6, 4, 3, dither = false, fit = PixelFitMode.STRETCH)

        assertEquals(6, result.width)
        assertEquals(4, result.height)
        assertTrue(PixelEngine.palette(result).size <= 3)
        assertTrue(result.pixels.any { it == 0 })
    }

    @Test
    fun `rotation flip and sprite sheet preserve exact pixels`() {
        val source = PixelBitmap(2, 1, intArrayOf(RED, BLUE))

        val rotated = PixelEngine.rotate90(source)
        val flipped = PixelEngine.flip(source, horizontal = true)
        val sheet = PixelEngine.spriteSheet(listOf(source, flipped), columns = 2)

        assertEquals(1, rotated.width)
        assertEquals(2, rotated.height)
        assertContentEquals(intArrayOf(RED, BLUE), rotated.pixels)
        assertContentEquals(intArrayOf(BLUE, RED), flipped.pixels)
        assertEquals(4, sheet.width)
        assertContentEquals(intArrayOf(RED, BLUE, BLUE, RED), sheet.pixels)
    }

    @Test
    fun `fill stays inside the connected region`() {
        val source =
            PixelBitmap(
                3,
                3,
                intArrayOf(
                    RED,
                    RED,
                    BLUE,
                    RED,
                    BLUE,
                    BLUE,
                    RED,
                    RED,
                    BLUE,
                ),
            )

        val result = PixelEngine.floodFill(source, 0, 0, GREEN)

        assertContentEquals(
            intArrayOf(
                GREEN,
                GREEN,
                BLUE,
                GREEN,
                BLUE,
                BLUE,
                GREEN,
                GREEN,
                BLUE,
            ),
            result.pixels,
        )
    }

    @Test
    fun `flatten composites visible layers and ignores hidden layers`() {
        val frame =
            PixelFrame(
                layers =
                    listOf(
                        PixelLayer("base", pixels = intArrayOf(RED)),
                        PixelLayer("hidden", visible = false, pixels = intArrayOf(BLUE)),
                        PixelLayer("top", opacity = 0.5f, pixels = intArrayOf(BLUE)),
                    ),
            )

        val result = PixelEngine.flatten(frame, 1, 1)

        val color = result.pixels.single()
        assertEquals(255, color ushr 24 and 0xff)
        assertTrue(color ushr 16 and 0xff in 126..129)
        assertTrue(color and 0xff in 126..129)
    }

    @Test
    fun `background removal follows connected corners and outline surrounds sprite`() {
        val source =
            PixelBitmap(
                3,
                3,
                intArrayOf(
                    WHITE,
                    WHITE,
                    WHITE,
                    WHITE,
                    RED,
                    WHITE,
                    WHITE,
                    WHITE,
                    WHITE,
                ),
            )

        val transparent = PixelEngine.removeCornerBackground(source, tolerance = 0)
        val outlined = PixelEngine.outline(transparent, BLUE)

        assertEquals(RED, outlined.pixels[4])
        assertContentEquals(
            intArrayOf(
                0,
                BLUE,
                0,
                BLUE,
                RED,
                BLUE,
                0,
                BLUE,
                0,
            ),
            outlined.pixels,
        )
    }

    @Test
    fun `editable project round trips with frames layers and pixels`() {
        val project =
            PixelProject(
                id = "sprite",
                name = "Hero",
                width = 2,
                height = 1,
                palette = listOf(RED, BLUE),
                frames =
                    listOf(
                        PixelFrame(
                            name = "Idle",
                            layers = listOf(PixelLayer("Body", pixels = intArrayOf(RED, BLUE))),
                        ),
                    ),
                updatedAt = 1,
            )

        val decoded = Json.decodeFromString<PixelProject>(Json.encodeToString(project))

        assertEquals(project.copy(frames = emptyList()), decoded.copy(frames = emptyList()))
        assertContentEquals(project.frames.single().layers.single().pixels, decoded.frames.single().layers.single().pixels)
    }

    private companion object {
        const val RED = -0x10000
        const val GREEN = -0xff0100
        const val BLUE = -0xffff01
        const val WHITE = -0x1
        const val BLACK = -0x1000000
    }
}
