/*
 * Copyright (c) 2019, Projekt Development LLC.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package projekt.commons.shell

import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader
import java.util.*
import kotlin.collections.ArrayList

/**
 * Main class for shell helpers, by default non-root shell will be initialized.
 */
object Shell {
    private var proc = ProcessBuilder("sh").apply { redirectErrorStream(true) }.start()
    private var writer = DataOutputStream(proc.outputStream)
    private var outputReader = BufferedReader(InputStreamReader(proc.inputStream))
    private var isRoot = false

    /**
     * Reinitialize shell process with root access
     */
    fun reinitWithRoot() {
        proc = ProcessBuilder("su").apply { redirectErrorStream(true) }.start()
        writer = DataOutputStream(proc.outputStream)
        outputReader = BufferedReader(InputStreamReader(proc.inputStream))
        isRoot = true
    }

    /**
     * Check if root access is available
     *
     * @return `true` if root access is available
     */
    fun rootAccess(): Boolean {
        return if (!isRoot) {
            false
        } else {
            exec("echo revel").output[0] == "revel"
        }
    }

    /**
     * Executes command with initialized shell process
     *
     * @param command the command to be executed
     * @return [Result] of command
     */
    fun exec(command: String): Result {
        return exec(arrayOf(command))
    }

    /**
     * Executes command with initialized shell process
     *
     * @param commands array of commands to be executed in sequence
     * @return [Result] of commands
     */
    @Synchronized fun exec(commands: Array<String>): Result {
        val output = arrayListOf<String>()
        val callback = UUID.randomUUID().toString()
        try {
            writer.run {
                commands.forEach {
                    writeBytes("$it\n")
                    flush()
                }
                writeBytes("echo $callback\n")
                flush()
            }
            while(true) {
                val line = outputReader.readLine()
                if (line == callback || line == null) {
                    break
                } else {
                    output.add(line)
                }
            }
        } catch (ignored: Exception) {
            ignored.printStackTrace()
        }
        return Result(output)
    }

    /**
     * The result of executed command
     *
     * @param output Output of executed command
     */
    data class Result(val output: ArrayList<String>)
}
