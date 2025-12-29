package net.codeocean.cheese.utils

import net.codeocean.cheese.server.Log
import net.codeocean.cheese.utils.FileUtils.isFileSizeLessThan10MB
import net.codeocean.cheese.utils.TerminalUtils.executeCommand
import org.json.JSONObject
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

object HttpUtils {

    fun curl(url:String,path:String):Boolean{
        val command = """
        curl -L -C - "$url" -o "$path" 
    """.trimIndent()
        executeCommand(command, File("")) { output ->
            Log.logger?.info(output)
        }

        return isFileSizeLessThan10MB(path)
    }

    fun getData(url: String): JSONObject? {
        try {
            // 创建 URL 对象并打开连接
            val urlObj = URL(url)
            val connection = urlObj.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.setRequestProperty("User-Agent", "Mozilla/5.0")

            // 获取响应并读取数据
            val inputStream = connection.inputStream
            val reader = BufferedReader(InputStreamReader(inputStream))
            val response = StringBuilder()

            var line: String?
            while (reader.readLine().also { line = it } != null) {
                response.append(line)
            }
            reader.close()

            // 解析 JSON 数据
            return try {
                JSONObject(response.toString())
            } catch (e: Exception) {
                e.printStackTrace()  // 解析失败时打印错误
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()  // 请求失败时打印错误
            return null
        }
    }


    fun postAppData(appMD5: String, appKey: String) {
        val url = "http://localhost:8080/insert-or-update" // 接口 URL
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true

        // 创建 JSON 请求体
        val jsonInput = JSONObject()
        jsonInput.put("app_md5", appMD5)
        jsonInput.put("app_key", appKey)

        // 发送请求数据
        DataOutputStream(connection.outputStream).use { outputStream ->
            outputStream.writeBytes(jsonInput.toString())
            outputStream.flush()
        }

        // 读取响应内容
        BufferedReader(InputStreamReader(connection.inputStream)).use { reader ->
            val response = StringBuilder()
            var inputLine: String?
            while (reader.readLine().also { inputLine = it } != null) {
                response.append(inputLine)
            }
            println("Response: ${response.toString()}")
        }
    }

}