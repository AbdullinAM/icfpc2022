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
import ru.spbstu.icfpc2022.imageParser.toAwt
import ru.spbstu.icfpc2022.move.LineCutMove
import ru.spbstu.icfpc2022.move.Orientation
import ru.spbstu.icfpc2022.move.PointCutMove
import java.awt.Rectangle

class AutocropTactic(
    task: Task,
    val tacticStorage: TacticStorage,
    val colorTolerance: Int,
    val pixelTolerance: Double
) : BlockTactic(task, tacticStorage) {
    var leftBlocks = mutableSetOf<BlockId>()

    val backgroundTactic: ColorBackgroundTactic? get() = tacticStorage.get()

    companion object {

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

        /**
         * Returns true if the colors of all pixels in the array are within the given tolerance
         * compared to the referenced color
         */
        public fun approx(color: Color, tolerance: Int, pixels: Array<Pixel>, pt: Double): Boolean {
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
            return pixels.count { p: Pixel ->
                p.red() in minColor.red..maxColor.red &&
                        p.green() in minColor.green..maxColor.green &&
                        p.blue() in minColor.blue..maxColor.blue &&
                        p.alpha() in minColor.alpha..maxColor.alpha
            } > (pixels.size * pt)
        }

        /**
         * Returns true if all pixels in the array have the same color, or both are fully transparent.
         */
        private fun uniform(color: Color, pixels: Array<Pixel>, pt: Double): Boolean {
            val target = RGBColor.fromAwt(color.toAwt())
            return pixels.count { p: Pixel -> p.alpha() == 0 && target.alpha == 0 || p.toARGBInt() == target.toARGBInt() } > (pixels.size * pt).toLong()
        }

        /**
         * Returns true if the colors of all pixels in the array are within the given tolerance
         * compared to the referenced color.
         *
         *
         * If the given colour and target colour are both fully transparent, then they will match.
         */
        private fun colorMatches(color: Color, tolerance: Int, pixels: Array<Pixel>, pt: Double): Boolean {
            if (tolerance < 0 || tolerance > 255) throw RuntimeException("Tolerance value must be between 0 and 255 inclusive")
            return if (tolerance == 0) uniform(color, pixels, pt) else approx(color, tolerance, pixels, pt)
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
            tolerance: Int,
            pt: Double
        ): Int {
            return if (col == width || !colorMatches(
                    color,
                    tolerance,
                    f.apply(Rectangle(col, 0, 1, height)),
                    pt
                )
            ) col else scanright(color, height, width, col + 1, f, tolerance, pt)
        }

        private fun scanleft(
            color: Color,
            height: Int,
            col: Int,
            f: PixelsExtractor,
            tolerance: Int,
            pt: Double
        ): Int {
            return if (col == 0 || !colorMatches(
                    color,
                    tolerance,
                    f.apply(Rectangle(col, 0, 1, height)),
                    pt
                )
            ) col else scanleft(color, height, col - 1, f, tolerance, pt)
        }

        private fun scandown(
            color: Color,
            height: Int,
            width: Int,
            row: Int,
            f: PixelsExtractor,
            tolerance: Int,
            pt: Double
        ): Int {
            return if (row == height || !colorMatches(
                    color,
                    tolerance,
                    f.apply(Rectangle(0, row, width, 1)),
                    pt
                )
            ) row else scandown(color, height, width, row + 1, f, tolerance, pt)
        }

        private fun scanup(
            color: Color,
            width: Int,
            row: Int,
            f: PixelsExtractor,
            tolerance: Int,
            pt: Double
        ): Int {
            return if (row == 0 || !colorMatches(
                    color,
                    tolerance,
                    f.apply(Rectangle(0, row, width, 1)),
                    pt
                )
            ) row else scanup(color, width, row - 1, f, tolerance, pt)
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
        fun autocrop(
            image: ImmutableImage,
            color: Color,
            colorTolerance: Int,
            pixelTolerance: Double
        ): Pair<Shape?, ImmutableImage> {
            val x1 =
                scanright(
                    color,
                    image.height,
                    image.width,
                    0,
                    { r -> pixels(image, r.x, r.y, r.width, r.height) },
                    colorTolerance,
                    pixelTolerance
                )
            val x2 =
                scanleft(
                    color,
                    image.height,
                    image.width - 1,
                    { r -> pixels(image, r.x, r.y, r.width, r.height) },
                    colorTolerance,
                    pixelTolerance
                )
            val y1 =
                scandown(
                    color,
                    image.height,
                    image.width,
                    0,
                    { r -> pixels(image, r.x, r.y, r.width, r.height) },
                    colorTolerance,
                    pixelTolerance
                )
            val y2 =
                scanup(
                    color,
                    image.width,
                    image.height - 1,
                    { r -> pixels(image, r.x, r.y, r.width, r.height) },
                    colorTolerance,
                    pixelTolerance
                )
            return when {
                x1 == 0 && y1 == 0 && x2 == image.width - 1 && y2 == image.height - 1 -> null to image
                x2 <= x1 || y2 <= y1 -> null to image
                else -> Shape(Point(x1, y1), Point(x2 + 1, y2 + 1)) to image.subimage(
                    x1,
                    y1,
                    x2 - x1,
                    y2 - y1
                )
            }
        }

        data class AutocropState(
            val state: PersistentState,
            val image: ImmutableImage,
            val block: BlockId
        )
    }

    fun Point.fitInto(shape: Shape) = Point(
        x.coerceAtLeast(0).coerceAtMost(shape.width - 1),
        y.coerceAtLeast(0).coerceAtMost(shape.height - 1),
    )

    override fun invoke(state: PersistentState, blockId: BlockId): PersistentState {
        val cropBlock = state.canvas.blocks[blockId]!!
        var autocropState = AutocropState(
            state,
            task.targetImage.subimage(cropBlock.shape),
            blockId
        )

        while (true) {
            val width = 10
            val tolerance = colorTolerance
            val shape = autocropState.state.canvas.blocks[autocropState.block]!!.shape
            val variants = mutableListOf(
                Point(0, 0) to Point(shape.width, width),
                Point(0, 0) to Point(width, shape.height),
                Point(0, shape.height - width) to Point(shape.width, shape.height),
                Point(shape.width - width, 0) to Point(shape.width, shape.height),
            ).map { (p1, p2) -> p1.fitInto(shape) to p2.fitInto(shape) }

            val colors = variants.map {
                computeBlockMax(autocropState.image, it.first, it.second)
            }
            val autocrops = colors.map { it to autocrop(autocropState.image, it, tolerance, pixelTolerance) }
                .filter { it.second.first != null }
                .map { Triple(it.first, it.second.first!!, it.second.second) }

            if (autocrops.isEmpty()) break

            val bestCrop = autocrops.minBy { it.second.size }

            val newBlockShape = Shape(
                bestCrop.second.lowerLeftInclusive.add(shape.lowerLeftInclusive),
                bestCrop.second.upperRightExclusive.add(shape.lowerLeftInclusive),
            )

            val bestCropAverage = computeNotBlockGeometricMedianApproximated(autocropState.image, newBlockShape)

            var newState = colorBlock(autocropState.state, autocropState.block, bestCropAverage, colorTolerance)

            var nextBlock = autocropState.block

            if (newBlockShape.lowerLeftInclusive != shape.lowerLeftInclusive) {
                val firstCrop = when {
                    newBlockShape.lowerLeftInclusive.x == shape.lowerLeftInclusive.x -> LineCutMove(
                        nextBlock,
                        Orientation.Y,
                        newBlockShape.lowerLeftInclusive.y
                    )

                    newBlockShape.lowerLeftInclusive.y == shape.lowerLeftInclusive.y -> LineCutMove(
                        nextBlock,
                        Orientation.X,
                        newBlockShape.lowerLeftInclusive.x
                    )

                    else -> PointCutMove(nextBlock, newBlockShape.lowerLeftInclusive)
                }
                newState = newState.move(firstCrop)

                nextBlock = newState.canvas.blocks.toList().firstOrNull {
                    newBlockShape.middle.isStrictlyInside(
                        it.second.shape.lowerLeftInclusive,
                        it.second.shape.upperRightExclusive
                    )
                }?.first.also {
                    if (it == null) System.err.println("cut failed")
                } ?: break
            }

            if (newBlockShape.upperRightExclusive != shape.upperRightExclusive) {
                val secondCrop = when {
                    newBlockShape.upperRightExclusive.x == shape.upperRightExclusive.x -> LineCutMove(
                        nextBlock,
                        Orientation.Y,
                        newBlockShape.upperRightExclusive.y - 1
                    )

                    newBlockShape.upperRightExclusive.y == shape.upperRightExclusive.y -> LineCutMove(
                        nextBlock,
                        Orientation.X,
                        newBlockShape.upperRightExclusive.x - 1
                    )

                    else -> PointCutMove(nextBlock, newBlockShape.upperRightExclusive - Point(1, 1))
                }
                newState = newState.move(secondCrop)
                nextBlock = newState.canvas.blocks.toList().firstOrNull {
                    newBlockShape.middle.isStrictlyInside(
                        it.second.shape.lowerLeftInclusive,
                        it.second.shape.upperRightExclusive
                    )
                }?.first.also {
                    if (it == null) System.err.println("cut failed")
                } ?: break
            }


            autocropState = AutocropState(
                newState,
                bestCrop.third,
                nextBlock,
            )
        }
        leftBlocks.add(autocropState.block)
        return autocropState.state
    }
}
