package com.ue.wearable_hud.flux.window

import com.ue.wearable_hud.flux.extensions.visibleCharPadEnd
import com.ue.wearable_hud.flux.extensions.visibleCharSlice

class WindowManager {
    private var nextHandle = 1
    private val _windows = mutableListOf<Window>()
    private val viewLastUpdated = mutableMapOf<TextView, Long>()

    fun createNewWindow(xPos: Int, yPos: Int, width: Int, height: Int): Window {
        val w = Window(nextHandle++, xPos, yPos, width, height)
        validateDoesNotOverlap(w)
        // TODO: Bounds check against terminal size
        _windows.add(w)
        return w
    }

    val windows : List<Window>
        get() = listOf(*_windows.toTypedArray())

    private fun validateDoesNotOverlap(w: Window) {
        // TODO: Probably want to get rid of this constraint.
        // TODO: It could make sense for us to have modal windows, for instance.
        if (_windows.any { it.overlaps(w) }) {
            throw IllegalArgumentException("Window $w would overlap existing windows!")
        }
    }

    fun displayText(handle: Int, textView: TextView) {
        val lastUpdated = viewLastUpdated[textView] ?: 0L
        if (lastUpdated < textView.lastUpdated) {
            viewLastUpdated[textView] = textView.lastUpdated

            val window = _windows.find { it.handle == handle } ?: return
            val formattedLines = formatText(textView, window)
            printFormattedLines(formattedLines, window)
        }
    }

    private fun printFormattedLines(finalLinesToPrint: List<String>, window: Window) {
        finalLinesToPrint.forEachIndexed { i, line ->
            printAtXY(window.x, window.y + i, line)
        }
    }

    private fun formatText(text: TextView, window: Window): List<String> {
        val lines = text.getLines()
        val lastStringIndex = Math.min(lines.size, window.height)
        val printableLines = lines.
                slice(0 until lastStringIndex).  //  Get the max number of lines that can be displayed in this window
                map {
                    val slice = it.visibleCharSlice(0 until window.width)   // slice the line until it fits in the width
                    val padded = slice.visibleCharPadEnd(window.width, ' ')  // but pad it with spaces to clear out any old text
                    padded
                }

        val numBlankLines = window.height - printableLines.size
        val finalLinesToPrint = printableLines + (0..numBlankLines).map { " ".repeat(window.width) }
        return finalLinesToPrint
    }

    private fun printAtXY(x: Int, y: Int, s: String) = print("\u001B[${y};${x}H$s")
}
