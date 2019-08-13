package com.ue.wearable_hud.flux.window

import org.hamcrest.CoreMatchers.`is`
import org.junit.Assert.*
import org.junit.Test

class WindowManagerTest {

    class MockConsole: Console {
        val linesPrinted = mutableListOf<String>()
        override fun clearScreen() { linesPrinted.clear() }
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

    @Test
    fun withTwoWindowsResizeWorks() {
        val console = MockConsole()
        val wm = WindowManager(console)
        val window1 = wm.createNewWindow(0, 0, 4, 2)
        val window2 = wm.createNewWindow(0, 1, 4, 0)
        val view1 = BasicTextView()
        val view2 = BasicTextView()
        view1.replaceLines(listOf("abcd", "efgh"))
        view2.replaceLines(listOf("ijkl"))
        wm.displayText(window1.handle, view1)

        assertThat(console.linesPrinted, `is`(listOf("abcd", "efgh")))

        console.clearScreen()

        wm.resizeWindow(window1.handle, height = 1)
        wm.resizeWindow(window2.handle, height = 1)
        wm.displayText(window1.handle, view1)
        wm.displayText(window2.handle, view2)

        assertThat(console.linesPrinted, `is`(listOf("efgh", "ijkl")))
    }
}