package com.ue.wearable_hud.flux.console

open class ConsoleCommand {}

enum class TextColorMode {
    COLOR_16,
    COLOR_256
}

data class SetTextColor(val dim: Boolean? = null,
                        val bold: Boolean? = null,
                        val blink: Boolean? = null,
                        val hidden: Boolean? = null,
                        val colorSet: TextColorMode = TextColorMode.COLOR_16,
                        val foreground: Int? = null,
                        val background: Int? = null): ConsoleCommand()

data class PrintString(val text: String): ConsoleCommand()

// TODO: data class GoTo(val x: Int, val y: Int): ConsoleCommand()