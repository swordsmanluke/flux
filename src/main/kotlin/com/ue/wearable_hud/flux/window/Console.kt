package com.ue.wearable_hud.flux.window

import com.googlecode.lanterna.SGR
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.graphics.TextGraphics
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.ue.wearable_hud.flux.console.*
import org.w3c.dom.Text
import java.util.*
import kotlin.collections.HashMap

interface Console {
    fun clearScreen()
    fun printAtXY(x: Int, y: Int, s: String)
}

class VT100Console:Console {
    val ESC = "\u001B"

    override fun clearScreen() {
        println("$ESC[2J")
    }

    override fun printAtXY(x: Int, y: Int, s: String) {
        print("$ESC[${y};${x}H$s")
    }

    // TODO: This doesn't work. Figure out why
    fun showCursor(show: Boolean)  {
        if (show) {
            print("$ESC[25h")
        }
        else {
            print("$ESC[25l")
        }
    }
}

class LanternaConsole:Console {
    private val VT100ToANSI: HashMap<Int, TextColor> = hashMapOf(
            // TODO: Can we represent the light/dark 16 colors, or only 8 colors?

            // **** FOREGROUND
            30 to TextColor.ANSI.BLACK,
            31 to TextColor.ANSI.RED,
            32 to TextColor.ANSI.GREEN,
            33 to TextColor.ANSI.YELLOW,
            34 to TextColor.ANSI.BLUE,
            35 to TextColor.ANSI.MAGENTA,
            36 to TextColor.ANSI.CYAN,
            37 to TextColor.ANSI.WHITE,
            39 to TextColor.ANSI.DEFAULT,

            // "light"
            90 to TextColor.ANSI.BLACK,
            91 to TextColor.ANSI.RED,
            92 to TextColor.ANSI.GREEN,
            93 to TextColor.ANSI.YELLOW,
            94 to TextColor.ANSI.BLUE,
            95 to TextColor.ANSI.MAGENTA,
            96 to TextColor.ANSI.CYAN,
            97 to TextColor.ANSI.WHITE,

            // **** BACKGROUND
            40 to TextColor.ANSI.BLACK,
            41 to TextColor.ANSI.RED,
            42 to TextColor.ANSI.GREEN,
            43 to TextColor.ANSI.YELLOW,
            44 to TextColor.ANSI.BLUE,
            45 to TextColor.ANSI.MAGENTA,
            46 to TextColor.ANSI.CYAN,
            47 to TextColor.ANSI.WHITE,
            49 to TextColor.ANSI.DEFAULT,

            // "light"
            100 to TextColor.ANSI.BLACK,
            101 to TextColor.ANSI.RED,
            102 to TextColor.ANSI.GREEN,
            103 to TextColor.ANSI.YELLOW,
            104 to TextColor.ANSI.BLUE,
            105 to TextColor.ANSI.MAGENTA,
            106 to TextColor.ANSI.CYAN,
            107 to TextColor.ANSI.WHITE)

    val defaultTerminalFactory = DefaultTerminalFactory()
    val screen = defaultTerminalFactory.createScreen()
    val parser = VT100ConsoleParser()

    init {
        screen.startScreen()
    }

    override fun clearScreen() {
        screen.clear()
        screen.refresh()
    }

    override fun printAtXY(x: Int, y: Int, s: String) {
        val tg = screen.newTextGraphics()
        val commands = parser.parse(s)
        var start = x

        for (cmd in commands) {
            when(cmd) {
                is PrintString -> { tg.putString(start, y, cmd.text); start += cmd.text.length }
                is SetTextColor -> { setTextStyle(tg, cmd) }
                else -> {} // TODO: Log the unsupported command?
            }
        }

        screen.refresh()
    }

    private fun setTextStyle(tg: TextGraphics, cmd: SetTextColor) {
        val modifiersToAdd = mutableListOf<SGR>()
        val modifiersToRemove = mutableListOf<SGR>()

        if (cmd.blink != null) if (cmd.blink) modifiersToAdd.add(SGR.BLINK) else modifiersToRemove.add(SGR.BLINK)
        if (cmd.bold != null) if (cmd.bold) modifiersToAdd.add(SGR.BOLD) else modifiersToRemove.add(SGR.BOLD)

        // "Bright" colors are represented by "BOLD" in Lanterna
        if (cmd.background != null) { tg.backgroundColor = getBackgroundColor(cmd); if (cmd.background >= 100) modifiersToAdd.add(SGR.BOLD) }
        if (cmd.foreground != null) { tg.foregroundColor = getForegroundColor(cmd); if (cmd.foreground >= 90)  modifiersToAdd.add(SGR.BOLD) }

        if (modifiersToAdd.size > 0) { tg.setModifiers(EnumSet.copyOf(modifiersToAdd)) }
        if (modifiersToRemove.size > 0) { tg.disableModifiers(*modifiersToRemove.toTypedArray()) }
    }

    private fun getForegroundColor(cmd: SetTextColor): TextColor? {
        return getColor(cmd, cmd.foreground!!)
    }

    private fun getBackgroundColor(cmd: SetTextColor): TextColor? {
        return getColor(cmd, cmd.background!!)
    }

    private fun getColor(cmd: SetTextColor, colorCode: Int): TextColor? {
        if (cmd.colorSet == TextColorMode.COLOR_256) {
            return TextColor.Indexed(colorCode)
        } else {
            return VT100ToANSI[colorCode]
        }
    }
}