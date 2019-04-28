package com.ue.wearable_hud.flux.window

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Test

class WindowManagerTest {

    class MockConsole: Console {
        val linesPrinted = mutableListOf<String>()
        override fun clearScreen() {}
        override fun printAtXY(x: Int, y: Int, s: String) { linesPrinted.add(s) }
    }

    @Test
    fun truncatesLinesToSizeOfWindow() {
        val console = MockConsole()
        val wm = WindowManager(console)
        val height = 2
        val window = wm.createNewWindow(0, 0, 4, height)
        val view = BasicTextView()

        view.replaceLines(listOf("abcd", "efgh", "ijkl"))
        wm.displayText(window.handle, view)

        assertThat(console.linesPrinted.count(), `is`(height))
    }

    @Test
    fun showsLastLinesInView() {
        val console = MockConsole()
        val wm = WindowManager(console)
        val window = wm.createNewWindow(0, 0, 4, 2)
        val view = BasicTextView()
        view.replaceLines(listOf("abcd", "efgh", "ijkl"))
        wm.displayText(window.handle, view)
        assertThat(console.linesPrinted, `is`(listOf("efgh", "ijkl")))
    }

}