package ru.spbstu.icfpc2022.algo.tactics

import com.sksamuel.scrimage.ImmutableImage
import ru.spbstu.icfpc2022.canvas.Color
import ru.spbstu.icfpc2022.canvas.Point
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.icfpc2022.imageParser.euclid
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import kotlin.math.sqrt

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
    return Color((r / count).toInt(), (g / count).toInt(), (b / count).toInt(), (a / count).toInt())
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
    return Color((r / count).toInt(), (g / count).toInt(), (b / count).toInt(), (a / count).toInt())
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
