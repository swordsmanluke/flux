package com.ue.wearable_hud.flux.program

import com.ue.wearable_hud.flux.task.NullTask
import com.ue.wearable_hud.flux.task.Task
import com.ue.wearable_hud.flux.window.*

class FluxConfiguration(
        val windowManager: WindowManager,
        private val viewForWindow: MutableMap<Int, TextView>,
        private val taskForWindow: MutableMap<Int, Task>,
        val tasks: MutableCollection<Task>) {

    val activeTask: Task
        get() = getTask(windowManager.mainWindow)

    fun addTask(t: Task) {
        tasks.add(t)
    }

    fun getView(w: Window) : TextView {
        if (!viewForWindow.containsKey(w.handle)) {
            viewForWindow[w.handle] = BasicTextView()
        }
        return viewForWindow[w.handle] ?: NullView()
    }

    fun getTask(w: Window) : Task {
        if (!taskForWindow.containsKey(w.handle)) {
            taskForWindow[w.handle] = NullTask()
        }

        return taskForWindow[w.handle] ?: NullTask()
    }

    fun setTask(window: Window, task: Task) {
        taskForWindow[window.handle] = task
    }

    fun getTaskById(taskId: String): Task {
        val taskIds = tasks.map { it.id }

        return if (taskIds.contains(taskId)) {
            tasks.first { it.id == taskId }
        } else {
            activeTask
        }
    }
}
