package net.codeocean.cheese.server

import net.codeocean.cheese.Env
import net.codeocean.cheese.Env.httpDownloadPath
import net.codeocean.cheese.ui.NodeUi
import net.codeocean.cheese.ui.PaintUi
import net.codeocean.cheese.utils.FileUtils.createDirectoryIfNotExists
import net.codeocean.cheese.utils.TerminalUtils.executeCommand

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import io.ktor.util.logging.*
import io.ktor.websocket.*
import java.awt.Dimension
import java.io.BufferedReader
import java.io.File
import java.io.File.separator
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import javax.swing.ImageIcon
import javax.swing.JComponent
import javax.swing.JFrame
import javax.swing.JTabbedPane

val clientSessions = ConcurrentHashMap<String, WebSocketServerSession>()
fun Routing.routeWebSocket() {
    // 使用线程安全的 Map 存储设备 ID 和客户端会话
    get("/download") {
        if (!httpDownloadPath.isNullOrEmpty()) { // 确保路径非空
            val file = File(httpDownloadPath!!) // 文件路径
            if (file.exists()) {
                call.respondFile(file) // 如果文件存在，返回文件
            } else {
                call.respondText("File not found", ContentType.Text.Plain) // 文件不存在时返回错误信息
            }
        } else {
            call.respondText("Invalid or missing download path", ContentType.Text.Plain) // 路径为空时返回错误信息
        }
    }
    webSocket("/chat") {
        var deviceId: String? = null // 当前客户端的设备 ID
        try {
            // 监听客户端消息
            for (frame in incoming) {
                when (frame) {
                    is Frame.Text -> {
                        val receivedText = frame.readText()
                        if (deviceId == null) {
                            deviceId = receivedText
                            clientSessions[deviceId] = this
                            Log.logger?.info("设备连接: $deviceId")
                        } else {
                            if (deviceId.contains("device")) {
                                clientSessions["ide"]?.send(receivedText)
                            }
                            else if (deviceId.contains("tool")){
                                if (receivedText=="node"){
                                    Log.logger?.info("新工具获取节点")
                                    clientSessions["device"]?.send("5")
                                }else{
                                    Log.logger?.info("新工具获取图片")
                                    clientSessions["device"]?.send("4")
                                }
                            }
                            else if (deviceId.contains("ide")){
                                if (receivedText=="工具"){
                                    val paintFrame: JFrame = PaintUi.getFrame() // 获取 PaintUi 的 JFrame
                                    val nodeFrame: JFrame = NodeUi.getFrame() // 获取 NodeUi 的 JFrame

                                    // 获取 JFrame 的内容面板，避免直接将整个窗口添加到标签中
                                    val paintContentPane = paintFrame.contentPane as JComponent
                                    val nodeContentPane = nodeFrame.contentPane as JComponent

                                    // 创建 JTabbedPane 组件并设置样式
                                    val tabbedPane = JTabbedPane(JTabbedPane.TOP).apply {
                                        // 添加标签和面板
                                        addTab("图色", paintContentPane)  // 第一项标签
                                        addTab("节点", nodeContentPane)  // 第二项标签
                                        tabLayoutPolicy = JTabbedPane.SCROLL_TAB_LAYOUT
                                    }

                                    // 创建主窗口 JFrame
                                    val mainFrame = JFrame("抓抓").apply {
                                        defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE // 改为 DISPOSE_ON_CLOSE
                                        size = Dimension(800, 600)  // 设置窗口大小
                                        contentPane = tabbedPane  // 将 tabbedPane 设置为主窗口的内容面板
                                        val classLoader = Env::class.java.classLoader
                                        val resource = classLoader.getResource("icon.png")
                                        val icon= ImageIcon(resource)
                                        this.iconImage = icon.image  // 设置窗口图标
                                    }

                                    // 显示窗口
                                    mainFrame.isVisible = true

                                }
                                else if(receivedText.contains("http")){
                                    val secondPart = receivedText.split("|")
                                    httpDownloadPath=secondPart.last()
                                    clientSessions["device"]?.send(secondPart[1])
                                }
                                else{
                                    clientSessions["device"]?.send(receivedText)
                                }

                            }
                        }
                    }
                    is Frame.Binary -> {
                        val data = frame.readBytes()
                        val separatorIndex = data.indexOf(124.toByte())
                        if (deviceId != null) {
                            if (deviceId.contains("device")) {
                                if (separatorIndex == -1) {
                                    Log.logger?.error("Invalid data format: no separator found.")
                                    continue
                                }

                                val cmdBytes = data.copyOfRange(0, separatorIndex)
                                val cmd = String(cmdBytes, Charsets.UTF_8) // 转换为字符串
                                val remainingData = data.copyOfRange(separatorIndex + 1, data.size)
                                val fileNameIndex = remainingData.indexOf(124.toByte()) // 查找第二个分隔符
                                val fileNameBytes = remainingData.copyOfRange(0, fileNameIndex)
                                val fileName = String(fileNameBytes, Charsets.UTF_8)
                                val fileContentBytes = remainingData.copyOfRange(fileNameIndex + 1, remainingData.size)
                                // 保存文件（示例：保存到当前目录下）
                                if(cmd=="1"){
                                    createDirectoryIfNotExists("${Env.sdkPath}${separator}material${separator}device")
                                    val uploadDir =  "${Env.sdkPath}${separator}material${separator}device"
                                    val uniqueFileName = UUID.randomUUID().toString() + "_" + fileName
                                    val file = java.io.File(uploadDir,uniqueFileName)
                                    println(file.absolutePath)
                                    file.writeBytes(fileContentBytes)
//
//                                    Log.logger?.info("新工具展示图片")
//                                    clientSessions["tool"]?.send(cmd+"|"+file.absolutePath)
                                    PaintUi.imagePanel?.loadImage(file.absolutePath)
//                                    loadImage(file.absolutePath)
                                    file.delete()
                                }else if (cmd=="2"){
                                    createDirectoryIfNotExists("${Env.sdkPath}${separator}material${separator}device")
                                    val uploadDir =  "${Env.sdkPath}${separator}material${separator}device"
                                    val uniqueFileName = UUID.randomUUID().toString() + "_" + fileName
                                    val file = java.io.File(uploadDir,uniqueFileName)
                                    println(file.absolutePath)
                                    file.writeBytes(fileContentBytes)
//                                    Log.logger?.info("新工具展示图片")
//                                    clientSessions["tool"]?.send(cmd+"|"+file.absolutePath)
                                    NodeUi.imagePanel?.loadImage(file.absolutePath)
//                                    loadImage(file.absolutePath)
                                    file.delete()
                                }else{
                                    createDirectoryIfNotExists("${Env.sdkPath}${separator}material${separator}device")
                                    val uploadDir =  "${Env.sdkPath}${separator}material${separator}device"
                                    val uniqueFileName = UUID.randomUUID().toString() + "_" + fileName
                                    val file = java.io.File(uploadDir,uniqueFileName)
                                    println(file.absolutePath)
                                    file.writeBytes(fileContentBytes)
//                                    Log.logger?.info("新工具展示节点")
//                                    clientSessions["tool"]?.send(cmd+"|"+file.absolutePath)
                                    NodeUi.loadXML(file.absolutePath)

                                    file.delete()
                                }
//                                Log.logger?.info("Cmd：${cmd} File received: $fileName (${fileContentBytes.size} bytes)")
                            }else if (deviceId.contains("ide")){
                                clientSessions["device"]?.send(data)
                            }
                        }



                    }
                    else -> {
                        Log.logger?.error("Received unsupported frame type.")
                    }
                }


            }
        } catch (e: Exception) {
            Log.logger?.error(e.message,e)

        } finally {

            // 移除断开连接的客户端
            deviceId?.let {
//                if (it.contains("ide")){
//                    System.exit(0); // 正常退出程序
//                }
                clientSessions.remove(it)
                Log.logger?.error("设备 $it 已断开连接")
            }
        }
    }
}


fun closeProcessByPortWindows(port: Int) {
    try {
        // 使用 netstat 查找使用指定端口的进程 PID
        var pid: String? = null
        val command = "netstat -ano | findstr :$port"
        executeCommand(command, File("")) { output ->
            if (output.contains(":$port")) {
                val parts = output.split("\\s+".toRegex())
                if (parts.size > 4) {
                    pid = parts[parts.size - 1]  // PID 通常是最后一列
                }
            }


        }
        Log.logger?.info(pid,)
        if (pid != null) {
            val killCommand = "taskkill /PID $pid /F"
            val killProcess = Runtime.getRuntime().exec(killCommand)
            killProcess.waitFor() // 等待进程终止
        }
    } catch (e: InterruptedException) {
        Log.logger?.error(e.message,e)
    } catch (e: Exception) {
        Log.logger?.error(e.message,e)
    }
}


