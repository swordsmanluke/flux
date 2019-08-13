package com.ue.wearable_hud.flux.program

import com.googlecode.lanterna.input.KeyType
import com.ue.wearable_hud.flux.task.StaticTask
import com.ue.wearable_hud.flux.terminal.KeyModifier
import com.ue.wearable_hud.flux.terminal.Terminal
import com.ue.wearable_hud.flux.terminal.TerminalKey
import com.ue.wearable_hud.flux.window.*
import kotlinx.coroutines.*
import mu.KotlinLogging
import java.util.concurrent.Executors
import kotlin.math.max
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger {}

class Flux(val context: FluxConfiguration, val terminal: Terminal) {

    val taskRunnerCtx = Executors.newFixedThreadPool(5).asCoroutineDispatcher()
    val errTask = StaticTask("errors", emptyList()) // Task for displaying errors
    var shouldExit = false
    var mainWindowBottom = context.windowManager.mainWindow.maxY

    init {
        context.addTask(errTask)
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
                } else {
                    val c = if (input.isShiftDown) input.character?.toUpperCase() else input.character

                    val modifiers = mutableListOf<KeyModifier>()

                    if (input.isAltDown) modifiers.add(KeyModifier.ALT)
                    if (input.isCtrlDown) modifiers.add(KeyModifier.CTRL)

                    val key = when (input.keyType) {
                        KeyType.Escape -> TerminalKey.ESC
                        KeyType.Delete -> TerminalKey.DELETE
                        KeyType.Backspace -> TerminalKey.BACKSPACE
                        KeyType.Enter -> TerminalKey.ENTER
//                        KeyType.ArrowLeft -> TODO()
//                        KeyType.ArrowRight -> TODO()
//                        KeyType.ArrowUp -> TODO()
//                        KeyType.ArrowDown -> TODO()
//                        KeyType.Home -> TODO()
//                        KeyType.End -> TODO()
//                        KeyType.Tab -> TODO()
//                        KeyType.ReverseTab -> TODO()
                        else -> null
                    }

                    terminal.sendCharacter(c, key, modifiers)
                    adjustBottomOfMainWindow()

                    val terminalTask = context.getTask(context.windowManager.terminalWindow) as StaticTask
                    val terminalView = context.getView(context.windowManager.terminalWindow)

                    if (terminal.commandString.isNotEmpty()) {
                        val formattedCommand = "> ${terminal.commandString}"
                        terminalTask.update(listOf(formattedCommand))
                    } else {
                        terminalTask.update(listOf())
                    }

                    terminalView.replaceLines(terminalTask.getLines())
                    refreshUI()
                }
            } catch (e: Exception) {
                logger.error("Unhandled error reading keyboard input", e)
            }
        } while (true)
    }

    private fun adjustBottomOfMainWindow() {
        val mainWindow = context.windowManager.mainWindow
        val terminalWindow = context.windowManager.terminalWindow
        val mainWindowTooSmall = mainWindow.maxY < mainWindowBottom
        val mainWindowFullSize = !mainWindowTooSmall
        val commandPresent = terminal.commandString.isNotEmpty()

        when (commandPresent) {
            true  -> if(mainWindowFullSize) {
                logger.info("Detected new command - shrinking main window")
                context.windowManager.resizeWindow(mainWindow.handle, height=mainWindow.height - 1)
                context.windowManager.resizeWindow(terminalWindow.handle, x=mainWindow.x, y=mainWindow.maxY - 1, width=mainWindow.width, height=1)
                refreshUI()
            }
            false -> if(mainWindowTooSmall) {
                logger.info("Detected command cleared - restoring main window")
                context.windowManager.resizeWindow(mainWindow.handle, height=mainWindow.height + 1)
                context.windowManager.resizeWindow(terminalWindow.handle, x=0, y=0, width=0, height=0)
                refreshUI()
            }
        }
    }

    private suspend fun runRefreshTaskLoop() {
        logger.info("=== Beginning Refresh Task Loop ===")
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
                refreshUI()
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

    private fun refreshUI() {
        try {
            context.windowManager.windows.forEach { window ->
                val task = context.getTask(window)
                val view = context.getView(window)
                view.replaceLines(task.getLines())
                context.windowManager.displayText(window.handle, view)
            }
        } catch (e: Exception) {
            logger.error(e) { "Unhandled exception updating UI!" }
            displayErrorInMain(e)
        }
    }

    private suspend fun sleepTilNextTask() {
        val now = System.currentTimeMillis()
        val nextRun = context.tasks.map { it.nextRunAt() }.min() ?: now

        val delta = max(100, nextRun - System.currentTimeMillis())  // Wait at least .1 second between refreshes
        delay(delta)
    }

    private fun displayErrorInMain(e: Exception) {
        val errors = listOf(e.message ?: "") + e.stackTrace.map { it?.toString() ?: "" }
        // Truncate the stack trace to fit in the window. Extra lines will push down the output
        val lastErrLine = Math.min(errors.count(), context.windowManager.mainWindow.height)
        errTask.update(errors.slice(0..lastErrLine))
        context.setTask(context.windowManager.mainWindow, errTask)
    }
}