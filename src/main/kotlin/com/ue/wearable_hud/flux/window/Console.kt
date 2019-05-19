package com.ue.wearable_hud.flux.window

import com.googlecode.lanterna.TextCharacter
import com.googlecode.lanterna.terminal.DefaultTerminalFactory



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
    val defaultTerminalFactory = DefaultTerminalFactory()
    val screen = defaultTerminalFactory.createScreen()

    init {
        screen.startScreen()
    }

    override fun clearScreen() {
        screen.clear()
        screen.refresh()
    }

    override fun printAtXY(x: Int, y: Int, s: String) {
        // TODO: Doesn't work with control characters, e.g. VT100 codes
        val tg = screen.newTextGraphics()
        tg.putString(x, y, s)
        screen.refresh()
    }
}