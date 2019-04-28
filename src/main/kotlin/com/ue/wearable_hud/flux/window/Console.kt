package com.ue.wearable_hud.flux.window

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