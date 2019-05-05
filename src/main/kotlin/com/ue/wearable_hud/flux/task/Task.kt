package com.ue.wearable_hud.flux.task

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

interface Task {
    val id: String
    fun nextRunAt(): Long
    fun readyToSchedule(): Boolean
    fun getLines(): Collection<String>
    suspend fun run(): Collection<String>
}

class StaticTask(override val id: String, var strings: Collection<String>): Task {
    fun update(newStrings: Collection<String>) {
        strings = newStrings
    }

    override fun nextRunAt(): Long = Long.MAX_VALUE
    override fun readyToSchedule(): Boolean = false // Never updates
    override fun getLines(): Collection<String> = strings
    override suspend fun run(): Collection<String> = strings
}

class UnixTask(override val id: String, val workingDir: File, val command: String, val refreshPeriodSec: Int) : Task {
    private var lines: Collection<String> = emptyList()
    private var lastRun = 0L

    override fun nextRunAt(): Long {
        return lastRun + refreshPeriodSec * 1000
    }

    override fun readyToSchedule(): Boolean {
        return System.currentTimeMillis() > nextRunAt()
    }

    override fun getLines(): Collection<String> {
        return lines
    }

    override suspend fun run(): Collection<String> {
        try {
            val parts = command.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            proc.waitFor(60, TimeUnit.MINUTES)
            val text = proc.inputStream.bufferedReader().readText()
            lastRun = System.currentTimeMillis()
            var lines = text.split("\n")

            if (lines.last() == "") {
                lines = lines.slice(0 until (lines.count() - 1))
            }

            this.lines = lines
            return lines
        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        }
    }
}

class NullTask : Task {
    override val id = "nulltask"
    override fun nextRunAt(): Long = Long.MAX_VALUE // Never needs to be scheduled
    override fun readyToSchedule(): Boolean = false // Never needs to be run
    override fun getLines(): Collection<String> = emptyList()
    override suspend fun run(): Collection<String> = emptyList()
}
