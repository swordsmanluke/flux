package com.ue.wearable_hud.flux.task

import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

interface Task {
    fun getLines(): Collection<String>
    fun run(): Collection<String>
}

class UnixTask(val workingDir: File, val command: String): Task {

    private var lines : Collection<String> = emptyList()

    override fun getLines() : Collection<String> {
        return lines
    }

    override fun run(): Collection<String> {
        try {
            val parts = command.split("\\s".toRegex())
            val proc = ProcessBuilder(*parts.toTypedArray())
                    .directory(workingDir)
                    .redirectOutput(ProcessBuilder.Redirect.PIPE)
                    .redirectError(ProcessBuilder.Redirect.PIPE)
                    .start()

            proc.waitFor(60, TimeUnit.MINUTES)
            val text = proc.inputStream.bufferedReader().readText()
            lines = text.split("\n")
            return lines
        } catch(e: IOException) {
            e.printStackTrace()
            return emptyList()
        }
    }
}

class NullTask(): Task {
    override fun getLines() : Collection<String> = emptyList()
    override fun run(): Collection<String> = emptyList()
}
