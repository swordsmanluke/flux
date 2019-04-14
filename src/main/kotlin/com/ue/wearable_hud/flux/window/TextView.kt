package com.ue.wearable_hud.flux.window

import com.ue.wearable_hud.flux.extensions.safeSlice

interface TextView {
    var lines: MutableList<String>

    fun replaceLines(lines: Collection<String>) { this.lines = mutableListOf(*lines.toTypedArray()) }
    fun addLines(lines: Collection<String>) { this.lines.addAll(lines) }
    fun getLines() : Collection<String>
}

enum class ScrollDirection {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

class ScollableTextView(val width: Int, val height: Int):TextView {
    var offsetX = 0
    var offsetY = 0

    override var lines: MutableList<String> = mutableListOf()

    fun scroll(direction: ScrollDirection, amount: Int = 1) {
        when(direction) {
            ScrollDirection.UP -> offsetY -= amount
            ScrollDirection.DOWN -> offsetY += amount
            ScrollDirection.LEFT -> offsetX -= amount
            ScrollDirection.RIGHT -> offsetX += amount
        }

        val maxLength = lines.maxBy { it.length }?.length ?: 0

        offsetX = stayWithinBounds(offsetX, 0, Math.max(0, maxLength - width))
        offsetY = stayWithinBounds(offsetY, 0, Math.max(0, lines.size - height))
    }

    private fun stayWithinBounds(value: Int, min: Int, max: Int): Int {
        return if (value < min)  { min }
        else   if (value >= max) { max }
        else                     { value }
    }

    override fun getLines(): Collection<String> {
        val lastStringIndex = stayWithinBounds(offsetY + height, 0, lines.size)
        val maxLength = lines.maxBy { it.length }?.length ?: 0
        val lastWidthPos = stayWithinBounds(offsetX + width, 0, maxLength)
        return lines.
                slice(offsetY until lastStringIndex).
                map{ it.safeSlice(offsetX until (offsetX + width)) }
    }

}
