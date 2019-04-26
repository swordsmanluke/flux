package com.ue.wearable_hud.flux

import com.ue.wearable_hud.flux.task.Task
import com.ue.wearable_hud.flux.window.BasicTextView
import com.ue.wearable_hud.flux.window.ScrollDirection
import com.ue.wearable_hud.flux.window.ScrollableTextView
import com.ue.wearable_hud.flux.window.WindowManager
import java.io.File

fun main(args: Array<String>) {
    val wm = WindowManager()
    val w1 = wm.createNewWindow(1, 1, 8, 10)

    val curDir = File("/home/lucas/Software/dark_goggles-0.1/bin")
    val command = "./dark_goggles daily"
    val darkGoggles = Task(curDir, command)
    darkGoggles.run()
    val tv =  ScrollableTextView(20, 10)
    tv.replaceLines(darkGoggles.getLines())

    wm.displayText(w1, tv)

    (1..1).forEach {
        tv.scroll(ScrollDirection.RIGHT, 3)

        wm.displayText(w1, tv)
        Thread.sleep(250)
    }

    (1..1).forEach{
        tv.scroll(ScrollDirection.LEFT, 3)

        wm.displayText(w1, tv)
        Thread.sleep(250)
    }

    val (lines, columns) = getTerminalDimensions()
//    println("Lines   = $lines")
//    println("Columns = $columns")
}

private fun getTerminalDimensions(): Pair<Int, Int> {
    val lineStr = System.getenv("LINES") ?: "80"
    val colStr = System.getenv("COLUMNS") ?: "240"
    return Pair(lineStr.toInt(), colStr.toInt())
}

fun gotoxy(x: Int, y: Int) = print("\u001B[$x;${y}H")