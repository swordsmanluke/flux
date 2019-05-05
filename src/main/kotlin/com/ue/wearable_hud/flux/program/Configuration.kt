package com.ue.wearable_hud.flux.program

import com.ue.wearable_hud.flux.task.Task
import com.ue.wearable_hud.flux.window.TextView
import com.ue.wearable_hud.flux.window.Window
import com.ue.wearable_hud.flux.window.WindowManager

class FluxConfiguration(
        val windowManager: WindowManager,
        val viewForWindow: Map<Window, TextView>,
        val taskForWindow: MutableMap<Window, Task>,
        val tasks: MutableCollection<Task>) {

}