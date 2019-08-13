package com.ue.wearable_hud.flux

import com.ue.wearable_hud.flux.program.Flux
import com.ue.wearable_hud.flux.program.FluxConfiguration
import com.ue.wearable_hud.flux.task.NullTask
import com.ue.wearable_hud.flux.task.StaticTask
import com.ue.wearable_hud.flux.task.Task
import com.ue.wearable_hud.flux.task.UnixTask
import com.ue.wearable_hud.flux.terminal.Terminal
import com.ue.wearable_hud.flux.window.*
import java.io.File
import kotlinx.coroutines.*
import mu.KotlinLogging
import net.consensys.cava.toml.Toml
import net.consensys.cava.toml.TomlTable
import java.lang.IllegalArgumentException
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}
fun main(args: Array<String>) {
    // Raspi view size: 84 col x 22 lines
    val console = LanternaConsole()
    val wm = WindowManager(console)

    val config = loadConfiguration(wm)
    val terminal = Terminal({ s -> config.getTaskById(s) } ) // lambda to wrap the function refererence in a closure
    val prog = Flux(config, terminal)

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            logger.info("Shutdown")
            println("\nShutdown")
        }
    })

    try {
        runBlocking { prog.run() }
        logger.info("Shutdown")
        println("\nShutdown")
    } catch (e: Exception) {
        logger.error(e) { "FATAL: An unhandled exception has killed Flux!" }
    }
}

private fun loadConfiguration(wm: WindowManager): FluxConfiguration {
    val viewForWindow = mutableMapOf<Int, TextView>()
    val taskForWindow = mutableMapOf<Int, Task>()

    val taskConfig = Toml.parse(Paths.get("config/tasks.toml"))
    taskConfig.errors().forEach { error -> System.err.println(error.toString()) }
    val taskDescriptions = taskConfig.getArray("tasks")

    val taskCount = taskDescriptions?.size() ?: 0
    val tasks = (0 until taskCount).map { i ->
        createTask(taskDescriptions?.getTable(i))
    }.toMutableList()

    val windowDescriptions = taskConfig.getArray("windows")
    val windowCount = windowDescriptions?.size() ?: 0
    (0 until windowCount).forEach { i ->
        val windowConfig = windowDescriptions?.getTable(i)
        val (window, task, view) = constructWindow(wm, windowConfig, tasks)
        viewForWindow[window.handle] = view
        taskForWindow[window.handle] = task
        logger.info("Associating task ${task.id} -> window ${window.handle}")
    }

    val tw = wm.terminalWindow
    val termTask = StaticTask("terminal")
    tasks.add(termTask)
    taskForWindow[tw.handle] = termTask

    return FluxConfiguration(wm, viewForWindow, taskForWindow, tasks)
}

private fun constructWindow(wm: WindowManager, windowConfig: TomlTable?, tasks: List<Task>): Triple<Window, Task, TextView> {
    val window = createWindow(wm, windowConfig)
    val taskId = windowConfig?.getString("task_id") ?: ""
    val task = tasks.find { it.id == taskId } ?: NullTask()
    val view = ScrollableTextView(window.width, window.height)
    return Triple(window, task, view)
}


fun createWindow(wm: WindowManager, windowConfig: TomlTable?): Window {
    if (windowConfig == null) return NullWindow()
    return wm.createNewWindow(
            windowConfig.getLong("x")?.toInt()!!,
            windowConfig.getLong("y")?.toInt()!!,
            windowConfig.getLong("width")?.toInt()!!,
            windowConfig.getLong("height")?.toInt()!!
    )
}

private fun createTask(task: TomlTable?): Task {
    if (task == null) return NullTask()

    val id = task.getString("id")!!
    val name = task.getString("name")
    val description = task.getString("description")
    val path = task.getString("path")!!
    val command = task.getString("command")!!
    val period = task.getString("period")!!
    return UnixTask(id, File(path), command, parsePeriod(period))
}

fun parsePeriod(periodString: String): Int {
    val match = Regex("(\\d+)(\\w)?").find(periodString) ?: throw IllegalArgumentException("Invalid period definition '$periodString'")
    val time = match.groups[1]?.value?.toInt() ?: throw IllegalArgumentException("Invalid period definition '$periodString'")
    val units = match.groups[2]?.value ?: 's'
    return when(units) {
        "s" -> time
        "m" -> time * 60
        "h" -> time * 60 * 60
        "d" -> time * 60 * 60 * 24 // Probably don't need this, but what the hell.
        else -> throw IllegalArgumentException("Invalid period definition '$periodString'")
    }
}

private fun getTerminalDimensions(): Pair<Int, Int> {
    val lineStr = System.getenv("LINES") ?: "80"
    val colStr = System.getenv("COLUMNS") ?: "240"
    return Pair(lineStr.toInt(), colStr.toInt())
}