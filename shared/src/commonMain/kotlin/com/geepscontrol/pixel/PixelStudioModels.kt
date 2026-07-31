package com.geepscontrol.pixel

import kotlinx.serialization.Serializable
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

@Serializable
enum class PixelResizeMode { SMART, NEAREST, AREA, BILINEAR }

@Serializable
enum class PixelFitMode { CONTAIN, COVER, STRETCH }

@Serializable
data class PixelLayer(
    val name: String = "Layer 1",
    val visible: Boolean = true,
    val opacity: Float = 1f,
    val pixels: IntArray,
)

@Serializable
data class PixelFrame(
    val name: String = "Frame 1",
    val durationMillis: Int = 120,
    val layers: List<PixelLayer>,
)

@Serializable
data class PixelProject(
    val id: String,
    val name: String,
    val width: Int,
    val height: Int,
    val palette: List<Int> = emptyList(),
    val frames: List<PixelFrame>,
    val activeFrame: Int = 0,
    val activeLayer: Int = 0,
    val showGrid: Boolean = true,
    val showOnionSkin: Boolean = false,
    val updatedAt: Long,
)

data class PixelBitmap(
    val width: Int,
    val height: Int,
    val pixels: IntArray,
) {
    init {
        require(width > 0 && height > 0 && pixels.size == width * height)
    }
}

object PixelEngine {
    fun flatten(frame: PixelFrame, width: Int, height: Int): PixelBitmap {
        val output = IntArray(width * height)
        frame.layers.filter { it.visible }.forEach { layer ->
            val opacity = layer.opacity.coerceIn(0f, 1f)
            layer.pixels.forEachIndexed { index, color ->
                if (index < output.size) output[index] = composite(output[index], color, opacity)
            }
        }
        return PixelBitmap(width, height, output)
    }

    fun resize(
        source: PixelBitmap,
        targetWidth: Int,
        targetHeight: Int,
        mode: PixelResizeMode,
        fit: PixelFitMode = PixelFitMode.STRETCH,
        background: Int = 0,
    ): PixelBitmap {
        val width = targetWidth.coerceIn(1, 512)
        val height = targetHeight.coerceIn(1, 512)
        if (source.width == width && source.height == height) return source.copy(pixels = source.pixels.copyOf())
        if (fit == PixelFitMode.STRETCH) return resizeStretched(source, width, height, selectMode(source, width, height, mode))
        val scale =
            if (fit == PixelFitMode.CONTAIN) {
                min(width.toDouble() / source.width, height.toDouble() / source.height)
            } else {
                max(width.toDouble() / source.width, height.toDouble() / source.height)
            }
        val scaledWidth = max(1, (source.width * scale).roundToInt())
        val scaledHeight = max(1, (source.height * scale).roundToInt())
        val scaled = resizeStretched(source, scaledWidth, scaledHeight, selectMode(source, scaledWidth, scaledHeight, mode))
        val output = IntArray(width * height) { background }
        val sourceX = max(0, (scaledWidth - width) / 2)
        val sourceY = max(0, (scaledHeight - height) / 2)
        val targetX = max(0, (width - scaledWidth) / 2)
        val targetY = max(0, (height - scaledHeight) / 2)
        val copyWidth = min(width, scaledWidth)
        val copyHeight = min(height, scaledHeight)
        repeat(copyHeight) { y ->
            repeat(copyWidth) { x ->
                output[(y + targetY) * width + x + targetX] = scaled.pixels[(y + sourceY) * scaledWidth + x + sourceX]
            }
        }
        return PixelBitmap(width, height, output)
    }

    fun pixelify(
        source: PixelBitmap,
        targetWidth: Int,
        targetHeight: Int,
        colorCount: Int,
        dither: Boolean,
        fit: PixelFitMode,
        transparentThreshold: Int = 8,
    ): PixelBitmap {
        val resized = resize(source, targetWidth, targetHeight, PixelResizeMode.SMART, fit)
        val cleaned = resized.copy(pixels = resized.pixels.map { color -> if (alpha(color) <= transparentThreshold) 0 else color }.toIntArray())
        return quantize(cleaned, colorCount.coerceIn(2, 64), dither)
    }

    fun quantize(source: PixelBitmap, colorCount: Int, dither: Boolean): PixelBitmap {
        val opaque = source.pixels.filter { alpha(it) > 0 }
        if (opaque.isEmpty()) return source
        val palette = buildPalette(opaque, colorCount.coerceIn(2, 64))
        if (!dither) {
            return source.copy(pixels = source.pixels.map { if (alpha(it) == 0) 0 else nearestColor(it, palette) }.toIntArray())
        }
        val workR = FloatArray(source.pixels.size) { red(source.pixels[it]).toFloat() }
        val workG = FloatArray(source.pixels.size) { green(source.pixels[it]).toFloat() }
        val workB = FloatArray(source.pixels.size) { blue(source.pixels[it]).toFloat() }
        val result = IntArray(source.pixels.size)
        repeat(source.height) { y ->
            repeat(source.width) xLoop@{ x ->
                val index = y * source.width + x
                if (alpha(source.pixels[index]) == 0) return@xLoop
                val original = argb(alpha(source.pixels[index]), workR[index].roundToInt(), workG[index].roundToInt(), workB[index].roundToInt())
                val chosen = nearestColor(original, palette)
                result[index] = chosen
                val errorR = workR[index] - red(chosen)
                val errorG = workG[index] - green(chosen)
                val errorB = workB[index] - blue(chosen)
                spreadError(workR, workG, workB, source.width, source.height, x + 1, y, errorR, errorG, errorB, 7f / 16f)
                spreadError(workR, workG, workB, source.width, source.height, x - 1, y + 1, errorR, errorG, errorB, 3f / 16f)
                spreadError(workR, workG, workB, source.width, source.height, x, y + 1, errorR, errorG, errorB, 5f / 16f)
                spreadError(workR, workG, workB, source.width, source.height, x + 1, y + 1, errorR, errorG, errorB, 1f / 16f)
            }
        }
        return PixelBitmap(source.width, source.height, result)
    }

    fun palette(source: PixelBitmap, maximum: Int = 32): List<Int> =
        source.pixels
            .asSequence()
            .filter { alpha(it) > 0 }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .take(maximum)
            .map { it.key }

    fun replaceColor(source: PixelBitmap, from: Int, to: Int, tolerance: Int = 20): PixelBitmap =
        source.copy(
            pixels =
                source.pixels.map {
                    if (alpha(it) > 0 && colorDistance(it, from) <= tolerance * tolerance * 3) {
                        argb(alpha(it), red(to), green(to), blue(to))
                    } else {
                        it
                    }
                }.toIntArray(),
        )

    fun removeCornerBackground(source: PixelBitmap, tolerance: Int = 24): PixelBitmap {
        val output = source.pixels.copyOf()
        val visited = BooleanArray(output.size)
        val seeds =
            listOf(
                0,
                source.width - 1,
                (source.height - 1) * source.width,
                source.pixels.lastIndex,
            ).distinct()
        seeds.forEach { seed ->
            val reference = source.pixels[seed]
            val queue = ArrayDeque<Int>()
            queue.add(seed)
            while (queue.isNotEmpty()) {
                val index = queue.removeFirst()
                if (visited[index] || colorDistance(source.pixels[index], reference) > tolerance * tolerance * 3) continue
                visited[index] = true
                output[index] = 0
                val x = index % source.width
                val y = index / source.width
                if (x > 0) queue.add(index - 1)
                if (x + 1 < source.width) queue.add(index + 1)
                if (y > 0) queue.add(index - source.width)
                if (y + 1 < source.height) queue.add(index + source.width)
            }
        }
        return source.copy(pixels = output)
    }

    fun outline(source: PixelBitmap, color: Int): PixelBitmap {
        val output = source.pixels.copyOf()
        repeat(source.height) { y ->
            repeat(source.width) xLoop@{ x ->
                val index = y * source.width + x
                if (alpha(source.pixels[index]) != 0) return@xLoop
                val touchesOpaque =
                    (x > 0 && alpha(source.pixels[index - 1]) > 0) ||
                        (x + 1 < source.width && alpha(source.pixels[index + 1]) > 0) ||
                        (y > 0 && alpha(source.pixels[index - source.width]) > 0) ||
                        (y + 1 < source.height && alpha(source.pixels[index + source.width]) > 0)
                if (touchesOpaque) output[index] = color
            }
        }
        return source.copy(pixels = output)
    }

    fun adjustColor(source: PixelBitmap, hueDegrees: Float, saturation: Float, brightness: Float, contrast: Float): PixelBitmap =
        source.copy(
            pixels =
                source.pixels.map { color ->
                    if (alpha(color) == 0) return@map color
                    val hsv = rgbToHsv(red(color), green(color), blue(color))
                    val adjustedHue = ((hsv[0] + hueDegrees) % 360f + 360f) % 360f
                    val adjustedSaturation = (hsv[1] * saturation).coerceIn(0f, 1f)
                    val adjustedValue = (hsv[2] * brightness).coerceIn(0f, 1f)
                    val rgb = hsvToRgb(adjustedHue, adjustedSaturation, adjustedValue)
                    val factor = contrast.coerceIn(0f, 3f)
                    argb(
                        alpha(color),
                        (((rgb[0] - 128) * factor) + 128).roundToInt(),
                        (((rgb[1] - 128) * factor) + 128).roundToInt(),
                        (((rgb[2] - 128) * factor) + 128).roundToInt(),
                    )
                }.toIntArray(),
        )

    fun rotate90(source: PixelBitmap, clockwise: Boolean = true): PixelBitmap {
        val output = IntArray(source.pixels.size)
        repeat(source.height) { y ->
            repeat(source.width) { x ->
                val newX = if (clockwise) source.height - 1 - y else y
                val newY = if (clockwise) x else source.width - 1 - x
                output[newY * source.height + newX] = source.pixels[y * source.width + x]
            }
        }
        return PixelBitmap(source.height, source.width, output)
    }

    fun rotate(source: PixelBitmap, degrees: Float, expand: Boolean = false): PixelBitmap {
        val radians = degrees * kotlin.math.PI / 180.0
        val cosine = cos(radians)
        val sine = sin(radians)
        val width =
            if (expand) ceil(abs(source.width * cosine) + abs(source.height * sine)).toInt() else source.width
        val height =
            if (expand) ceil(abs(source.height * cosine) + abs(source.width * sine)).toInt() else source.height
        val output = IntArray(width * height)
        val sourceCx = (source.width - 1) / 2.0
        val sourceCy = (source.height - 1) / 2.0
        val targetCx = (width - 1) / 2.0
        val targetCy = (height - 1) / 2.0
        repeat(height) { y ->
            repeat(width) { x ->
                val dx = x - targetCx
                val dy = y - targetCy
                val sourceX = (dx * cosine + dy * sine + sourceCx).roundToInt()
                val sourceY = (-dx * sine + dy * cosine + sourceCy).roundToInt()
                if (sourceX in 0 until source.width && sourceY in 0 until source.height) {
                    output[y * width + x] = source.pixels[sourceY * source.width + sourceX]
                }
            }
        }
        return PixelBitmap(width, height, output)
    }

    fun flip(source: PixelBitmap, horizontal: Boolean): PixelBitmap {
        val output = IntArray(source.pixels.size)
        repeat(source.height) { y ->
            repeat(source.width) { x ->
                val sourceX = if (horizontal) source.width - 1 - x else x
                val sourceY = if (horizontal) y else source.height - 1 - y
                output[y * source.width + x] = source.pixels[sourceY * source.width + sourceX]
            }
        }
        return source.copy(pixels = output)
    }

    fun floodFill(source: PixelBitmap, x: Int, y: Int, replacement: Int): PixelBitmap {
        if (x !in 0 until source.width || y !in 0 until source.height) return source
        val output = source.pixels.copyOf()
        val target = output[y * source.width + x]
        if (target == replacement) return source
        val queue = ArrayDeque<Int>()
        queue.add(y * source.width + x)
        while (queue.isNotEmpty()) {
            val index = queue.removeFirst()
            if (output[index] != target) continue
            output[index] = replacement
            val px = index % source.width
            val py = index / source.width
            if (px > 0) queue.add(index - 1)
            if (px + 1 < source.width) queue.add(index + 1)
            if (py > 0) queue.add(index - source.width)
            if (py + 1 < source.height) queue.add(index + source.width)
        }
        return source.copy(pixels = output)
    }

    fun line(source: PixelBitmap, fromX: Int, fromY: Int, toX: Int, toY: Int, color: Int, brushSize: Int = 1): PixelBitmap {
        val output = source.pixels.copyOf()
        var x = fromX
        var y = fromY
        val dx = abs(toX - fromX)
        val stepX = if (fromX < toX) 1 else -1
        val dy = -abs(toY - fromY)
        val stepY = if (fromY < toY) 1 else -1
        var error = dx + dy
        while (true) {
            paint(output, source.width, source.height, x, y, color, brushSize)
            if (x == toX && y == toY) break
            val twice = 2 * error
            if (twice >= dy) {
                error += dy
                x += stepX
            }
            if (twice <= dx) {
                error += dx
                y += stepY
            }
        }
        return source.copy(pixels = output)
    }

    fun spriteSheet(frames: List<PixelBitmap>, columns: Int = frames.size.coerceAtLeast(1)): PixelBitmap {
        require(frames.isNotEmpty())
        val frameWidth = frames.maxOf { it.width }
        val frameHeight = frames.maxOf { it.height }
        val columnCount = columns.coerceIn(1, frames.size)
        val rows = ceil(frames.size.toDouble() / columnCount).toInt()
        val output = IntArray(frameWidth * columnCount * frameHeight * rows)
        frames.forEachIndexed { index, frame ->
            val startX = (index % columnCount) * frameWidth
            val startY = (index / columnCount) * frameHeight
            repeat(frame.height) { y ->
                repeat(frame.width) { x ->
                    output[(startY + y) * frameWidth * columnCount + startX + x] = frame.pixels[y * frame.width + x]
                }
            }
        }
        return PixelBitmap(frameWidth * columnCount, frameHeight * rows, output)
    }

    private fun selectMode(source: PixelBitmap, width: Int, height: Int, requested: PixelResizeMode): PixelResizeMode {
        if (requested != PixelResizeMode.SMART) return requested
        val unique = source.pixels.asSequence().filter { alpha(it) > 0 }.take(513).toSet().size
        val appearsPixelArt = unique <= 256
        return when {
            width < source.width || height < source.height -> PixelResizeMode.AREA
            appearsPixelArt -> PixelResizeMode.NEAREST
            else -> PixelResizeMode.BILINEAR
        }
    }

    private fun resizeStretched(source: PixelBitmap, width: Int, height: Int, mode: PixelResizeMode): PixelBitmap =
        when (mode) {
            PixelResizeMode.NEAREST, PixelResizeMode.SMART -> nearestResize(source, width, height)
            PixelResizeMode.BILINEAR -> bilinearResize(source, width, height)
            PixelResizeMode.AREA -> areaResize(source, width, height)
        }

    private fun nearestResize(source: PixelBitmap, width: Int, height: Int): PixelBitmap {
        val output = IntArray(width * height)
        repeat(height) { y ->
            val sourceY = min(source.height - 1, y * source.height / height)
            repeat(width) { x ->
                val sourceX = min(source.width - 1, x * source.width / width)
                output[y * width + x] = source.pixels[sourceY * source.width + sourceX]
            }
        }
        return PixelBitmap(width, height, output)
    }

    private fun bilinearResize(source: PixelBitmap, width: Int, height: Int): PixelBitmap {
        val output = IntArray(width * height)
        repeat(height) { y ->
            val sourceY = (y + 0.5) * source.height / height - 0.5
            val y0 = floor(sourceY).toInt().coerceIn(0, source.height - 1)
            val y1 = (y0 + 1).coerceAtMost(source.height - 1)
            val fy = (sourceY - floor(sourceY)).toFloat()
            repeat(width) { x ->
                val sourceX = (x + 0.5) * source.width / width - 0.5
                val x0 = floor(sourceX).toInt().coerceIn(0, source.width - 1)
                val x1 = (x0 + 1).coerceAtMost(source.width - 1)
                val fx = (sourceX - floor(sourceX)).toFloat()
                output[y * width + x] =
                    interpolate(
                        source.pixels[y0 * source.width + x0],
                        source.pixels[y0 * source.width + x1],
                        source.pixels[y1 * source.width + x0],
                        source.pixels[y1 * source.width + x1],
                        fx,
                        fy,
                    )
            }
        }
        return PixelBitmap(width, height, output)
    }

    private fun areaResize(source: PixelBitmap, width: Int, height: Int): PixelBitmap {
        if (width >= source.width && height >= source.height) return bilinearResize(source, width, height)
        val output = IntArray(width * height)
        repeat(height) { y ->
            val startY = floor(y * source.height.toDouble() / height).toInt()
            val endY = max(startY + 1, ceil((y + 1) * source.height.toDouble() / height).toInt()).coerceAtMost(source.height)
            repeat(width) { x ->
                val startX = floor(x * source.width.toDouble() / width).toInt()
                val endX = max(startX + 1, ceil((x + 1) * source.width.toDouble() / width).toInt()).coerceAtMost(source.width)
                var totalA = 0
                var totalR = 0
                var totalG = 0
                var totalB = 0
                var count = 0
                for (sourceY in startY until endY) {
                    for (sourceX in startX until endX) {
                        val color = source.pixels[sourceY * source.width + sourceX]
                        totalA += alpha(color)
                        totalR += red(color)
                        totalG += green(color)
                        totalB += blue(color)
                        count++
                    }
                }
                output[y * width + x] = argb(totalA / count, totalR / count, totalG / count, totalB / count)
            }
        }
        val existingPalette = palette(source, 64)
        return if (existingPalette.size <= 64) {
            PixelBitmap(width, height, output.map { if (alpha(it) == 0) 0 else nearestColor(it, existingPalette) }.toIntArray())
        } else {
            PixelBitmap(width, height, output)
        }
    }

    private fun buildPalette(colors: List<Int>, count: Int): List<Int> {
        val samples =
            if (colors.size <= 20_000) {
                colors
            } else {
                List(20_000) { index -> colors[index * colors.size / 20_000] }
            }
        val unique = samples.distinct()
        if (unique.size <= count) return unique
        var centroids = List(count) { index -> unique[index * unique.size / count] }
        repeat(10) {
            val sums = Array(count) { LongArray(4) }
            samples.forEach { color ->
                val group = centroids.indices.minBy { colorDistance(color, centroids[it]) }
                sums[group][0] += red(color)
                sums[group][1] += green(color)
                sums[group][2] += blue(color)
                sums[group][3]++
            }
            centroids =
                centroids.mapIndexed { index, old ->
                    val samples = sums[index][3]
                    if (samples == 0L) old else argb(255, (sums[index][0] / samples).toInt(), (sums[index][1] / samples).toInt(), (sums[index][2] / samples).toInt())
                }
        }
        return centroids.distinct()
    }

    private fun nearestColor(color: Int, palette: List<Int>): Int =
        palette.minBy { colorDistance(color, it) }.let { argb(alpha(color), red(it), green(it), blue(it)) }

    private fun colorDistance(first: Int, second: Int): Int {
        val redDifference = red(first) - red(second)
        val greenDifference = green(first) - green(second)
        val blueDifference = blue(first) - blue(second)
        return redDifference * redDifference + greenDifference * greenDifference + blueDifference * blueDifference
    }

    private fun spreadError(r: FloatArray, g: FloatArray, b: FloatArray, width: Int, height: Int, x: Int, y: Int, er: Float, eg: Float, eb: Float, amount: Float) {
        if (x !in 0 until width || y !in 0 until height) return
        val index = y * width + x
        r[index] = (r[index] + er * amount).coerceIn(0f, 255f)
        g[index] = (g[index] + eg * amount).coerceIn(0f, 255f)
        b[index] = (b[index] + eb * amount).coerceIn(0f, 255f)
    }

    private fun interpolate(topLeft: Int, topRight: Int, bottomLeft: Int, bottomRight: Int, x: Float, y: Float): Int {
        fun channel(selector: (Int) -> Int): Int {
            val top = selector(topLeft) * (1 - x) + selector(topRight) * x
            val bottom = selector(bottomLeft) * (1 - x) + selector(bottomRight) * x
            return (top * (1 - y) + bottom * y).roundToInt()
        }
        return argb(channel(::alpha), channel(::red), channel(::green), channel(::blue))
    }

    private fun composite(background: Int, foreground: Int, layerOpacity: Float): Int {
        val foregroundAlpha = alpha(foreground) / 255f * layerOpacity
        val backgroundAlpha = alpha(background) / 255f
        val outputAlpha = foregroundAlpha + backgroundAlpha * (1 - foregroundAlpha)
        if (outputAlpha <= 0f) return 0
        fun channel(front: Int, back: Int): Int =
            ((front * foregroundAlpha + back * backgroundAlpha * (1 - foregroundAlpha)) / outputAlpha).roundToInt()
        return argb((outputAlpha * 255).roundToInt(), channel(red(foreground), red(background)), channel(green(foreground), green(background)), channel(blue(foreground), blue(background)))
    }

    private fun paint(pixels: IntArray, width: Int, height: Int, x: Int, y: Int, color: Int, brushSize: Int) {
        val radius = (brushSize.coerceAtLeast(1) - 1) / 2
        for (paintY in y - radius..y + radius) {
            for (paintX in x - radius..x + radius) {
                if (paintX in 0 until width && paintY in 0 until height) pixels[paintY * width + paintX] = color
            }
        }
    }

    private fun rgbToHsv(red: Int, green: Int, blue: Int): FloatArray {
        val r = red / 255f
        val g = green / 255f
        val b = blue / 255f
        val maximum = max(r, max(g, b))
        val minimum = min(r, min(g, b))
        val delta = maximum - minimum
        val hue =
            when {
                delta == 0f -> 0f
                maximum == r -> 60f * (((g - b) / delta) % 6f)
                maximum == g -> 60f * (((b - r) / delta) + 2f)
                else -> 60f * (((r - g) / delta) + 4f)
            }
        return floatArrayOf((hue + 360f) % 360f, if (maximum == 0f) 0f else delta / maximum, maximum)
    }

    private fun hsvToRgb(hue: Float, saturation: Float, value: Float): IntArray {
        val chroma = value * saturation
        val x = chroma * (1 - abs((hue / 60f) % 2 - 1))
        val m = value - chroma
        val values =
            when {
                hue < 60 -> floatArrayOf(chroma, x, 0f)
                hue < 120 -> floatArrayOf(x, chroma, 0f)
                hue < 180 -> floatArrayOf(0f, chroma, x)
                hue < 240 -> floatArrayOf(0f, x, chroma)
                hue < 300 -> floatArrayOf(x, 0f, chroma)
                else -> floatArrayOf(chroma, 0f, x)
            }
        return intArrayOf(((values[0] + m) * 255).roundToInt(), ((values[1] + m) * 255).roundToInt(), ((values[2] + m) * 255).roundToInt())
    }

    private fun alpha(color: Int) = color ushr 24 and 0xff
    private fun red(color: Int) = color ushr 16 and 0xff
    private fun green(color: Int) = color ushr 8 and 0xff
    private fun blue(color: Int) = color and 0xff
    private fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int =
        (alpha.coerceIn(0, 255) shl 24) or (red.coerceIn(0, 255) shl 16) or (green.coerceIn(0, 255) shl 8) or blue.coerceIn(0, 255)
}
