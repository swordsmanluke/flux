package com.ue.wearable_hud.flux

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

fun main(args: Array<String>) {
    val curDir = File(".")

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

fun runCommand(workingDir: File, command: String): String? {
    try {
        val parts = command.split("\\s".toRegex())
        val proc = ProcessBuilder(*parts.toTypedArray())
                .directory(workingDir)
                .redirectOutput(ProcessBuilder.Redirect.PIPE)
                .redirectError(ProcessBuilder.Redirect.PIPE)
                .start()

        proc.waitFor(60, TimeUnit.MINUTES)
        return proc.inputStream.bufferedReader().readText()
    } catch(e: IOException) {
        e.printStackTrace()
        return null
    }
}

fun runCommandOut(workingDir: File, command: String) {
    val parts = command.split("\\s".toRegex())
    ProcessBuilder(*parts.toTypedArray())
            .directory(workingDir)
            .redirectOutput(ProcessBuilder.Redirect.INHERIT)
            .redirectError(ProcessBuilder.Redirect.INHERIT)
            .start()
            .waitFor(60, TimeUnit.MINUTES)
}