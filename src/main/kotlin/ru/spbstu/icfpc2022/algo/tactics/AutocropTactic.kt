package ru.spbstu.icfpc2022.algo.tactics

import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.color.RGBColor
import com.sksamuel.scrimage.pixels.Pixel
import com.sksamuel.scrimage.pixels.PixelsExtractor
import ru.spbstu.icfpc2022.algo.PersistentState
import ru.spbstu.icfpc2022.algo.Task
import ru.spbstu.icfpc2022.canvas.*
import ru.spbstu.icfpc2022.imageParser.get
import ru.spbstu.icfpc2022.imageParser.subimage
import ru.spbstu.icfpc2022.imageParser.getCanvasColor
import ru.spbstu.icfpc2022.imageParser.toAwt
import ru.spbstu.icfpc2022.move.ColorMove
import ru.spbstu.icfpc2022.move.LineCutMove
import ru.spbstu.icfpc2022.move.Orientation
import ru.spbstu.icfpc2022.move.PointCutMove
import java.awt.Rectangle

class AutocropTactic(task: Task, tacticStorage: TacticStorage) : BlockTactic(task, tacticStorage) {

    companion object {

        /**
         * Returns true if the colors of all pixels in the array are within the given tolerance
         * compared to the referenced color
         */
        private fun approx(color: Color, tolerance: Int, pixels: Array<Pixel>): Boolean {
            val refColor = RGBColor.fromAwt(color.toAwt())
            val minColor = RGBColor(
                (refColor.red - tolerance).coerceAtLeast(0),
                (refColor.green - tolerance).coerceAtLeast(0),
                (refColor.blue - tolerance).coerceAtLeast(0),
                (refColor.alpha - tolerance).coerceAtLeast(0)
            )
            val maxColor = RGBColor(
                (refColor.red + tolerance).coerceAtMost(255),
                (refColor.green + tolerance).coerceAtMost(255),
                (refColor.blue + tolerance).coerceAtMost(255),
                (refColor.alpha + tolerance).coerceAtMost(255),
            )
            return pixels.all { p: Pixel ->
                p.red() in minColor.red..maxColor.red &&
                        p.green() in minColor.green..maxColor.green &&
                        p.blue() in minColor.blue..maxColor.blue &&
                        p.alpha() in minColor.alpha..maxColor.alpha
            }
        }

        /**
         * Returns true if all pixels in the array have the same color, or both are fully transparent.
         */
        private fun uniform(color: Color, pixels: Array<Pixel>): Boolean {
            val target = RGBColor.fromAwt(color.toAwt())
            return pixels.count { p: Pixel -> p.alpha() == 0 && target.alpha == 0 || p.toARGBInt() == target.toARGBInt() } > (pixels.size * 0.8).toLong()
        }

        /**
         * Returns true if the colors of all pixels in the array are within the given tolerance
         * compared to the referenced color.
         *
         *
         * If the given colour and target colour are both fully transparent, then they will match.
         */
        private fun colorMatches(color: Color, tolerance: Int, pixels: Array<Pixel>): Boolean {
            if (tolerance < 0 || tolerance > 255) throw RuntimeException("Tolerance value must be between 0 and 255 inclusive")
            return if (tolerance == 0) uniform(color, pixels) else approx(color, tolerance, pixels)
        }


        /**
         * Starting with the given column index, will return the first column index
         * which contains a colour that does not match the given color.
         */
        private fun scanright(
            color: Color,
            height: Int,
            width: Int,
            col: Int,
            f: PixelsExtractor,
            tolerance: Int
        ): Int {
            return if (col == width || !colorMatches(
                    color,
                    tolerance,
                    f.apply(Rectangle(col, 0, 1, height))
                )
            ) col else scanright(color, height, width, col + 1, f, tolerance)
        }

        private fun scanleft(color: Color, height: Int, col: Int, f: PixelsExtractor, tolerance: Int): Int {
            return if (col == 0 || !colorMatches(
                    color,
                    tolerance,
                    f.apply(Rectangle(col, 0, 1, height))
                )
            ) col else scanleft(color, height, col - 1, f, tolerance)
        }

        private fun scandown(color: Color, height: Int, width: Int, row: Int, f: PixelsExtractor, tolerance: Int): Int {
            return if (row == height || !colorMatches(
                    color,
                    tolerance,
                    f.apply(Rectangle(0, row, width, 1))
                )
            ) row else scandown(color, height, width, row + 1, f, tolerance)
        }

        private fun scanup(color: Color, width: Int, row: Int, f: PixelsExtractor, tolerance: Int): Int {
            return if (row == 0 || !colorMatches(
                    color,
                    tolerance,
                    f.apply(Rectangle(0, row, width, 1))
                )
            ) row else scanup(color, width, row - 1, f, tolerance)
        }

        /**
         * Returns a rectangular region within the given boundaries as a single
         * dimensional array of Pixels.
         *
         *
         * Eg, pixels(10, 10, 30, 20) would result in an array of size 600 with
         * the first row of the region in indexes 0,..,29, second row 30,..,59 etc.
         *
         * @param x the start x coordinate
         * @param y the start y coordinate
         * @param w the width of the region
         * @param h the height of the region
         * @return an Array of pixels for the region
         */
        fun pixels(image: ImmutableImage, x: Int, y: Int, w: Int, h: Int): Array<Pixel> {
            val pixels = arrayOfNulls<Pixel>(w * h)
            var k = 0
            for (y1 in y until y + h) {
                for (x1 in x until x + w) {
                    pixels[k++] = image[x1, y1]
                }
            }
            @Suppress("UNCHECKED_CAST")
            return pixels as Array<Pixel>
        }

        /**
         * Crops an image by removing cols and rows that are composed only of a single
         * given color.
         *
         *
         * Eg, if an image had a 20 pixel row of white at the top, and this method was
         * invoked with Color.WHITE then the image returned would have that 20 pixel row
         * removed.
         *
         *
         * This method is useful when images have an abudance of a single colour around them.
         *
         * @param color          the color to match
         * @param colorTolerance the amount of tolerance to use when determining whether
         * the color matches the reference color [0..255]
         */
        fun autocrop(image: ImmutableImage, color: Color, colorTolerance: Int): Pair<Shape?, ImmutableImage> {
            val x1 =
                scanright(
                    color,
                    image.height,
                    image.width,
                    0,
                    { r -> pixels(image, r.x, r.y, r.width, r.height) },
                    colorTolerance
                )
            val x2 =
                scanleft(
                    color,
                    image.height,
                    image.width - 1,
                    { r -> pixels(image, r.x, r.y, r.width, r.height) },
                    colorTolerance
                )
            val y1 =
                scandown(
                    color,
                    image.height,
                    image.width,
                    0,
                    { r -> pixels(image, r.x, r.y, r.width, r.height) },
                    colorTolerance
                )
            val y2 =
                scanup(
                    color,
                    image.width,
                    image.height - 1,
                    { r -> pixels(image, r.x, r.y, r.width, r.height) },
                    colorTolerance
                )
            return when {
                x1 == 0 && y1 == 0 && x2 == image.width - 1 && y2 == image.height - 1 -> null to image
                x2 <= x1 || y2 <= y1 -> null to image
                else -> Shape(Point(x1, y1), Point(x2, y2)) to image.subimage(
                    x1,
                    y1,
                    x2 - x1,
                    y2 - y1
                )
            }
        }

        private fun computeMaxColor(image: ImmutableImage, start: Point, end: Point): Color {
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

        data class AutocropState(
            val state: PersistentState,
            val image: ImmutableImage,
            val block: BlockId
        )
    }

    override fun invoke(state: PersistentState, blockId: BlockId): PersistentState {
        val cropBlock = state.canvas.blocks[blockId]!!
        var autocropState = AutocropState(
            state,
            task.targetImage.subimage(cropBlock.shape),
            SimpleId(0)
        )

        while (true) {
            val width = 10
            val tolerance = 17
            val shape = autocropState.state.canvas.blocks[autocropState.block]!!.shape
            val variants = mutableListOf(
                Point(0, 0) to Point(shape.width, width),
                Point(0, 0) to Point(width, shape.height),
                Point(0, shape.height - width) to Point(shape.width, shape.height),
                Point(shape.width - width, 0) to Point(shape.width, shape.height),
            )

            val colors = variants.map {
                computeMaxColor(autocropState.image, it.first, it.second)
            }
            val autocrops = colors.map { it to autocrop(autocropState.image, it, tolerance) }
                .filter { it.second.first != null }
                .map { Triple(it.first, it.second.first!!, it.second.second) }

            if (autocrops.isEmpty()) break

            val bestCrop = autocrops.minBy { it.second.size }

            val colorMove = ColorMove(autocropState.block, bestCrop.first)
            var newState = autocropState.state.move(colorMove)

            val newBlockShape = Shape(
                bestCrop.second.lowerLeft.add(shape.lowerLeft),
                bestCrop.second.upperRight.add(shape.lowerLeft),
            )

            var nextBlock = autocropState.block

            if (newBlockShape.lowerLeft != shape.lowerLeft) {
                val firstCrop = when {
                    newBlockShape.lowerLeft.x == shape.lowerLeft.x -> LineCutMove(nextBlock, Orientation.Y, newBlockShape.lowerLeft.y)
                    newBlockShape.lowerLeft.y == shape.lowerLeft.y -> LineCutMove(nextBlock, Orientation.X, newBlockShape.lowerLeft.x)
                    else -> PointCutMove(nextBlock, newBlockShape.lowerLeft)
                }
                newState = newState.move(firstCrop)

                nextBlock = newState.canvas.blocks.toList().first {
                    newBlockShape.middle.isStrictlyInside(
                        it.second.shape.lowerLeft,
                        it.second.shape.upperRight
                    )
                }.first
            }

            if (newBlockShape.upperRight != shape.upperRight) {
                val secondCrop = when {
                    newBlockShape.upperRight.x == shape.upperRight.x -> LineCutMove(nextBlock, Orientation.Y, newBlockShape.upperRight.y)
                    newBlockShape.upperRight.y == shape.upperRight.y -> LineCutMove(nextBlock, Orientation.X, newBlockShape.upperRight.x)
                    else -> PointCutMove(nextBlock, newBlockShape.upperRight)
                }
                newState = newState.move(secondCrop)
                nextBlock = newState.canvas.blocks.toList().first {
                    newBlockShape.middle.isStrictlyInside(
                        it.second.shape.lowerLeft,
                        it.second.shape.upperRight
                    )
                }.first
            }


            autocropState = AutocropState(
                newState,
                bestCrop.third,
                nextBlock,
            )
        }
        return autocropState.state
    }
}
