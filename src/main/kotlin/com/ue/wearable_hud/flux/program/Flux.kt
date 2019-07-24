package com.ue.wearable_hud.flux.program

import com.ue.wearable_hud.flux.task.NullTask
import com.ue.wearable_hud.flux.task.StaticTask
import com.ue.wearable_hud.flux.window.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.Executors
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

class Flux(val context: FluxConfiguration) {

    val taskRunnerCtx = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
    val errTask = StaticTask("errors", emptyList()) // Task for displaying errors
    var shouldExit = false

    init {
        context.tasks.add(errTask)
    }

    suspend fun run() {
        context.windowManager.console.clearScreen()

        coroutineScope {
            val jobs = mutableListOf(
                    async { runRefreshTaskLoop() },
                    async { runRefreshUiLoop() }
            )

            if (context.windowManager.console is LanternaConsole) {
                jobs.add(
                        async { runInputLoop(context.windowManager.console) }
                )
            }

            awaitAll(*jobs.toTypedArray())
        }
    }

    private suspend fun runInputLoop(console: LanternaConsole) {
        do {
            try {
                delay(100)
                val input = console.screen.terminal.pollInput() ?: continue

                if (input.isCtrlDown && input.character.toLowerCase() == 'c') {
                    logger.info { "Caught CTRL-C. Attempting shutdown" }
                    shouldExit = true
                    break
                }
            } catch (e: Exception) {
                logger.error("Unhandled error reading keyboard input", e)
            }
        } while (true)
    }

    private suspend fun runRefreshTaskLoop() {
        logger.info("=== Beginning Refresh Task Loop ===")
        logger.info("Refreshing ${context.tasks.count()} tasks")
        do {
            try {
                runScheduledTasks()
            } catch (e: Exception) {
                logger.error(e) { "Unhandled exception running a task!" }
                displayErrorInMain(e)
            }
            sleepTilNextTask()
            if (shouldExit) {
                break
            }
        } while (true)
    }

    private suspend fun runScheduledTasks() {
        withContext(taskRunnerCtx) {
            for (task in context.tasks) {
                if (!task.readyToSchedule()) continue
                launch {
                    val elapsed = measureTimeMillis { task.run() }
                    logger.info { "Task ${task.id} ran in ${elapsed / 1000.0}s" }
                }
            }
        }
    }

    private suspend fun runRefreshUiLoop() {
        logger.info("=== Beginning Refresh UI Loop ===")
        logger.info("Refreshing ${context.windowManager.windows.count()} windows")
        val loopDelayMillis = 1000L
        do {
            val elapsed = measureTimeMillis {
                try {
                    context.windowManager.windows.forEach { window ->
                        val task = context.taskForWindow[window] ?: NullTask()
                        val view = context.viewForWindow[window] ?: NullView()
                        view.replaceLines(task.getLines())
                        context.windowManager.displayText(window.handle, view)
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Unhandled exception updating UI!" }
                    displayErrorInMain(e)
                }
                delay(loopDelayMillis) // Sleep between UI refreshes
            }

            if (elapsed > 2 * loopDelayMillis) {
                logger.warn { "UI loop update took ${(elapsed / 1000.0)}s" }
            }

            if (shouldExit) {
                break
            }
        } while (true)
    }

    private suspend fun sleepTilNextTask() {
        val now = System.currentTimeMillis()
        val nextRun = context.tasks.map { it.nextRunAt() }.min() ?: now

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