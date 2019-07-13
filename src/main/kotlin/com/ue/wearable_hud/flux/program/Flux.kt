package com.ue.wearable_hud.flux.program

import com.ue.wearable_hud.flux.task.NullTask
import com.ue.wearable_hud.flux.task.StaticTask
import com.ue.wearable_hud.flux.window.*
import kotlinx.coroutines.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}
class Flux(val context: FluxConfiguration) {

    val errTask = StaticTask("errors", emptyList()) // Task for displaying errors

    init {
        context.tasks.add(errTask)
    }

    suspend fun run() {
        context.windowManager.console.clearScreen()

        coroutineScope{
            val j1 = async { runRefreshTaskLoop() }
            val j2 = async { runRefreshUiLoop() }

            awaitAll(j1, j2)
        }
    }

    @UseExperimental(ObsoleteCoroutinesApi::class)
    private suspend fun runRefreshTaskLoop() {
        logger.info("=== Beginning Refresh Task Loop ===")
        logger.info("Refreshing ${context.tasks.count()} tasks")
        do {
            try {
                coroutineScope {
                    context.tasks.filter { it.readyToSchedule() }.forEach { launch(newSingleThreadContext(it.id)) { it.run() } }
                }
            } catch (e: Exception) {
                logger.error(e) { "Unhandled exception running a task!" }
                displayErrorInMain(e)
            }
            sleepTilNextTask()
        }while (true)
    }

    private suspend fun runRefreshUiLoop() {
        logger.info("=== Beginning Refresh UI Loop ===")
        logger.info("Refreshing ${context.windowManager.windows.count()} windows")
        do {
            try {
                context.windowManager.windows.forEach { window ->
                    val task =  context.taskForWindow[window] ?: NullTask()
                    val view =  context.viewForWindow[window] ?: NullView()
                    view.replaceLines(task.getLines())
                    context.windowManager.displayText(window.handle, view)
                }
            } catch (e: Exception) {
                logger.error(e) { "Unhandled exception updating UI!" }
                displayErrorInMain(e)
            }
            delay(10) // Sleep .1 second between UI refreshes
        } while (true)
    }

    private suspend fun sleepTilNextTask() {
        val now = System.currentTimeMillis()
        val nextRun =  context.tasks.map { it.nextRunAt() }.min() ?: now

        val delta = Math.max(1000, nextRun - System.currentTimeMillis())  // Wait at least 1 second between refreshes
        delay(delta)
    }

    private fun displayErrorInMain(e: Exception) {
        val errors = listOf(e.message ?: "") + e.stackTrace.map { it?.toString() ?: "" }
        // Truncate the stack trace to fit in the window. Extra lines will push down the output
        val lastErrLine = Math.min(errors.count(), context.windowManager.mainWindow.height)
        errTask.update(errors.slice(0..lastErrLine))
         context.taskForWindow[context.windowManager.mainWindow] = errTask
    }

}