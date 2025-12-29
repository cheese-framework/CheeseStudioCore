package net.codeocean.cheese

import coco.cheese.ide.test.ColorTest.findMultiColors
import coco.cheese.ide.test.ColorTest.parseColor
import net.codeocean.cheese.Env
import net.codeocean.cheese.manager.SDKManager
import net.codeocean.cheese.project.BuildProject
import net.codeocean.cheese.project.CreateProject
import net.codeocean.cheese.project.DebugProject
import net.codeocean.cheese.project.UpdateProject
import net.codeocean.cheese.server.Log
import net.codeocean.cheese.server.closeProcessByPortWindows
import net.codeocean.cheese.server.routeWebSocket
import net.codeocean.cheese.ui.NodeUi
import net.codeocean.cheese.ui.PaintUi
import net.codeocean.cheese.utils.FileUtils.getJsonValue
import net.codeocean.cheese.utils.HttpUtils
import net.codeocean.cheese.utils.HttpUtils.getData
import net.codeocean.cheese.utils.ZipUtils.unzip
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.formdev.flatlaf.FlatDarkLaf
import com.formdev.flatlaf.FlatLightLaf
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import org.kohsuke.args4j.CmdLineParser
import org.kohsuke.args4j.Option
import org.opencv.core.Core
import org.opencv.core.Mat
import org.opencv.core.Point
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.awt.Color
import java.awt.Dimension
import java.io.File
import java.io.File.separator
import java.time.Duration
import javax.swing.*

class CommandLineTool {
    // 配置输入的 JSON 字符串
    @Option(name = "-c", usage = "JSON string input for configuration", required = false)
    var configInput: String? = null

    // 是否创建项目
    @Option(name = "-create", usage = "Create project from template", required = false)
    var createProject: Boolean = false

    // 是否运行代码
    @Option(name = "-runCode", usage = "Run the code after project creation", required = false)
    var runCode: Boolean = false

    // 是否启动 UI
    @Option(name = "-runUi", usage = "Run the UI after project creation", required = false)
    var runUi: Boolean = false

    // 是否启动服务器
    @Option(name = "-server", usage = "Start server for project", required = false)
    var server: Boolean = false

    // 是否构建项目
    @Option(name = "-build", usage = "Build the project", required = false)
    var build: Boolean = false

    // 是否编译热更新产物
    @Option(name = "-hot", usage = "Build the project", required = false)
    var hot: Boolean = false

    // 是否编译PIP项目
    @Option(name = "-pip", usage = "Build the project", required = false)
    var pip: Boolean = false

    // 是否更新项目
    @Option(name = "-updateProject", usage = "Update the project", required = false)
    var updateProject: Boolean = false

    // 是否检查 SDK
    @Option(name = "-checkSDK", usage = "Check SDK version", required = false)
    var checkSDK: Boolean = false

    // 项目的基础目录
    @Option(name = "-baseDir", usage = "Base directory for project", required = false)
    var baseDir: String? = null

    // SDK 的路径
    @Option(name = "-sdkPath", usage = "Path to the SDK for the project template", required = false)
    var sdkPath: String? = null

    // 目标路径
    @Option(name = "-o", usage = "Path to the destination where the project will be created", required = false)
    var outputPath: String? = null

    // 服务器的端口号
    @Option(name = "-port", usage = "Port number for server", required = false)
    var port: Int? = null


    // 是否解包
    @Option(name = "-u", usage = "Untie the project", required = false)
    var u: Boolean = false

    // 代理设置
    @Option(name = "-proxy", usage = "Proxy settings", required = false)
    var proxy: String? = null


    fun validate() {
        if (createProject) {
            requireNotNull(sdkPath) { "❌ 创建项目失败：缺少 SDK 路径 (-sdk)" }
            requireNotNull(outputPath) { "❌ 创建项目失败：缺少输出路径 (-o)" }
            requireNotNull(configInput) { "❌ 创建项目失败：缺少配置信息 (-c)" }
        } else if (server) {
            requireNotNull(port) { "❌ 启动服务器失败：缺少端口号 (-port)" }
            requireNotNull(sdkPath) { "❌ 启动服务器失败：缺少 SDK 路径 (-sdk)" }
        } else if (runCode || runUi) {
            requireNotNull(baseDir) { "❌ 运行${if (runCode) "代码" else "界面"}失败：缺少基础目录 (-baseDir)" }
            requireNotNull(sdkPath) { "❌ 运行失败：缺少 SDK 路径 (-sdk)" }
        } else if (build) {
            requireNotNull(sdkPath) { "❌ 构建失败：缺少 SDK 路径 (-sdk)" }
            requireNotNull(baseDir) { "❌ 构建失败：缺少基础目录 (-baseDir)" }
        } else if (hot) {
            requireNotNull(sdkPath) { "❌ 热更新失败：缺少 SDK 路径 (-sdk)" }
            requireNotNull(baseDir) { "❌ 热更新失败：缺少基础目录 (-baseDir)" }
        } else if (pip) {
            requireNotNull(sdkPath) { "❌ PIP 操作失败：缺少 SDK 路径 (-sdk)" }
            requireNotNull(baseDir) { "❌ PIP 操作失败：缺少基础目录 (-baseDir)" }
        } else if (checkSDK) {
            requireNotNull(sdkPath) { "❌ 检查 SDK 失败：缺少 SDK 路径 (-sdk)" }
        } else if (updateProject) {
            requireNotNull(sdkPath) { "❌ 更新项目失败：缺少 SDK 路径 (-sdk)" }
            requireNotNull(baseDir) { "❌ 更新项目失败：缺少基础目录 (-baseDir)" }
        }
    }


    fun parseConfig(): Map<String, String> {
        return jacksonObjectMapper().readValue(
            configInput!!.replace(Regex("""(\w+):([^,}]+)""")) {
                "\"${it.groupValues[1]}\":\"${it.groupValues[2]}\""
            }
        )
    }

    fun execute() {
        validate()
        if (createProject) {
            val config = parseConfig()
            try {
                CreateProject(config, sdkPath!!, outputPath!!)
            } catch (e: Exception) {
                println(e)
            }

        } else if (server) {
            port?.let {
                closeProcessByPortWindows(port!!)
                Env.sdkPath = sdkPath
                embeddedServer(Netty, port = it) {
                    install(io.ktor.server.websocket.WebSockets) {
                        // 配置 WebSocket ping/pong 和超时
                        pingPeriod = Duration.ofSeconds(15)
                        timeout = Duration.ofSeconds(30)
                        maxFrameSize = Long.MAX_VALUE
                        masking = false

                    }
                    routing {
                        routeWebSocket()
                    }
                }.start(wait = false)
                println("Start Ok")
                Thread.currentThread().join()
            }
        } else if (runCode) {
            baseDir?.let { sdkPath?.let { it1 -> DebugProject(it1, it).run() } }
        } else if (runUi) {
            baseDir?.let { sdkPath?.let { it1 -> DebugProject(it1, it).runUi() } }
        } else if (build) {
            BuildProject(u, sdkPath!!, baseDir!!)
        } else if (hot) {

            baseDir?.let { sdkPath?.let { it1 -> DebugProject(it1, it).hot() } }
        } else if (pip) {

            baseDir?.let { sdkPath?.let { it1 -> DebugProject(it1, it).pip() } }
        } else if (checkSDK) {
            if (SDKManager(sdkPath!!).compareVersions()) {
                Log.logger?.info("需要更新SDK,即将开始下载")
                if (proxy != null) {
                    Log.logger?.info("设置代理镜像：" + proxy)
                    HttpUtils.curl(
                        proxy + "/https://github.com/0cococ/cheese-sdk/releases/download/cheese-sdk/cheese-sdk.zip",
                        sdkPath!! + separator + "cheese-sdk.zip"
                    )

                } else {
                    Log.logger?.info("不设置代理镜像")
                    HttpUtils.curl(
                        "https://github.com/0cococ/cheese-sdk/releases/download/cheese-sdk/cheese-sdk.zip",
                        sdkPath!! + separator + "cheese-sdk.zip"
                    )
                }
                unzip(sdkPath!! + separator + "cheese-sdk.zip", sdkPath!!)
                Log.logger?.info("更新完毕")
            } else {
                Log.logger?.info("不需要更新SDK")
            }
        } else if (updateProject) {
            UpdateProject(sdkPath!!, baseDir!!)

        }
    }
}


fun main(args: Array<String>) {


    FlatLightLaf.setup()
    UIManager.setLookAndFeel(FlatDarkLaf());


    val tool = CommandLineTool()
    val parser = CmdLineParser(tool)
    try {
        parser.parseArgument(*args)
        tool.execute()
    } catch (e: Exception) {
        println("Error: ${e.message}")
        parser.printUsage(System.err)
    }
}
