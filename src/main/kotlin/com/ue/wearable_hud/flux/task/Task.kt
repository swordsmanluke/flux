package com.ue.wearable_hud.flux.task

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
    fun sendCommand(command: List<String>): Collection<String>
}

class StaticTask(override val id: String, var strings: Collection<String> = emptyList()) : Task {
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
    override fun sendCommand(command: List<String>): Collection<String> = strings // Static Tasks do not support commands
}

class UnixTask(override val id: String, val workingDir: File, val command: String, val refreshPeriodSec: Int) : Task {
    private var lines = emptyList<String>()
    private var lastRun = 0L
    private var running = false

    init {
        logger.info { "Creating new UnixTask for task $id. Command: '${workingDir}/$command' Refresh: ${refreshPeriodSec}s" }
    }

    override fun nextRunAt(): Long {
        val nextRefresh = refreshPeriodSec * 1000
        // Add +-5 % jitter to avoid same-time refreshes
        // val negativeJitterOffset = -(nextRefresh*0.05).toInt()
        // val jitter = Random.nextInt(negativeJitterOffset, (nextRefresh*0.1).toInt())
        return lastRun + nextRefresh
    }

    override fun readyToSchedule(): Boolean {
        return !running && System.currentTimeMillis() > nextRunAt()
    }

    override fun getLines(): Collection<String> {
        return lines
    }

    override suspend fun run(): Collection<String> {
        running = true
        return try {
            runCommand()
        } catch (e: IOException) {
            logger.error("Unhandled exception running UnixTask", e)
            emptyList()
        } finally {
            running = false
        }
    }

    override fun sendCommand(command: List<String>): Collection<String> {
        runBlocking {
            runCommand(command)
        }
        return lines
    }

    private suspend fun runCommand(args: List<String> = emptyList()): List<String> {
        val parts = command.split("\\s".toRegex()) + args
        var text = ""
        var exitCode = -1000

        val backgroundRunner = Thread {

            val proc = ProcessBuilder(*parts.toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            proc.waitFor(1, TimeUnit.MINUTES)
            exitCode = proc.exitValue()
            text = proc.inputStream.bufferedReader().readText() + proc.errorStream.bufferedReader().readText()
        }

        backgroundRunner.start()

        while (backgroundRunner.isAlive) {
            delay(100)
        }

        lastRun = System.currentTimeMillis()
        logger.info { "Command '${parts.joinToString(" ")}' exit: $exitCode" }
        val lines = text.split("\n")
        val lastLine = if (lines.last() == "") lines.count() - 1 else lines.count() // Strip out an empty last line
        this.lines = lines.slice(0 until lastLine)
        return lines
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
    override fun sendCommand(command: List<String>): Collection<String> = emptyList()
}
