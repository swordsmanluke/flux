package com.ue.wearable_hud.flux

import com.ue.wearable_hud.flux.program.Flux
import com.ue.wearable_hud.flux.task.NullTask
import com.ue.wearable_hud.flux.task.Task
import com.ue.wearable_hud.flux.task.UnixTask
import com.ue.wearable_hud.flux.window.*
import java.io.File

fun main(args: Array<String>) {
    // Raspi view size: 84 col x 22 lines
    val wm = WindowManager()
    val viewForWindow = mutableMapOf<Int, TextView>()
    val taskForWindow = mutableMapOf<Int, Task>()
    val tasks = mutableListOf<Task>()

    val dailyForecastWindow = wm.createNewWindow(1, 1, 16, 1)
    val hourlyForecastWindow = wm.createNewWindow(1, 2, 16, 1)

    viewForWindow[dailyForecastWindow] = ScrollableTextView(16, 1)
    viewForWindow[hourlyForecastWindow] = ScrollableTextView(16, 1)

    val dailyForecast = dailyForecastTask()
    tasks.add(dailyForecast)

    val hourlyForecast = hourlyForecastTask()
    tasks.add(hourlyForecast)

    taskForWindow[dailyForecastWindow] = dailyForecast
    taskForWindow[hourlyForecastWindow] = hourlyForecast

    val prog = Flux(wm, viewForWindow, taskForWindow, tasks)
    prog.run()

    val (lines, columns) = getTerminalDimensions()
//    println("Lines   = $lines")
//    println("Columns = $columns")
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

private fun forecastTask(type: ForecastType): Task {
    val curDir = File("/home/lucas/Software/dark_goggles-0.1/bin")
    val command = "./dark_goggles ${type.toString().toLowerCase()}"
    val darkGoggles = UnixTask(curDir, command)
    return darkGoggles
}

private fun getTerminalDimensions(): Pair<Int, Int> {
    val lineStr = System.getenv("LINES") ?: "80"
    val colStr = System.getenv("COLUMNS") ?: "240"
    return Pair(lineStr.toInt(), colStr.toInt())
}

fun gotoxy(x: Int, y: Int) = print("\u001B[$x;${y}H")