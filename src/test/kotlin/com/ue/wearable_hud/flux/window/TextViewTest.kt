package com.ue.wearable_hud.flux.window

import org.junit.Assert.*
import org.junit.Test

class ScrollableTextViewTest {
    @Test
    fun appendingLinesWorks() {
        val stv = ScollableTextView(5, 5)
        stv.addLines(arrayListOf("abcd1", "abcd2"))
        stv.addLines(arrayListOf("abcd3"))
        val boundedLines = stv.getLines()
        assertEquals(3, boundedLines.size)
        arrayListOf("abcd1", "abcd2", "abcd3").forEach { assertTrue(boundedLines.contains(it)) }
    }

    @Test
    fun replacingingLinesWorks() {
        val stv = ScollableTextView(5, 5)
        stv.addLines(arrayListOf("abcd1", "abcd2"))
        stv.replaceLines(arrayListOf("abcd3"))
        val boundedLines = stv.getLines()
        assertEquals(1, boundedLines.size)
        arrayListOf("abcd3").forEach { assertTrue(boundedLines.contains(it)) }
    }

    //**********
    // Scroll vertically
    //**********

    @Test
    fun textViewBoundsHeightToExpectedBound() {
        val stv = ScollableTextView(5, 5)
        stv.addLines(arrayListOf("abcd1", "abcd2", "abcd3", "abcd4", "abcd5", "abcd6"))
        val boundedLines = stv.getLines()
        assertEquals(5, boundedLines.size)
        assertFalse(boundedLines.contains("abcd6"))
        arrayListOf("abcd1", "abcd2", "abcd3", "abcd4", "abcd5").forEach { assertTrue(boundedLines.contains(it)) }
    }

    @Test
    fun textViewScrollsHeight() {
        val stv = ScollableTextView(5, 5)
        stv.addLines(arrayListOf("abcd1", "abcd2", "abcd3", "abcd4", "abcd5", "abcd6"))
        stv.scroll(ScrollDirection.DOWN)
        val boundedLines = stv.getLines()
        assertEquals(5, boundedLines.size)
        assertFalse(boundedLines.contains("abcd1"))
        arrayListOf("abcd2", "abcd3", "abcd4", "abcd5", "abcd6").forEach { assertTrue(boundedLines.contains(it)) }
    }

    @Test
    fun textViewScrollsHeightToEndOfBound() {
        val stv = ScollableTextView(5, 5)
        stv.addLines(arrayListOf("abcd1", "abcd2", "abcd3", "abcd4", "abcd5", "abcd6", "abcd7"))
        stv.scroll(ScrollDirection.DOWN, 20)
        val boundedLines = stv.getLines()
        assertEquals(5, boundedLines.size)
        assertFalse(boundedLines.contains("abcd1"))
        assertFalse(boundedLines.contains("abcd2"))
        arrayListOf("abcd3", "abcd4", "abcd5", "abcd6", "abcd7").forEach { assertTrue(boundedLines.contains(it)) }
    }

    @Test
    fun textViewDoesNotScrollsHeightWhenCannotScroll() {
        val stv = ScollableTextView(5, 5)
        stv.addLines(arrayListOf("abcd1", "abcd2", "abcd3"))
        stv.scroll(ScrollDirection.DOWN, 20)
        val boundedLines = stv.getLines()
        assertEquals(3, boundedLines.size)
        arrayListOf("abcd1", "abcd2", "abcd3").forEach { assertTrue(boundedLines.contains(it)) }
    }

    @Test
    fun textViewScrollsBackUp() {
        val stv = ScollableTextView(5, 2)
        stv.addLines(arrayListOf("abcd1", "abcd2", "abcd3"))
        stv.scroll(ScrollDirection.DOWN)
        stv.scroll(ScrollDirection.UP, 20)
        val boundedLines = stv.getLines()
        assertEquals(2, boundedLines.size)
        arrayListOf("abcd1", "abcd2").forEach { assertTrue(boundedLines.contains(it)) }
    }

    //**********
    // Scroll horizontally
    //**********

    @Test
    fun textViewBoundsWidthToExpectedBound() {
        val stv = ScollableTextView(5, 3)
        stv.addLines(arrayListOf("abcd1:abc", "abcd2:abc", "abcd3:abc"))
        val boundedLines = stv.getLines()
        assertEquals(3, boundedLines.size)
        arrayListOf("abcd1", "abcd2", "abcd3").forEach { assertTrue(boundedLines.contains(it)) }
    }

    @Test
    fun textViewScrollsWidth() {
        val stv = ScollableTextView(5, 3)
        stv.addLines(arrayListOf("abcd1:abc", "abcd2:abc", "abcd3:abc"))
        stv.scroll(ScrollDirection.RIGHT)
        val boundedLines = stv.getLines()
        assertEquals(3, boundedLines.size)
        arrayListOf("bcd1:", "bcd2:", "bcd3:").forEach { assertTrue(boundedLines.contains(it)) }
    }

    @Test
    fun textViewScrollsWidthToEndOfBound() {
        val stv = ScollableTextView(5, 3)
        stv.addLines(arrayListOf("abcd1:abc", "abcd2:abc", "abcd3:abc"))
        stv.scroll(ScrollDirection.RIGHT, 20)
        val boundedLines = stv.getLines()
        arrayListOf("1:abc", "2:abc", "3:abc").forEach { assertTrue(boundedLines.contains(it)) }
    }

    @Test
    fun textViewDoesNotScrollsWidthWhenCannotScroll() {
        val stv = ScollableTextView(5, 5)
        stv.addLines(arrayListOf("abcd1", "abcd2", "abcd3"))
        stv.scroll(ScrollDirection.RIGHT, 20)
        val boundedLines = stv.getLines()
        assertEquals(3, boundedLines.size)
        arrayListOf("abcd1", "abcd2", "abcd3").forEach { assertTrue(boundedLines.contains(it)) }
    }

    @Test
    fun textViewScrollsWidthPastEndOfShortStrings() {
        val stv = ScollableTextView(5, 5)
        stv.addLines(arrayListOf("a", "abcd1:", "abcd2:abc"))
        stv.scroll(ScrollDirection.RIGHT, 20)
        val boundedLines = stv.getLines()
        assertEquals(3, boundedLines.size)
        arrayListOf("", "1:", "2:abc").forEach { assertTrue(boundedLines.contains(it)) }
    }

    @Test
    fun textViewScrollsLeft() {
        val stv = ScollableTextView(5, 5)
        stv.addLines(arrayListOf("a", "abcd1:", "abcd2:abc"))
        stv.scroll(ScrollDirection.RIGHT, 2)
        stv.scroll(ScrollDirection.LEFT, 20)
        val boundedLines = stv.getLines()
        assertEquals(3, boundedLines.size)
        arrayListOf("a", "abcd1", "abcd2").forEach { assertTrue(boundedLines.contains(it)) }
    }
}