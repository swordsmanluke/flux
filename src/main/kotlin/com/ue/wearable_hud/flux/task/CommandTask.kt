package com.ue.wearable_hud.flux.task

class CommandTask(override val id: String, var subTask: Task=NullTask()): Task {
    private var lines = emptyList<String>()

    override fun nextRunAt() = System.currentTimeMillis() + 1000
    override fun readyToSchedule() = true
    override fun getLines(): Collection<String> = lines

    override suspend fun run(): Collection<String> {
        return lines
    }

    override fun sendCommand(command: List<String>): Collection<String> {
        lines = subTask.sendCommand(command).toList()
        return lines
    }
}