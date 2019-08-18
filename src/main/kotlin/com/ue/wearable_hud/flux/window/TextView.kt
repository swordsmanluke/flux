package com.ue.wearable_hud.flux.window

import com.ue.wearable_hud.flux.extensions.visibleCharSlice
import kotlin.math.max

interface TextView {
    var storedLines: MutableList<String>
    var lastUpdated: Long

    fun replaceLines(lines: Collection<String>) {
        if (linesDiffer(storedLines, lines)) {
            lastUpdated = System.currentTimeMillis()
        }
        this.storedLines = mutableListOf(*lines.toTypedArray())
    }

    private fun linesDiffer(lines1: Collection<String>, lines2: Collection<String>): Boolean {
        if (lines1.count() != lines2.count()) { return true }
        lines1.zip(lines2).forEach { (i, j) ->
            if (i != j) { return true }
        }
        return false
    }

    fun addLines(lines: Collection<String>) { this.storedLines.addAll(lines); lastUpdated = System.currentTimeMillis() }
    fun getLines() : List<String>
}

class BasicTextView : TextView{
    private var _lastUpdated = 0L
    override var lastUpdated: Long
        get() = _lastUpdated
        set(value) { _lastUpdated = value }

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
    private var _lastUpdated = 0L
    override var lastUpdated: Long
        get() = _lastUpdated
        set(value) { _lastUpdated = value }

    fun scroll(direction: ScrollDirection, amount: Int = 1) {
        when(direction) {
            ScrollDirection.UP -> offsetY -= amount
            ScrollDirection.DOWN -> offsetY += amount
            ScrollDirection.LEFT -> offsetX -= amount
            ScrollDirection.RIGHT -> offsetX += amount
        }

        val maxLength = storedLines.maxBy { it.length }?.length ?: 0

        offsetX = stayWithinBounds(offsetX, 0, max(0, maxLength - width))
        offsetY = stayWithinBounds(offsetY, 0, max(0, storedLines.size - height))
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

class NullView: TextView {
    override var lastUpdated: Long
        get() = System.currentTimeMillis()
        set(_) { }

    override var storedLines: MutableList<String>
        get() = mutableListOf()
        set(_) {}

    override fun getLines(): List<String> = storedLines
}
