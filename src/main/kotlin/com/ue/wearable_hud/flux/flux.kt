package com.ue.wearable_hud.flux

import com.ue.wearable_hud.flux.program.Flux
import com.ue.wearable_hud.flux.task.Task
import com.ue.wearable_hud.flux.task.UnixTask
import com.ue.wearable_hud.flux.window.*
import java.io.File
import kotlinx.coroutines.*

fun main(args: Array<String>) {
    // Raspi view size: 84 col x 22 lines
    val console = VT100Console()
    val wm = WindowManager(console)

    val viewForWindow = mutableMapOf<Window, TextView>()
    val taskForWindow = mutableMapOf<Window, Task>()
    val tasks = mutableListOf<Task>()

    // Top bar
    var topX = 1

    val dailyForecastWindow = wm.createNewWindow(topX, 1, 16, 1)
    topX += dailyForecastWindow.width + 4 // Padding
    val timeWindow = wm.createNewWindow(topX, 1, 28, 1)
    topX += timeWindow.width + 4 // Padding
    val hourlyForecastWindow = wm.createNewWindow(topX, 1, 16, 1)

    val mainWindow = wm.createNewWindow(3,2,81,15)

    viewForWindow[dailyForecastWindow] = ScrollableTextView(16, 1)
    viewForWindow[hourlyForecastWindow] = ScrollableTextView(16, 1)
    viewForWindow[timeWindow] = BasicTextView()
    viewForWindow[mainWindow] = ScrollableTextView(81, 17)

    val dailyForecast = dailyForecastTask()
    tasks.add(dailyForecast)

    val hourlyForecast = hourlyForecastTask()
    tasks.add(hourlyForecast)

    val time = datetimeTask()
    tasks.add(time)

    val todoList = todoListTask()
    tasks.add(todoList)

    taskForWindow[dailyForecastWindow] = dailyForecast
    taskForWindow[hourlyForecastWindow] = hourlyForecast
    taskForWindow[timeWindow] = time
    taskForWindow[mainWindow] = todoList

    val prog = Flux(wm, viewForWindow, taskForWindow, tasks, mainWindow.handle)

    Runtime.getRuntime().addShutdownHook(object : Thread() {
        override fun run() {
            println("\nShutdown")
            // TODO: Add shutdown code here
        }
    })

    runBlocking { prog.run() }
}

private fun datetimeTask(): Task {
    val curDir = File("./")
    val command = "date"
    val time = UnixTask(curDir, command, 1)
    return time
}

private fun dailyForecastTask(): Task {
    return forecastTask(ForecastType.DAILY)
}

private fun hourlyForecastTask(): Task {
    return forecastTask(ForecastType.HOURLY)
}

enum class ForecastType {
    HOURLY,
    DAILY
}

private val TEN_MINUTES = 600

private fun forecastTask(type: ForecastType): Task {
    val curDir = File("/home/lucas/Software/dark_goggles-0.1/bin")
    val command = "./dark_goggles ${type.toString().toLowerCase()}"
    val forecast = UnixTask(curDir, command, TEN_MINUTES)
    return forecast
}

private fun todoListTask(): Task {
    val curDir = File("/home/lucas/workspace/wearable_hud/hud-todo-list")
    val command = "./dist/__main__/todolist"
    val todoList = UnixTask(curDir, command, TEN_MINUTES)
    return todoList
}

private fun getTerminalDimensions(): Pair<Int, Int> {
    val lineStr = System.getenv("LINES") ?: "80"
    val colStr = System.getenv("COLUMNS") ?: "240"
    return Pair(lineStr.toInt(), colStr.toInt())
}