package com.ue.wearable_hud.flux.program

import com.ue.wearable_hud.flux.task.NullTask
import com.ue.wearable_hud.flux.task.StaticTask
import com.ue.wearable_hud.flux.task.Task
import com.ue.wearable_hud.flux.window.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class Flux(val wm: WindowManager,
           val viewForWindow: MutableMap<Window, TextView>,
           val taskForWindow: MutableMap<Window, Task>,
           val tasks: MutableList<Task>,
           val mainWindowId: Int) {

    val errTask = StaticTask(emptyList()) // Task for displaying errors
    val ESC = "\u001B" // Used in our printer codes
    init {
        tasks.add(errTask)
    }


    suspend fun run() {
        // Clear the screen
        println("$ESC[2J")

        do {
            try {
                runLoopIteration()
            } catch (e: Exception) {
                // Show exception in main window
                displayErrorInMain(e)
            }
        } while (true)
    }

    private suspend fun runLoopIteration() {
        refreshTasks()
        refreshUi()
        sleepTilNextTask()
    }

    private fun displayErrorInMain(e: Exception) {
        val mainWindow = wm.windows.find { it.handle == mainWindowId } ?: NullWindow()
        errTask.update(listOf(e.message ?: "") + e.stackTrace.map { stringifyStack(it) })
        taskForWindow[mainWindow] = errTask
    }

    private fun stringifyStack(el: StackTraceElement?): String {
        if(el == null) { return "" }

        return el.toString()
    }

    private fun sleepTilNextTask() {
        val now = System.currentTimeMillis()
        val nextRun = tasks.map { it.nextRunAt() }.min() ?: now

        val delta = Math.max(1000, nextRun - System.currentTimeMillis())  // Wait at least 1 second between refreshes
        Thread.sleep(delta)
    }

    private suspend fun refreshTasks() {
        coroutineScope {
            tasks.filter { it.readyToSchedule() }.forEach { async { it.run() } }
        }
    }

    private fun refreshUi() {
        wm.windows.forEach { window ->
            val task = taskForWindow[window] ?: NullTask()
            val view = viewForWindow[window] ?: NullView()
            view.replaceLines(task.getLines())
            wm.displayText(window.handle, view)
        }
    }

    fun showCursor(show: Boolean)  {
        if (show) {
            print("$ESC[25h")
        }
        else {
            print("$ESC[25l")
        }
    }
}