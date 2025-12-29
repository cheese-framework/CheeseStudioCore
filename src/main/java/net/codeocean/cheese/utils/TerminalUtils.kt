package net.codeocean.cheese.utils

import java.io.File
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.Executors
import java.util.concurrent.Future

object TerminalUtils {
    /**
     * 执行 Shell 命令并实时获取输出信息。
     * @param command 要执行的命令。
     * @param workingDir 要设置的工作目录。
     * @param onOutput 行输出的处理函数。
     * @return 命令的输出。
     */
    fun executeCommand(command: String, workingDir: File, onOutput: (String) -> Unit): String {
        val os = System.getProperty("os.name").lowercase()
        val isWindows = os.contains("win")

        // 构造命令
        val finalCommand = if (isWindows) {
            arrayOf("cmd.exe", "/c", "chcp 65001 && $command")
        } else {
            arrayOf("/bin/sh", "-c", command)
        }

        val executor = Executors.newFixedThreadPool(2) // 使用固定线程池
        return try {
            val processBuilder = ProcessBuilder(*finalCommand).apply {
                redirectErrorStream(true)  // 将错误流合并到标准输出流
                if (workingDir.exists()) {
                    directory(workingDir)  // 设置工作目录
                }

            }

            val process = processBuilder.start()

            // 使用线程池读取流
            val stdoutFuture: Future<*> = executor.submit { readStream(process.inputStream, onOutput) }
            val stderrFuture: Future<*> = executor.submit { readStream(process.errorStream, onOutput) }

            val exitCode = process.waitFor()

            // 确保所有流都被读取
            stdoutFuture.get()
            stderrFuture.get()

            if (exitCode != 0) {
                throw IOException("Command failed with exit code $exitCode.")
            }

            "Command completed successfully."
        } catch (e: IOException) {
            e.printStackTrace()
            "Error: ${e.message}"
        } finally {
            executor.shutdown()
        }
    }

    /**
     * 读取输入流并处理每行输出。
     * @param inputStream 输入流。
     * @param onOutput 行输出的处理函数。
     */
    private fun readStream(inputStream: InputStream, onOutput: (String) -> Unit) {
        inputStream.bufferedReader(StandardCharsets.UTF_8).use { reader ->
            reader.lineSequence().forEach { line ->
                onOutput(line)
            }
        }
    }

}