package com.ue.wearable_hud.flux.window

import com.ue.wearable_hud.flux.extensions.visibleCharPadEnd
import com.ue.wearable_hud.flux.extensions.visibleCharSlice
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class WindowManager(val console: Console) {

    private var nextHandle = 1
    private val _windows = mutableListOf<Window>()
    private val viewLastUpdated = mutableMapOf<TextView, Long>()
    private var maxWindow: Window? = null
    val mainWindow: Window
        get() {
            if (maxWindow == null) {
                maxWindow = _windows.maxBy { it.width * it.height }
            }
            return maxWindow ?: NullWindow()
        }

    fun createNewWindow(xPos: Int, yPos: Int, width: Int, height: Int): Window {
        val w = Window(nextHandle++, xPos, yPos, width, height)

        logger.info("Creating window #$nextHandle [$width X $height] @ ($xPos,$yPos)")

        validateDoesNotOverlap(w)

        // TODO: Bounds check against terminal size
        _windows.add(w)
        maxWindow = null // Clear max until we need it again
        return w
    }

    val windows : List<Window>
        get() = listOf(*_windows.toTypedArray())

    private fun validateDoesNotOverlap(w: Window) {
        // TODO: Probably want to get rid of this constraint.
        // TODO: It could make sense for us to have modal windows, for instance.
        if (_windows.any { it.overlaps(w) }) {
            logger.warn("Window $w would overlap existing windows!")
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
        logger.info("Printing ${finalLinesToPrint.size} lines to window ${window.handle}")
        finalLinesToPrint.forEachIndexed { i, line ->
            console.printAtXY(window.x, window.y + i, line)
        }
    }

    private fun formatText(text: TextView, window: Window): List<String> {
        val lines = text.getLines()
        val firstStringIndex = Math.max(0, lines.size - window.height)
        val printableLines = lines.
                slice(firstStringIndex until lines.size).  //  Get the max number of lines that can be displayed in this window
                map {
                    val slice = it.visibleCharSlice(0 until window.width)   // slice the line until it fits in the width
                    val padded = slice.visibleCharPadEnd(window.width, ' ')  // but pad it with spaces to clear out any old text
                    padded
                }

        val numBlankLines = window.height - printableLines.size

        return printableLines + (0 until numBlankLines).map { " ".repeat(window.width) }
    }
}
