package com.ue.wearable_hud.flux.window

import com.ue.wearable_hud.flux.extensions.visibleCharSlice

class WindowManager {
    private var nextHandle = 1
    private val windows = mutableListOf<Window>()

    fun createNewWindow(xPos: Int, yPos: Int, width: Int, height: Int): Int {
        val w = Window(nextHandle++, xPos, yPos, width, height)
        validateDoesNotOverlap(w)
        // TODO: Bounds check against terminal size
        windows.add(w)
        return w.handle
    }

    private fun validateDoesNotOverlap(w: Window) {
        if (windows.any { it.overlaps(w) }) {
            throw IllegalArgumentException("Window $w would overlap existing windows!")
        }
    }

    fun displayText(handle: Int, textView: TextView) {
        val window = windows.find { it.handle == handle } ?: return
        val formattedLines = formatText(textView, window)
        printFormattedLines(formattedLines, window)
    }

    private fun printFormattedLines(finalLinesToPrint: List<String>, window: Window) {
        finalLinesToPrint.forEachIndexed { i, line ->
            printAtXY(window.y, window.x + i, line)
        }
    }

    private fun formatText(text: TextView, window: Window): List<String> {
        val lines = text.getLines()
        val lastStringIndex = Math.min(lines.size, window.height)
        val printableLines = lines.
                slice(0 until lastStringIndex).  //  Get the max number of lines that can be displayed in this window
                map { it.visibleCharSlice(0 until window.width).   // slice the line until it fits in the width
                        padEnd(window.width, ' ') }        // but pad it with spaces to clear out any old text

        val numBlankLines = window.height - printableLines.size
        val finalLinesToPrint = printableLines + (0..numBlankLines).map { " ".repeat(window.width) }
        return finalLinesToPrint
    }

    private fun printAtXY(x: Int, y: Int, s: String) = println("\u001B[$x;${y}H$s")
}
