package ru.spbstu.icfpc2022.robovinchi

import javafx.beans.property.SimpleIntegerProperty
import javafx.beans.property.SimpleStringProperty
import javafx.scene.Node
import javafx.scene.control.ScrollBar
import javafx.scene.image.Image
import javafx.scene.paint.Color
import javafx.scene.paint.Paint
import javafx.scene.shape.Circle
import javafx.scene.shape.Rectangle
import javafx.scene.text.Text
import tornadofx.*
import java.io.File

class RobovinchiView : View() {

    var curStep = SimpleIntegerProperty(0)
    var curCommand = SimpleStringProperty(StateCollector.commandToCanvas[curStep.value].first.toString())
    var nextCommand = SimpleStringProperty(StateCollector.commandToCanvas[curStep.value + 1].first.toString())
    var curRectangles = mutableListOf<Rectangle>()
    val snapPoints = StateCollector.task.snapPoints.flatMap { it.value.values }.map {
        val x = it.x.toDouble()
        val y = StateCollector.canvasHeight - it.y.toDouble()
        Circle(x, y, 0.5, Color(0.0, 1.0, 1.0, 0.5)).attachTo(this)
    }.takeIf { it.size < 20000 }.orEmpty()

    private fun getRectanglesFromBlocks(): List<Rectangle> {
        val blocks = StateCollector.commandToCanvas.getOrNull(curStep.value) ?: return curRectangles
        val rectangles = mutableListOf<Rectangle>()
        for (block in blocks.second) {
            val xCoor = block.shape.upperLeft.x.toDouble()
            //val yCoor = block.shape.lowerRight.y.toDouble()
            val yCoor = StateCollector.canvasHeight - block.shape.upperRight.y.toDouble()
            val width = block.shape.width.toDouble()
            val height = block.shape.height.toDouble()
            val rectangle = Rectangle(xCoor, yCoor, width, height).apply {
                fill = Color(
                    block.color.r.toDouble() / 255,
                    block.color.g.toDouble() / 255,
                    block.color.b.toDouble() / 255,
                    block.color.a.toDouble() / 255
                )
                stroke = Color.RED
            }
            rectangles.add(rectangle)
        }
        return rectangles
    }

    override val root = pane {
        val globalPane = this
        globalPane.apply {
            snapPoints.forEach { it.attachTo(globalPane) }
        }

        val curStepPane = pane {
            text(curStep.toString()).attachTo(this)
            layoutX = 0.0
            layoutY = 500.0
        }
        pane {
            imageview(Image(File(StateCollector.pathToProblemImage).toURI().toString()))
            layoutX = 500.0
            layoutY = 0.0
        }

        pane {
            text(curCommand).attachTo(this)
            layoutX = 0.0
            layoutY = 525.0
        }

        pane {
            text(nextCommand).attachTo(this)
            layoutX = 0.0
            layoutY = 550.0
        }
        pane {
            button("Next") {
                action {
                    curStep.set(curStep.value + 1)
                    curStepPane.apply {
                        clear()
                        text(curStep.toString()).attachTo(this)
                        layoutX = 0.0
                        layoutY = 500.0
                    }
                    if (curStep >= StateCollector.commandToCanvas.size) return@action
                    curRectangles.forEach { globalPane.children.remove(it) }
                    val rects = getRectanglesFromBlocks()
                    rects.forEach { rect ->
                        rect.attachTo(globalPane).also { curRectangles.add(it) }
                    }
                    snapPoints.forEach { it.removeFromParent() }
                    snapPoints.forEach { it.attachTo(globalPane) }
                    curCommand.set(nextCommand.value)
                    val newNextCommand =
                        StateCollector.commandToCanvas.getOrNull(curStep.value + 1)?.first?.toString() ?: "END"
                    nextCommand.set(newNextCommand)
                }
            }
            layoutX = 500.0
            layoutY = 500.0
        }
        pane {
            button("Prev") {
                action {
                    curStep.set(curStep.value - 1)
                    curStepPane.apply {
                        clear()
                        text(curStep.toString()).attachTo(this)
                        layoutX = 0.0
                        layoutY = 500.0
                    }
                    if (curStep >= StateCollector.commandToCanvas.size) return@action
                    curRectangles.forEach { globalPane.children.remove(it) }
                    val rects = getRectanglesFromBlocks()
                    rects.forEach { rect ->
                        rect.attachTo(globalPane).also { curRectangles.add(it) }
                    }
                    snapPoints.forEach { it.removeFromParent() }
                    snapPoints.forEach { it.attachTo(globalPane) }
                    curCommand.set(nextCommand.value)
                    val newNextCommand =
                        StateCollector.commandToCanvas.getOrNull(curStep.value + 1)?.first?.toString() ?: "END"
                    nextCommand.set(newNextCommand)
                }
            }
            layoutX = 500.0
            layoutY = 535.0
        }
        scrollpane {
            ScrollBar().attachTo(this).apply {
                min = 0.0
                max = StateCollector.commandToCanvas.size.toDouble()
                valueProperty().onChange {
                    curStep.set(it.toInt())
                    curStepPane.apply {
                        clear()
                        text(curStep.toString()).attachTo(this)
                        layoutX = 0.0
                        layoutY = 500.0
                    }
                    if (curStep >= StateCollector.commandToCanvas.size - 2) return@onChange
                    curRectangles.forEach { globalPane.children.remove(it) }
                    val rects = getRectanglesFromBlocks()
                    if (rects.isNotEmpty()) {
                        rects.forEach { rect ->
                            rect.attachTo(globalPane).also { curRectangles.add(it) }
                        }
                    }
                    snapPoints.forEach { it.removeFromParent() }
                    snapPoints.forEach { it.attachTo(globalPane) }
                    curCommand.set(nextCommand.value)
                    val newNextCommand =
                        StateCollector.commandToCanvas.getOrNull(curStep.value + 1)?.first?.toString() ?: "END"
                    nextCommand.set(newNextCommand)
                }
            }
            layoutX = 450.0
            layoutY = 450.0
        }
    }
}


class Block(val rectangle: Rectangle, id: String, val blockId: BlockId = BlockId(rectangle, id))

class BlockId(
    rectangle: Rectangle,
    text: String,
) {
    private val coorX: Double = rectangle.x + rectangle.width / 2 - 8
    private val coorY: Double = rectangle.y + rectangle.height / 2
    val asText = Text(coorX, coorY, text)
}
