package com.ue.wearable_hud.flux

import com.ue.wearable_hud.flux.task.Task
import java.io.File

fun main(args: Array<String>) {
    val curDir = File("/home/lucas/Software/dark_goggles-0.1/bin")
    val command = "./dark_goggles daily"
    val darkGoggles = Task(curDir, command)

    println(darkGoggles.run())

    val (lines, columns) = getTerminalDimensions()
    println("Lines   = $lines")
    println("Columns = $columns")
}

private fun getTerminalDimensions(): Pair<Int, Int> {
    val lineStr = System.getenv("LINES") ?: "80"
    val colStr = System.getenv("COLUMNS") ?: "240"
    return Pair(lineStr.toInt(), colStr.toInt())
}

fun gotoxy(x: Int, y: Int) = print("\u001B[$x;${y}H")