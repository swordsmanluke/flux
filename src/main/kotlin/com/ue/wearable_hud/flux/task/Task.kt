package com.ue.wearable_hud.flux.task

import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

private val logger = KotlinLogging.logger {}

interface Task {
    val id: String
    fun nextRunAt(): Long
    fun readyToSchedule(): Boolean
    fun getLines(): Collection<String>
    suspend fun run(): Collection<String>
}

class StaticTask(override val id: String, var strings: Collection<String>): Task {
    init {
        logger.info { "Creating new StaticTask for task $id" }
    }

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

    init {
        logger.info { "Creating new UnixTask for task $id. Command: '${workingDir}/$command' Refresh: ${refreshPeriodSec}s" }
    }

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
        logger.info("UnixTask ${id} running command '$command'")
        try {
            val parts = command.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            proc.waitFor(1, TimeUnit.MINUTES)
            
            val text = proc.inputStream.bufferedReader().readText()

            lastRun = System.currentTimeMillis()

            val lines = text.split("\n")
            val lastLine = if (lines.last() == "") lines.count() - 1 else lines.count() // Strip out an empty last line
            this.lines = lines.slice(0 until lastLine)

            return lines
        } catch (e: IOException) {
            e.printStackTrace()
            return emptyList()
        }
    }
}

class NullTask : Task {
    init {
        logger.info { "Creating new NullTask" }
    }

    override val id = "nulltask"
    override fun nextRunAt(): Long = Long.MAX_VALUE // Never needs to be scheduled
    override fun readyToSchedule(): Boolean = false // Never needs to be run
    override fun getLines(): Collection<String> = emptyList()
    override suspend fun run(): Collection<String> = emptyList()
}
