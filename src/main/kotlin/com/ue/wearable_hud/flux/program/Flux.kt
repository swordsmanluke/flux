package com.ue.wearable_hud.flux.program

import com.ue.wearable_hud.flux.task.NullTask
import com.ue.wearable_hud.flux.task.Task
import com.ue.wearable_hud.flux.window.*

class Flux(val wm: WindowManager,
           val viewForWindow: MutableMap<Int, TextView>,
           val taskForWindow: MutableMap<Int, Task>,
           val tasks: MutableList<Task>) {

    fun run() {
        tasks.forEach { it.run() }
        refreshUi()

        val dailyForecastView = viewForWindow[wm.windows.first().handle] as ScrollableTextView

        (1..3).forEach {
            dailyForecastView.scroll(ScrollDirection.RIGHT, 4)
            refreshUi()
            Thread.sleep(500)
        }

        (1..3).forEach{
            dailyForecastView.scroll(ScrollDirection.LEFT, 4)
            refreshUi()
            Thread.sleep(500)
        }
    }

    private fun refreshUi() {
        wm.windows.forEach { window ->
            val handle = window.handle
            val task = taskForWindow.get(handle) ?: NullTask()
            val view = viewForWindow[handle] ?: NullView()
            view.replaceLines(task.getLines())
            wm.displayText(handle, view)
        }
    }
}