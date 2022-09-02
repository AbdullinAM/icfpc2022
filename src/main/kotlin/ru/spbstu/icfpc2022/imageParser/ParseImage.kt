package ru.spbstu.icfpc2022.imageParser

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.pixels.Pixel
import java.io.File
import java.net.URL

fun parseImage(path: String) = ImmutableImage.loader().fromFile(File(path))
fun parseImage(url: URL) = ImmutableImage.loader().fromStream(url.openStream())

fun ImmutableImage.get(x: Int, y: Int) = this.pixel(x, y)

fun main() {
    val im = parseImage(URL("https://cdn.robovinci.xyz/imageframes/2.png"))
    var r = im.iterator().asSequence().map { it.red() }.average().toInt()
    var g = im.iterator().asSequence().map { it.green() }.average().toInt()
    var b = im.iterator().asSequence().map { it.blue() }.average().toInt()
    var a = im.iterator().asSequence().map { it.alpha() }.average().toInt()

    println("color [0] [$r,$g,$b,$a]")
}
