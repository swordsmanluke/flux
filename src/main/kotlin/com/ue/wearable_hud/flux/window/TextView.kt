package com.ue.wearable_hud.flux.window

import com.ue.wearable_hud.flux.extensions.visibleCharSlice

interface TextView {
    var storedLines: MutableList<String>

    fun replaceLines(lines: Collection<String>) { this.storedLines = mutableListOf(*lines.toTypedArray()) }
    fun addLines(lines: Collection<String>) { this.storedLines.addAll(lines) }
    fun getLines() : List<String>
}

class BasicTextView : TextView{
    override var storedLines: MutableList<String> = mutableListOf()
    override fun getLines(): List<String> = storedLines
}

enum class ScrollDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

class ScrollableTextView(val width: Int, val height: Int):TextView {
    var offsetX = 0
    var offsetY = 0

    override var storedLines: MutableList<String> = mutableListOf()

    fun scroll(direction: ScrollDirection, amount: Int = 1) {
        when(direction) {
            ScrollDirection.UP -> offsetY -= amount
            ScrollDirection.DOWN -> offsetY += amount
            ScrollDirection.LEFT -> offsetX -= amount
            ScrollDirection.RIGHT -> offsetX += amount
        }

        val maxLength = storedLines.maxBy { it.length }?.length ?: 0

        offsetX = stayWithinBounds(offsetX, 0, Math.max(0, maxLength - width))
        offsetY = stayWithinBounds(offsetY, 0, Math.max(0, storedLines.size - height))
    }

    private fun stayWithinBounds(value: Int, min: Int, max: Int): Int {
        return if (value < min)  { min }
        else   if (value >= max) { max }
        else                     { value }
    }

    override fun getLines(): List<String> {
        val lastStringIndex = stayWithinBounds(offsetY + height, 0, storedLines.size)
        val maxLength = storedLines.maxBy { it.length }?.length ?: 0
        val lastWidthPos = stayWithinBounds(offsetX + width, 0, maxLength)
        return storedLines.
                slice(offsetY until lastStringIndex).
                map{ it.visibleCharSlice(offsetX until lastWidthPos) }
    }

}
