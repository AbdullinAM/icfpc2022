package ru.spbstu.icfpc2022.robovinchi

import javafx.stage.Stage
import ru.spbstu.icfpc2022.shutdownClient
import tornadofx.*

class Robovinchi: App(RobovinchiView::class) {
    override fun start(stage: Stage) {
        super.start(stage)
        stage.width = 1000.0
        stage.height = 600.0
    }

    override fun stop() {
        super.stop()
        //shutdownClient()
    }
}
