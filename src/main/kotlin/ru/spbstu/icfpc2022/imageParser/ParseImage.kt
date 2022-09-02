package ru.spbstu.icfpc2022.imageParser

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import ru.spbstu.icfpc2022.canvas.Canvas
import ru.spbstu.icfpc2022.canvas.Color
import java.io.File
import java.net.URL

fun parseImage(path: String) = ImmutableImage.loader().fromFile(File(path))
fun parseImage(url: URL) = ImmutableImage.loader().fromStream(url.openStream())

operator fun ImmutableImage.get(x: Int, y: Int): Pixel = this.pixel(x, y)

fun Color.toAwt() = java.awt.Color(r.toInt(), g.toInt(), b.toInt(), a.toInt())

fun Canvas.toImage(): ImmutableImage {
    var image = ImmutableImage.create(width, height)
    for (block in this.allSimpleBlocks()) {
        image = image.overlay(ImmutableImage.filled(block.shape.width, block.shape.height, block.color.toAwt()))
    }
    return image
}

fun main() {
    val im = parseImage(URL("https://cdn.robovinci.xyz/imageframes/2.png"))
    var r = im.iterator().asSequence().map { it.red() }.average().toInt()
    var g = im.iterator().asSequence().map { it.green() }.average().toInt()
    var b = im.iterator().asSequence().map { it.blue() }.average().toInt()
    var a = im.iterator().asSequence().map { it.alpha() }.average().toInt()

    println("color [0] [$r,$g,$b,$a]")
}
