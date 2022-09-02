package ru.spbstu.icfpc2022.imageParser

import com.sksamuel.scrimage.ImmutableImage
import java.io.File

fun parseImage(path: String) = ImmutableImage.loader().fromFile(File(path)).toImmutableImage()
