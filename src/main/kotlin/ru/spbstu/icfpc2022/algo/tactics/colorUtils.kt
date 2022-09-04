package ru.spbstu.icfpc2022.algo.tactics

import com.sksamuel.scrimage.ImmutableImage
import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.imageParser.euclid
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import ru.spbstu.icfpc2022.move.ColorMove
import kotlin.math.round
import kotlin.math.sqrt

enum class ColoringMethod { AVERAGE, MEDIAN, MAX, GEOMETRIC_MEDIAN }

fun digest(image: ImmutableImage, shape: Shape, color: Color): Double {
    var total = 0.0
    for (x in shape.lowerLeftInclusive.x until shape.upperRightExclusive.x) {
        for (y in shape.lowerLeftInclusive.y until shape.upperRightExclusive.y) {
            val pixel = image[x, y]
            total += euclid(
                color.r - pixel.red(), color.g - pixel.green(), color.b - pixel.blue(), color.a - pixel.alpha()
            )
        }
    }
    return total
}

fun computeAverageColor(image: ImmutableImage, shape: Shape, coloringMethod: ColoringMethod): Color {
    return when (coloringMethod) {
        ColoringMethod.AVERAGE -> computeBlockAverage(image, shape)
        ColoringMethod.MEDIAN -> computeBlockMedian(image, shape)
        ColoringMethod.MAX -> computeBlockMax(image, shape)
        ColoringMethod.GEOMETRIC_MEDIAN -> computeBlockGeometricMedianApproximated(image, shape)
    }
}

fun computeBlockAverage(image: ImmutableImage, shape: Shape): Color {
    var r = 0L
    var g = 0L
    var b = 0L
    var a = 0L
    var count = 0
    for (x in shape.lowerLeftInclusive.x until shape.upperRightExclusive.x) {
        for (y in shape.lowerLeftInclusive.y until shape.upperRightExclusive.y) {
            val pixel = image[x, y]
            r += pixel.red()
            g += pixel.green()
            b += pixel.blue()
            a += pixel.alpha()
            count++
        }
    }
    return Color(
        round(r.toDouble() / count).toInt(),
        round(g.toDouble() / count).toInt(),
        round(b.toDouble() / count).toInt(),
        round(a.toDouble() / count).toInt()
    )
}

fun computeNotBlockAverage(image: ImmutableImage, shape: Shape): Color {
    var r = 0L
    var g = 0L
    var b = 0L
    var a = 0L
    var count = 0
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            if (!Point(x, y).isStrictlyInside(shape.lowerLeftInclusive, shape.upperRightExclusive)) {
                val pixel = image[x, y]
                r += pixel.red()
                g += pixel.green()
                b += pixel.blue()
                a += pixel.alpha()
                count++
            }
        }
    }
    return Color(
        round(r.toDouble() / count).toInt(),
        round(g.toDouble() / count).toInt(),
        round(b.toDouble() / count).toInt(),
        round(a.toDouble() / count).toInt()
    )
}

fun computeBlockMedian(image: ImmutableImage, shape: Shape): Color {
    val red = IntArray(256)
    val green = IntArray(256)
    val blue = IntArray(256)
    val alpha = IntArray(256)
    for (x in shape.lowerLeftInclusive.x until shape.upperRightExclusive.x) {
        for (y in shape.lowerLeftInclusive.y until shape.upperRightExclusive.y) {
            val pixel = image[x, y]
            red[pixel.red()]++
            green[pixel.green()]++
            blue[pixel.blue()]++
            alpha[pixel.alpha()]++
        }
    }
    return Color(
        red.withIndex().maxBy { it.value }.index,
        green.withIndex().maxBy { it.value }.index,
        blue.withIndex().maxBy { it.value }.index,
        alpha.withIndex().maxBy { it.value }.index,
    )
}

fun computeBlockMax(image: ImmutableImage, shape: Shape): Color {
    val colors = mutableMapOf<Color, Int>()
    for (x in shape.lowerLeftInclusive.x until shape.upperRightExclusive.x) {
        for (y in shape.lowerLeftInclusive.y until shape.upperRightExclusive.y) {
            val pixel = image[x, y]
            val col = pixel.getCanvasColor()
            colors[col] = colors.getOrDefault(col, 0) + 1
        }
    }
    return colors.maxBy { it.value }.key
}

fun computeBlockMax(image: ImmutableImage, start: Point, end: Point): Color {
    val colors = mutableMapOf<Color, Int>()
    for (x in start.x until end.x) {
        for (y in start.y until end.y) {
            val pixel = image[x, y]
            val col = pixel.getCanvasColor()
            colors[col] = colors.getOrDefault(col, 0) + 1
        }
    }
    return colors.maxBy { it.value }.key
}

private fun square(x: Int) = x * x

private fun distance(
    rData: IntArray,
    gData: IntArray,
    bData: IntArray,
    aData: IntArray,
    r: Int,
    g: Int,
    b: Int,
    a: Int
): Double {
    var distance = 0.0

    for (i in rData.indices) {
        var sum = 0.0
        sum += square(rData[i] - r)
        sum += square(gData[i] - g)
        sum += square(bData[i] - b)
        sum += square(aData[i] - a)
        distance += sqrt(sum)
    }

    return distance
}

fun computeBlockGeometricMedianApproximated(image: ImmutableImage, shape: Shape): Color {
    var rAvgCnt = 0.0
    var rMin = Int.MAX_VALUE
    var rMax = Int.MIN_VALUE
    val rData = IntArray(shape.size.toInt())

    var gAvgCnt = 0.0
    var gMin = Int.MAX_VALUE
    var gMax = Int.MIN_VALUE
    val gData = IntArray(shape.size.toInt())

    var bAvgCnt = 0.0
    var bMin = Int.MAX_VALUE
    var bMax = Int.MIN_VALUE
    val bData = IntArray(shape.size.toInt())

    var aAvgCnt = 0.0
    var aMin = Int.MAX_VALUE
    var aMax = Int.MIN_VALUE
    val aData = IntArray(shape.size.toInt())

    var idx = 0
    for (x in shape.lowerLeftInclusive.x until shape.upperRightExclusive.x) {
        for (y in shape.lowerLeftInclusive.y until shape.upperRightExclusive.y) {
            val pixel = image[x, y]
            rData[idx] = pixel.red()
            gData[idx] = pixel.green()
            bData[idx] = pixel.blue()
            aData[idx] = pixel.alpha()

            rAvgCnt += rData[idx]
            rMin = minOf(rMin, rData[idx])
            rMax = maxOf(rMax, rData[idx])

            gAvgCnt += gData[idx]
            gMin = minOf(gMin, gData[idx])
            gMax = maxOf(gMax, gData[idx])

            bAvgCnt += bData[idx]
            bMin = minOf(bMin, bData[idx])
            bMax = maxOf(bMax, bData[idx])

            aAvgCnt += aData[idx]
            aMin = minOf(aMin, aData[idx])
            aMax = maxOf(aMax, aData[idx])

            idx++
        }
    }

    val rAvg = round(rAvgCnt / idx).toInt()
    val gAvg = round(gAvgCnt / idx).toInt()
    val bAvg = round(bAvgCnt / idx).toInt()
    val aAvg = round(aAvgCnt / idx).toInt()

    return approximateGeometricMedian(
        rMin,
        rMax,
        gMin,
        gMax,
        bMin,
        bMax,
        aMin,
        aMax,
        rAvg,
        gAvg,
        bAvg,
        aAvg,
        rData,
        gData,
        bData,
        aData
    )
}


fun computeNotBlockGeometricMedianApproximated(image: ImmutableImage, shape: Shape): Color {
    var rAvgCnt = 0.0
    var rMin = Int.MAX_VALUE
    var rMax = Int.MIN_VALUE
    val rData = ArrayList<Int>((image.width * image.height) - shape.size.toInt() + 2)

    var gAvgCnt = 0.0
    var gMin = Int.MAX_VALUE
    var gMax = Int.MIN_VALUE
    val gData = ArrayList<Int>((image.width * image.height) - shape.size.toInt() + 2)

    var bAvgCnt = 0.0
    var bMin = Int.MAX_VALUE
    var bMax = Int.MIN_VALUE
    val bData = ArrayList<Int>((image.width * image.height) - shape.size.toInt() + 2)

    var aAvgCnt = 0.0
    var aMin = Int.MAX_VALUE
    var aMax = Int.MIN_VALUE
    val aData = ArrayList<Int>((image.width * image.height) - shape.size.toInt() + 2)

    var idx = 0
    for (x in 0 until image.width) {
        for (y in 0 until image.height) {
            if (Point(x, y).isStrictlyInside(shape.lowerLeftInclusive, shape.upperRightExclusive)) continue

            val pixel = image[x, y]
            rData.add(pixel.red())
            gData.add(pixel.green())
            bData.add(pixel.blue())
            aData.add(pixel.alpha())

            rAvgCnt += rData[idx]
            rMin = minOf(rMin, rData[idx])
            rMax = maxOf(rMax, rData[idx])

            gAvgCnt += gData[idx]
            gMin = minOf(gMin, gData[idx])
            gMax = maxOf(gMax, gData[idx])

            bAvgCnt += bData[idx]
            bMin = minOf(bMin, bData[idx])
            bMax = maxOf(bMax, bData[idx])

            aAvgCnt += aData[idx]
            aMin = minOf(aMin, aData[idx])
            aMax = maxOf(aMax, aData[idx])

            idx++
        }
    }

    val rAvg = round(rAvgCnt / idx).toInt()
    val gAvg = round(gAvgCnt / idx).toInt()
    val bAvg = round(bAvgCnt / idx).toInt()
    val aAvg = round(aAvgCnt / idx).toInt()

    return approximateGeometricMedian(
        rMin,
        rMax,
        gMin,
        gMax,
        bMin,
        bMax,
        aMin,
        aMax,
        rAvg,
        gAvg,
        bAvg,
        aAvg,
        rData.toIntArray(),
        gData.toIntArray(),
        bData.toIntArray(),
        aData.toIntArray()
    )
}

private fun approximateGeometricMedian(
    rMin: Int,
    rMax: Int,
    gMin: Int,
    gMax: Int,
    bMin: Int,
    bMax: Int,
    aMin: Int,
    aMax: Int,
    rAvg: Int,
    gAvg: Int,
    bAvg: Int,
    aAvg: Int,
    rData: IntArray,
    gData: IntArray,
    bData: IntArray,
    aData: IntArray
): Color {
    val searchR = rMin != rMax
    val searchG = gMin != gMax
    val searchB = bMin != bMax
    val searchA = aMin != aMax

    var bestR = rAvg
    var bestG = gAvg
    var bestB = bAvg
    var bestA = aAvg

    if (!searchR && !searchG && !searchB && !searchA) {
        Color(bestR, bestG, bestB, bestA)
    }

    val avgDistance = distance(rData, gData, bData, aData, rAvg, gAvg, bAvg, aAvg)

    var bestDist = avgDistance
    var step = intArrayOf(rMax - rMin, gMax - gMin, bMax - bMin, aMax - aMin).average()

    while (step > 0.2) {
        val updated = approxLoop(searchR, bestR, rMin, rMax, step) { r ->
            approxLoop(searchG, bestG, gMin, gMax, step) { g ->
                approxLoop(searchB, bestB, bMin, bMax, step) { b ->
                    approxLoop(searchA, bestA, aMin, aMax, step) { a ->
                        val dist = distance(rData, gData, bData, aData, r, g, b, a)
                        if (dist < bestDist) {
                            bestDist = dist
                            bestR = r
                            bestG = g
                            bestB = b
                            bestA = a
                            true
                        } else {
                            false
                        }
                    }
                }
            }
        }

        if (!updated) {
            step /= 2
        }
    }

    return Color(bestR, bestG, bestB, bestA)
}

private inline fun approxLoop(
    search: Boolean,
    current: Int,
    min: Int,
    max: Int,
    step: Double,
    body: (Int) -> Boolean
): Boolean {
    if (search) {
        val m1 = round(current - step).toInt()
        if (m1 >= min) {
            if (body(m1)) return true
        }
        val p1 = round(current + step).toInt()
        if (p1 <= max) {
            if (body(p1)) return true
        }
    }
    return body(current)
}

public fun approximatelyMatches(color1: Color, color2: Color, tolerance: Int): Boolean {
    val refColor = color1
    val minColor = Color(
        (refColor.r - tolerance).coerceAtLeast(0),
        (refColor.g - tolerance).coerceAtLeast(0),
        (refColor.b - tolerance).coerceAtLeast(0),
        (refColor.a - tolerance).coerceAtLeast(0)
    )
    val maxColor = Color(
        (refColor.r + tolerance).coerceAtMost(255),
        (refColor.g + tolerance).coerceAtMost(255),
        (refColor.b + tolerance).coerceAtMost(255),
        (refColor.a + tolerance).coerceAtMost(255),
    )
    return color2.r in minColor.r..maxColor.r &&
            color2.g in minColor.g..maxColor.g &&
            color2.b in minColor.b..maxColor.b &&
            color2.a in minColor.a..maxColor.a
}

fun colorBlock(state: PersistentState, blockId: BlockId, color: Color, colorTolerance: Int): PersistentState {
    val block = state.canvas.blocks[blockId]
    if (block!!.simpleChildren().all { approximatelyMatches(it.color, color, colorTolerance)  }) return state

    return state.move(ColorMove(blockId, color))
}

