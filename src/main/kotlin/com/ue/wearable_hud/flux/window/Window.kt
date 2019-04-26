package com.ue.wearable_hud.flux.window

data class Window(val handle: Int, val x: Int, val y: Int, val width: Int, val height: Int) {
    val minX: Int get() = x
    val minY: Int get() = y
    val maxX: Int get() = x + width
    val maxY: Int get() = y + height

    fun overlaps(otherWindow: Window): Boolean {
        // If one rectangle is on left side of other
        if (minX > otherWindow.maxX || otherWindow.minX > maxX) {
            return false
        }

        // If one rectangle is above other
        if (minY < otherWindow.maxY || otherWindow.minY < maxY) {
            return false
        }

        return true
    }
}
