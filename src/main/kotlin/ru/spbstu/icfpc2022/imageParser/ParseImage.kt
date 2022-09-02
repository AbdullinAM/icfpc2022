package ru.spbstu.icfpc2022.imageParser

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.Color
import ru.spbstu.icfpc2022.canvas.Shape
import ru.spbstu.ktuples.zip
import java.io.File
import java.net.URL
import kotlin.math.sqrt

fun parseImage(path: String) = ImmutableImage.loader().fromFile(File(path)).flipY()
fun parseImage(url: URL) = ImmutableImage.loader().fromStream(url.openStream()).flipY()


fun Pixel.getCanvasColor() = Color(red(), green(), blue(), alpha())

operator fun ImmutableImage.get(x: Int, y: Int): Pixel = this.pixel(x, y)

fun ImmutableImage.subimage(shape: Shape): ImmutableImage = subimage(shape.lowerLeft.x, shape.lowerLeft.y, shape.width, shape.height)

fun Color.toAwt() = java.awt.Color(r.toInt(), g.toInt(), b.toInt(), a.toInt())

fun Canvas.toImage(): ImmutableImage {
    var image = ImmutableImage.create(width, height)
    for (block in this.allSimpleBlocks()) {
        image = image.overlay(
            ImmutableImage.filled(
                block.shape.width,
                block.shape.height,
                block.color.toAwt()
            ),
            block.shape.lowerLeft.x,
            block.shape.lowerLeft.y
        )
    }
    return image
}

fun euclid(vararg vals: Double) = sqrt(vals.sumOf { it * it })
fun euclid(vararg vals: Int) = sqrt(vals.sumOf { it.toDouble() * it })

fun score(canvas: Canvas, target: ImmutableImage): Double {
    check(canvas.width == target.width)
    check(canvas.height == target.height)

    val cIm = canvas.toImage()

    return zip(target.iterator().asSequence(), cIm.iterator().asSequence()) { t, s ->
        check(t.x == s.x)
        check(t.y == s.y)
        euclid(t.red() - s.red(), t.green() - s.green(), t.blue() - s.blue(), t.alpha() - s.alpha())
    }.sum()
}

val Pixel.color: Color get() = Color(red(), green(), blue(), alpha())

fun main() {
    val im = parseImage(URL("https://cdn.robovinci.xyz/imageframes/2.png"))
    var r = im.iterator().asSequence().map { it.red() }.average().toInt()
    var g = im.iterator().asSequence().map { it.green() }.average().toInt()
    var b = im.iterator().asSequence().map { it.blue() }.average().toInt()
    var a = im.iterator().asSequence().map { it.alpha() }.average().toInt()

    println("color [0] [$r,$g,$b,$a]")
}
