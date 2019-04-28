package com.ue.wearable_hud.flux.program

import com.ue.wearable_hud.flux.task.NullTask
import com.ue.wearable_hud.flux.task.StaticTask
import com.ue.wearable_hud.flux.task.Task
import com.ue.wearable_hud.flux.window.*
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay

class Flux(val wm: WindowManager,
           val viewForWindow: MutableMap<Window, TextView>,
           val taskForWindow: MutableMap<Window, Task>,
           val tasks: MutableList<Task>,
           val mainWindowId: Int) {

    val errTask = StaticTask(emptyList()) // Task for displaying errors
    init {
        tasks.add(errTask)
    }


    suspend fun run() {
        wm.console.clearScreen()

        coroutineScope{
            val j1 = async { runRefreshTaskLoop() }
            val j2 = async { runRefreshUiLoop() }

            awaitAll(j1, j2)
        }
    }

    private suspend fun runRefreshTaskLoop() {
        do {
            try {
                coroutineScope {
                    tasks.filter { it.readyToSchedule() }.forEach { async { it.run() } }
                }
            } catch (e: Exception) {
                displayErrorInMain(e)
            }
            sleepTilNextTask()
        }while (true)
    }

    private suspend fun runRefreshUiLoop() {
        do {
            try {
                wm.windows.forEach { window ->
                    val task = taskForWindow[window] ?: NullTask()
                    val view = viewForWindow[window] ?: NullView()
                    view.replaceLines(task.getLines())
                    wm.displayText(window.handle, view)
                }
            } catch (e: Exception) {
                displayErrorInMain(e)
            }
            delay(1000) // Sleep 1 second between UI refreshes
        } while (true)
    }

    private suspend fun sleepTilNextTask() {
        val now = System.currentTimeMillis()
        val nextRun = tasks.map { it.nextRunAt() }.min() ?: now

        val delta = Math.max(1000, nextRun - System.currentTimeMillis())  // Wait at least 1 second between refreshes
        delay(delta)
    }

    private fun displayErrorInMain(e: Exception) {
        val mainWindow = wm.windows.find { it.handle == mainWindowId } ?: NullWindow()
        val errors = listOf(e.message ?: "") + e.stackTrace.map { stringifyStack(it) }
        // Truncate the stack trace to fit in the window. Extra lines will push down the output
        val lastErrLine = Math.min(errors.count(), mainWindow.height)
        errTask.update(errors.slice(0..lastErrLine))
        taskForWindow[mainWindow] = errTask
    }

    private fun stringifyStack(el: StackTraceElement?): String {
        if(el == null) { return "" }

        return el.toString()
    }
}