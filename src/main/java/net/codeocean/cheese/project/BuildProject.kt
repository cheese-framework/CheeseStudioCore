package net.codeocean.cheese.project

import com.iyxan23.zipalignjava.ZipAlign;
import net.codeocean.cheese.server.Log
import net.codeocean.cheese.utils.APKToolUtils
import net.codeocean.cheese.utils.FileUtils
import net.codeocean.cheese.utils.FileUtils.convertPath
import net.codeocean.cheese.utils.FileUtils.copyDirectory
import net.codeocean.cheese.utils.FileUtils.copyFile
import net.codeocean.cheese.utils.FileUtils.getJksCertificateMd5
import net.codeocean.cheese.utils.FileUtils.zipDirectory
import net.codeocean.cheese.utils.TerminalUtils.executeCommand
import net.codeocean.cheese.utils.YamlUtils
import net.codeocean.cheese.utils.ZipUtils
import com.google.gson.GsonBuilder
import com.iyxan23.zipalignjava.InvalidZipException
import org.jetbrains.annotations.NonNls
import org.tomlj.Toml
import org.tomlj.TomlParseError
import org.tomlj.TomlParseResult
import org.tomlj.TomlTable
import org.w3c.dom.Element
import java.io.File
import java.io.File.separator
import java.io.FileOutputStream
import java.io.IOException
import java.io.RandomAccessFile
import java.lang.Thread.sleep
import java.lang.reflect.Method
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.function.Consumer
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

class BuildProject(val untie: Boolean, val sdkPath: String, val baseDir: String) {
    val outputDir = File("${sdkPath}${separator}material${separator}apkout")
    val buildDir = File("${baseDir}${separator}build")

    init {
        val startTime = System.currentTimeMillis()

        var totalSteps = 6 // 初始已知的步骤
        var currentStep = 0
        try {
            clear()
            currentStep++
            updateProgress(currentStep, totalSteps)
            Unpkg()
            currentStep++
            updateProgress(currentStep, totalSteps)
            parseConfig()
            currentStep++
            updateProgress(currentStep, totalSteps)
            setConfig()
            currentStep++
            updateProgress(currentStep, totalSteps)
            buildPKG()
            currentStep++
            updateProgress(currentStep, totalSteps)
            sleep(2000)
            singAPK()
            currentStep++
            updateProgress(currentStep, totalSteps)
        } catch (e1: Exception) {
            Log.logger?.info(e1.toString())
            e1.printStackTrace()
        }
        val endTime = System.currentTimeMillis()
        val elapsedTimeInSeconds = (endTime - startTime) / 1000.0
        Log.logger?.info("构建完毕 耗时：${elapsedTimeInSeconds}秒")

    }

    fun updateProgress(currentStep: Int, totalSteps: Int) {
        val progress = (currentStep.toDouble() / totalSteps) * 100
        Log.logger?.info("进度: ${"%.2f".format(progress)}%")

    }


    fun clear() {
        Log.logger?.info("初始构建环境...")
        if (buildDir.exists()) {
            buildDir.deleteRecursively()
        }

    }

    fun Unpkg() {
        if (outputDir.exists()) {
            if (untie) {
                Log.logger?.info("开始解包...")

                outputDir.deleteRecursively()

                val source: Path = Paths.get("${baseDir}${separator}cheese.toml")
                val result: TomlParseResult = Toml.parse(source)
                result.errors().forEach(Consumer { error: TomlParseError ->
                    System.err.println(
                        error.toString()
                    )
                })
                val bindings = result.getString("bindings")
                if (bindings?.contains("node") == true) {
                    APKToolUtils.decodeApk(
                        File("${sdkPath}${separator}components${separator}project${separator}node.apk"),
                        outputDir
                    )
                } else if (bindings == "js" || bindings == "ts") {
                    APKToolUtils.decodeApk(
                        File("${sdkPath}${separator}components${separator}project${separator}js.apk"),
                        outputDir
                    )
                } else {
                    Log.logger?.error("解包失败，项目配置文件非最新版！")
                }


                Log.logger?.info("解包完毕...")
            } else {
                Log.logger?.info("跳过解包...")
            }
        } else {
            Log.logger?.info("开始解包...")
            outputDir.deleteRecursively()


            val source: Path = Paths.get("${baseDir}${separator}cheese.toml")
            val result: TomlParseResult = Toml.parse(source)
            result.errors().forEach(Consumer { error: TomlParseError ->
                System.err.println(
                    error.toString()
                )
            })
            val bindings = result.getString("bindings")
            if (bindings?.contains("node") == true) {
                APKToolUtils.decodeApk(
                    File("${sdkPath}${separator}components${separator}project${separator}node.apk"),
                    outputDir
                )
            } else if (bindings == "js" || bindings == "ts") {
                APKToolUtils.decodeApk(
                    File("${sdkPath}${separator}components${separator}project${separator}js.apk"),
                    outputDir
                )
            } else {
                Log.logger?.error("解包失败，项目配置文件非最新版！")

            }
            Log.logger?.info("解包完毕...")
        }
    }

    data class CommandArgs(
        val archs: String = "",  // 例如 "armeabi-v7a,x86"
        val platform: String = "" // 例如 "a"
    )

    fun parseCommand(input: String): CommandArgs {
        val parts = input.split("\\s+".toRegex())
        var archs = ""
        var platform = ""

        // 使用 Map 存储参数键值对
        val argsMap = mutableMapOf<String, String>()
        var i = 0
        while (i < parts.size) {
            if (parts[i].startsWith("-")) {
                val key = parts[i]
                if (i + 1 < parts.size && !parts[i + 1].startsWith("-")) {
                    argsMap[key] = parts[i + 1]
                    i++ // 跳过下一个元素（值）
                }
            }
            i++
        }

        // 从 Map 中提取参数
        archs = argsMap["-a"] ?: "x86,x86_64,arm64-v8a,armeabi-v7a"
        platform = argsMap["-p"] ?: ""

        return CommandArgs(archs, platform)
    }


    fun getLastDirName(path: String): String? {
        val file = File(path.trimEnd('/'))
        return if (file.isDirectory || !file.exists()) {
            file.name
        } else {
            file.parentFile?.name
        }
    }

    fun setSdkPath(path: String) {
        val file = File("${sdkPath}/components/project/jvm/local.properties")
        println(file.absolutePath)

        // 转换路径格式
        val escapedPath = path
            .replace("\\", "\\\\")  // 将单个反斜杠转义为双反斜杠
            .replace(":", "\\:")    // 转义冒号（如果需要）
        println(escapedPath)

        file.writeText(
            """
        sdk.dir=$escapedPath
        """.trimIndent()
        )


    }

    fun writeDependenciesJson(result: TomlParseResult) {
        val dependenciesArray = result.getArray("dependencies")

        // 1. 构建数据结构
        val dependenciesList = dependenciesArray?.let { array ->
            (0 until array.size()).map { array.getString(it) }.also {
                println(" ${it.size} 个依赖项")
            }
        } ?: run {
            println("0个依赖项")
            emptyList<String>()
        }

        // 2. 创建 Gson 实例（配置完美格式）
        val gson = GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()

        // 3. 生成标准格式 JSON
        val json = gson.toJson(mapOf("implementation" to dependenciesList))
            // Gson 默认格式微调（精确匹配你的要求）
            .replace("\": [", "\": [")  // 移除冒号后多余空格
            .replace("  \"", "    \"")  // 调整缩进为2空格

        // 4. 写入文件
        val outputFile = File("${sdkPath}/components/project/jvm/app/dependencies.json").apply {
            parentFile.mkdirs()
        }

        try {
            outputFile.writeText(json)
//            println("Gson 写入成功: ${outputFile.absolutePath}")
        } catch (e: Exception) {
            System.err.println("写入失败: ${e.message}")
        }
    }

    fun parseConfig() {
        Log.logger?.info("解析构建配置...")
        baseDir.let {
            val mainPath = "$it${separator}src${separator}main"
            val tomlPath = "$it${separator}cheese.toml"
            val buildPath = "$it${separator}build"
            val node_modules = "$it${separator}node_modules"
            val result: TomlParseResult = Toml.parse(Paths.get(tomlPath))
            result.errors().forEach(Consumer { error: TomlParseError ->
                System.err.println(
                    error.toString()
                )
            })

            if (!File("${sdkPath}${separator}material${separator}jks${separator}cheese.jks").exists()) {
                FileUtils.createDirectoryIfNotExists("${sdkPath}${separator}material${separator}jks")
                extractResourceToLocal(
                    "jks/cheese.jks",
                    File("${sdkPath}${separator}material${separator}jks${separator}cheese.jks")
                )
            }
            val buildTable = result.getTable("build")
            val build_toolsTable = buildTable!!.getString("protection")



            if (result.getString("bindings")?.contains("ts") == true) {
                val command = "tsc" // 替换为你想要执行的命令
                executeCommand(command, File(it)) { output ->
                    println(output)
                }



                copyDirectory("$buildPath${separator}js", "$buildPath${separator}release${separator}main${separator}js")
            } else {


                if (build_toolsTable != null) {


                    if (build_toolsTable == "obfuscator") {
                        val command = "npm run build" // 替换为你想要执行的命令
                        executeCommand(command, File("$baseDir${separator}dev")) { output ->
                            println(output)
                        }
                    } else if (build_toolsTable.contains("cloak")) {
                        val command =
                            "cloak compile -i ${baseDir}${separator}src${separator}main${separator}js -o ${baseDir}${separator}build${separator}debug${separator}main${separator}js -p cheese -s 2 -a " + parseCommand(
                                build_toolsTable
                            ).archs // 替换为你想要执行的命令
                        executeCommand(command, File("$sdkPath${separator}bin")) { output ->
                            println(output)
                        }
                        ZipUtils.unzipFileToSameDirectory("${baseDir}${separator}build${separator}debug${separator}main${separator}js${separator}cloak.zip")
                        File("${baseDir}${separator}build${separator}debug${separator}main${separator}js${separator}cloak.zip").delete()

                        File(buildPath + "${separator}debug${separator}main${separator}assets").mkdirs()

                        File("${baseDir}${separator}build${separator}debug${separator}main${separator}js${separator}cloak.apk").renameTo(
                            File(buildPath + "${separator}debug${separator}main${separator}assets${separator}cloak.apk")
                        )
                    } else {
                        copyDirectory(
                            "$mainPath${separator}js",
                            "$buildPath${separator}release${separator}main${separator}js"
                        )
                    }


                } else {
                    copyDirectory(
                        "$mainPath${separator}js",
                        "$buildPath${separator}release${separator}main${separator}js"
                    )
                }


            }

            val ui = result.getString("ui")

            if (ui == "xml") {
                copyDirectory("$mainPath${separator}ui", "$buildPath${separator}release${separator}main${separator}ui")
            } else {

                executeCommand("npm run build", File(convertPath("$mainPath${separator}ui"))) { output ->
                    Log.logger?.info(output)
                }

                copyDirectory(
                    convertPath("$mainPath${separator}ui/dist"),
                    "$buildPath${separator}release${separator}main${separator}ui"
                )

            }
            copyDirectory(
                "$mainPath${separator}assets",
                buildPath + "${separator}release${separator}main${separator}assets"
            )
            copyDirectory(node_modules, buildPath + "${separator}release${separator}node_modules")
            copyFile(tomlPath, buildPath + "${separator}release${separator}cheese.toml")

            copyFile(
                mainPath + "${separator}icon.png",
                "${sdkPath}${separator}material${separator}apkout${separator}res${separator}drawable${separator}icon.png"
            )


            val useJvm = result.getTable("build")!!.getBoolean("useJvm") ?: false
            if (useJvm) {
                writeDependenciesJson(result)
                //                result.getTable("build")!!.getString("sdk.dir")?.let {  }
                setSdkPath("${sdkPath}${separator}components${separator}android-sdk")

                executeCommand(
                    "gradlew.bat assembleRelease",
                    File("${sdkPath}${separator}components${separator}project${separator}jvm")
                ) { output ->
                    println(output)
                }
                println("导入JVM插件")
                copyFile(
                    "${sdkPath}${separator}components${separator}project${separator}jvm${separator}app${separator}build${separator}outputs${separator}apk${separator}release${separator}app-release-unsigned.apk",
                    "${buildPath}${separator}release${separator}main${separator}assets${separator}bp.apk"
                )
            }


            zipDirectory(buildPath + "${separator}release", buildPath + "${separator}release.zip")
        }
    }

    fun setConfig() {

        Log.logger?.info("更新构建配置...")
        baseDir.let {
            val path = "${sdkPath}${separator}material${separator}apkout${separator}apktool.yml"
            val source: Path = Paths.get("${baseDir}${separator}cheese.toml")
            val result: TomlParseResult = Toml.parse(source)
            result.errors().forEach(Consumer { error: TomlParseError ->
                System.err.println(
                    error.toString()
                )
            })
            copyFile(
                "$baseDir${separator}build" + "${separator}release.zip",
                "${sdkPath}${separator}material${separator}apkout${separator}assets${separator}release.zip"
            )
            val appTable = result.getTable("app")
            val apktoolYml = YamlUtils.loadYml(path) ?: return
            excludeLib(result, apktoolYml)
            rmLib(result, apktoolYml)
            apktoolYml.packageInfo!!.renameManifestPackage =
                appTable?.getString("package") ?: "net.codeocean.cheese.demo"
            apktoolYml.versionInfo!!.versionName = appTable?.getString("version") ?: "0.0.1"
            modifyStringInXmlFile(
                "${sdkPath}${separator}material${separator}apkout${separator}res${separator}values${separator}strings.xml",
                "app_name",
                appTable?.getString("name") ?: "cheese"
            )
            modifyStringInXmlFile(
                "${sdkPath}${separator}material${separator}apkout${separator}res${separator}values${separator}strings.xml",
                "base_ser_desc",
                appTable?.getString("accessible_service_desc") ?: "Cheese自动化测试框架"
            )
            modifyStringInXmlFile(
                "${sdkPath}${separator}material${separator}apkout${separator}res${separator}values${separator}strings.xml",
                "base_ser_name",
                appTable?.getString("accessible_service_name") ?: "cheese"
            )
            modifyStringInXmlFile(
                "${sdkPath}${separator}material${separator}apkout${separator}res${separator}values${separator}strings.xml",
                "input_name",
                appTable?.getString("inputmethod_service_name") ?: "Cheese输入法"
            )
            val xmlFilePath = "${sdkPath}${separator}material${separator}apkout${separator}AndroidManifest.xml"
            parseAndReplaceXml(xmlFilePath, appTable?.getString("package") ?: "net.codeocean.cheese.demo")
            apktoolYml.resourcesAreCompressed = true
            YamlUtils.write(apktoolYml, path)
        }
    }

    fun buildPKG() {
        Log.logger?.info("开始打包...")
        APKToolUtils.builderApk(
            outputDir,
            File("${baseDir}${separator}build${separator}apk${separator}app.apk")
        )
        Log.logger?.info("打包完毕...")
    }

    fun singAPK() {

        baseDir.let {

            val tomlPath = "$it${separator}cheese.toml"

            val result: TomlParseResult = Toml.parse(Paths.get(tomlPath))
            result.errors().forEach(Consumer { error: TomlParseError ->
                System.err.println(
                    error.toString()
                )
            })


            Log.logger?.info("开始签名、对齐等收尾工作...")


            val align = zipalign(
                "${baseDir}${separator}build${separator}apk${separator}app.apk",
                "${baseDir}${separator}build${separator}apk${separator}app-sign-align.apk"
            )

            Log.logger?.info("对齐结果：$align")

            apkSing(
                "${sdkPath}${separator}material${separator}jks${separator}cheese.jks",
                "${baseDir}${separator}build${separator}apk${separator}app-sign-align.apk",
                "key0",
                "123456",
                "123456"
            )

            Log.logger?.info("签名、对齐等收尾工作完毕...")
        }


    }

    fun zipalign(inputPath: String, outputPath: String): Boolean {
        return try {
            RandomAccessFile(inputPath, "r").use { inputFile ->
                FileOutputStream(outputPath).use { outputFile ->
                    ZipAlign.alignZip(inputFile, outputFile, 4) // 硬编码4字节对齐
                    true // 成功返回true
                }
            }
        } catch (e: Exception) {
            false // 任何异常都返回false
        }
    }


    fun rmLib(result: TomlParseResult, yml: net.codeocean.cheese.utils.APKToolYml) {
        val appTable = result.getTable("build")
        val tomlArray = appTable?.getArray("ndk") ?: return

        val targetList = listOf("x86_64", "x86", "arm64-v8a", "armeabi-v7a")
        val tomlValues = mutableSetOf<String>()
        for (i in 0 until tomlArray!!.size()) {
            val item = tomlArray[i].toString() // 确保转换为字符串
            tomlValues.add(item)

        }
        val missingValues = targetList.filter { it !in tomlValues }
        if (missingValues.isNotEmpty()) {
            missingValues.forEach { missingValue ->
                when (missingValue) {
                    "x86_64" -> {
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}${missingValue}").deleteRecursively()
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libyolov8ncnn.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libpython3.8.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libopencv_java4.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libmlkit_google_ocr_pipeline.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libjavet-node-android.v.3.1.0.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libc++_shared.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libBugly_Native.so")
                    }

                    "x86" -> {
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}${missingValue}").deleteRecursively()
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libyolov8ncnn.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libpython3.8.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libopencv_java4.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libmlkit_google_ocr_pipeline.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libjavet-node-android.v.3.1.0.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libc++_shared.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libBugly_Native.so")
                    }

                    "arm64-v8a" -> {
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}${missingValue}").deleteRecursively()
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libyolov8ncnn.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libpython3.8.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libopencv_java4.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libmlkit_google_ocr_pipeline.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libjavet-node-android.v.3.1.0.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libc++_shared.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libBugly_Native.so")
                    }

                    "armeabi-v7a" -> {
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}${missingValue}").deleteRecursively()
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libyolov8ncnn.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libpython3.8.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libopencv_java4.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libmlkit_google_ocr_pipeline.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libjavet-node-android.v.3.1.0.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libc++_shared.so")
                        yml.doNotCompress.remove("lib${separator}${missingValue}${separator}libBugly_Native.so")
                    }

                    else -> println("no...")
                }
            }
        }

    }

    fun excludeLib(result: TomlParseResult, yml: net.codeocean.cheese.utils.APKToolYml) {
        val appTable = result.getTable("build")
        val tomlArray = appTable?.getArray("excludeLib") ?: return

        // 如果 tomlArray 为 null，则直接返回


        val targetList = listOf("yolo", "opencv", "ocr")
        val tomlValues = mutableSetOf<String>()
        for (i in 0 until tomlArray.size()) {
            val item = tomlArray[i].toString() // 确保转换为字符串
            tomlValues.add(item)

        }
        val missingValues = targetList.filter { it in tomlValues }
        if (missingValues.isNotEmpty()) {
            missingValues.forEach { missingValue ->
                when (missingValue) {
                    "yolo" -> {
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86_64${separator}libyolov8ncnn.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86${separator}libyolov8ncnn.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}armeabi-v7a${separator}libyolov8ncnn.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}arm64-v8a${separator}libyolov8ncnn.so").delete()
                        yml.doNotCompress.remove("lib${separator}x86_64${separator}libyolov8ncnn.so")
                        yml.doNotCompress.remove("lib${separator}x86${separator}libyolov8ncnn.so")
                        yml.doNotCompress.remove("lib${separator}armeabi-v7a${separator}libyolov8ncnn.so")
                        yml.doNotCompress.remove("lib${separator}arm64-v8a${separator}libyolov8ncnn.so")
                    }

                    "opencv" -> {
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86_64${separator}libopencv_java4.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86${separator}libopencv_java4.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}armeabi-v7a${separator}libopencv_java4.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}arm64-v8a${separator}libopencv_java4.so").delete()
                        yml.doNotCompress.remove("lib${separator}x86_64${separator}libopencv_java4.so")
                        yml.doNotCompress.remove("lib${separator}x86${separator}libopencv_java4.so")
                        yml.doNotCompress.remove("lib${separator}armeabi-v7a${separator}libopencv_java4.so")
                        yml.doNotCompress.remove("lib${separator}arm64-v8a${separator}libopencv_java4.so")
                    }

                    "mlkitocr" -> {
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86_64${separator}libmlkit_google_ocr_pipeline.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86${separator}libmlkit_google_ocr_pipeline.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}armeabi-v7a${separator}libmlkit_google_ocr_pipeline.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}arm64-v8a${separator}libmlkit_google_ocr_pipeline.so").delete()
                        yml.doNotCompress.remove("lib${separator}x86_64${separator}libmlkit_google_ocr_pipeline.so")
                        yml.doNotCompress.remove("lib${separator}x86${separator}libmlkit_google_ocr_pipeline.so")
                        yml.doNotCompress.remove("lib${separator}armeabi-v7a${separator}libmlkit_google_ocr_pipeline.so")
                        yml.doNotCompress.remove("lib${separator}arm64-v8a${separator}libmlkit_google_ocr_pipeline.so")
                    }

                    "ddddocr" -> {
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86_64${separator}libddddocr.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86${separator}libddddocr.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}armeabi-v7a${separator}libddddocr.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}arm64-v8a${separator}libddddocr.so").delete()
                        yml.doNotCompress.remove("lib${separator}x86_64${separator}libddddocr.so")
                        yml.doNotCompress.remove("lib${separator}x86${separator}libddddocr.so")
                        yml.doNotCompress.remove("lib${separator}armeabi-v7a${separator}libddddocr.so")
                        yml.doNotCompress.remove("lib${separator}arm64-v8a${separator}libddddocr.so")
                    }

                    "paddleocr" -> {
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86_64${separator}libonnxruntime4j_jni.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86${separator}libonnxruntime4j_jni.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}armeabi-v7a${separator}libonnxruntime4j_jni.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}arm64-v8a${separator}libonnxruntime4j_jni.so").delete()
                        yml.doNotCompress.remove("lib${separator}x86_64${separator}libonnxruntime4j_jni.so")
                        yml.doNotCompress.remove("lib${separator}x86${separator}libonnxruntime4j_jni.so")
                        yml.doNotCompress.remove("lib${separator}armeabi-v7a${separator}libonnxruntime4j_jni.so")
                        yml.doNotCompress.remove("lib${separator}arm64-v8a${separator}libonnxruntime4j_jni.so")
                    }

                    "onnx" -> {
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86_64${separator}libonnxruntime.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86${separator}libonnxruntime.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}armeabi-v7a${separator}libonnxruntime.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}arm64-v8a${separator}libonnxruntime.so").delete()
                        yml.doNotCompress.remove("lib${separator}x86_64${separator}libonnxruntime.so")
                        yml.doNotCompress.remove("lib${separator}x86${separator}libonnxruntime.so")
                        yml.doNotCompress.remove("lib${separator}armeabi-v7a${separator}libonnxruntime.so")
                        yml.doNotCompress.remove("lib${separator}arm64-v8a${separator}libonnxruntime.so")

                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86_64${separator}libonnxruntime4j_jni.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86${separator}libonnxruntime4j_jni.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}armeabi-v7a${separator}libonnxruntime4j_jni.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}arm64-v8a${separator}libonnxruntime4j_jni.so").delete()
                        yml.doNotCompress.remove("lib${separator}x86_64${separator}libonnxruntime4j_jni.so")
                        yml.doNotCompress.remove("lib${separator}x86${separator}libonnxruntime4j_jni.so")
                        yml.doNotCompress.remove("lib${separator}armeabi-v7a${separator}libonnxruntime4j_jni.so")
                        yml.doNotCompress.remove("lib${separator}arm64-v8a${separator}libonnxruntime4j_jni.so")

                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86_64${separator}libddddocr.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}x86${separator}libddddocr.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}armeabi-v7a${separator}libddddocr.so").delete()
                        File("${sdkPath}${separator}material${separator}apkout${separator}lib${separator}arm64-v8a${separator}libddddocr.so").delete()
                        yml.doNotCompress.remove("lib${separator}x86_64${separator}libddddocr.so")
                        yml.doNotCompress.remove("lib${separator}x86${separator}libddddocr.so")
                        yml.doNotCompress.remove("lib${separator}armeabi-v7a${separator}libddddocr.so")
                        yml.doNotCompress.remove("lib${separator}arm64-v8a${separator}libddddocr.so")

                    }


                    else -> println("no...")
                }
            }
        }

    }

    fun parseAndReplaceXml(filePath: String, newAuthorities: String) {
        val file = File(filePath)
        if (!file.exists()) {
            println("文件不存在: $filePath")
            return
        }

        try {
            // 创建 DocumentBuilderFactory 实例
            val factory = DocumentBuilderFactory.newInstance()
            // 创建 DocumentBuilder 实例
            val builder = factory.newDocumentBuilder()
            // 解析 XML 文件并生成 Document 对象
            val document = builder.parse(file)

            // 规范化 XML 文档
            document.documentElement.normalize()


            val permissionList = document.getElementsByTagName("permission")
            for (i in 0 until permissionList.length) {
                val node = permissionList.item(i)
                if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                    val element = node as org.w3c.dom.Element
                    // 替换 authorities 属性值
                    val authorities = element.getAttribute("android:name")
                    if (authorities == "net.codeocean.cheese.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION") {
                        element.setAttribute("android:name", "$newAuthorities.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
                    }
                }
            }
            val uses_permissionList = document.getElementsByTagName("uses-permission")
            for (i in 0 until uses_permissionList.length) {
                val node = uses_permissionList.item(i)
                if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                    val element = node as org.w3c.dom.Element
                    // 替换 authorities 属性值
                    val authorities = element.getAttribute("android:name")
                    if (authorities == "net.codeocean.cheese.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION") {
                        element.setAttribute("android:name", "$newAuthorities.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION")
                    }
                }
            }

            // 获取 provider 元素
            val nodeList = document.getElementsByTagName("provider")
            for (i in 0 until nodeList.length) {
                val node = nodeList.item(i)
                if (node.nodeType == org.w3c.dom.Node.ELEMENT_NODE) {
                    val element = node as org.w3c.dom.Element
                    // 替换 authorities 属性值
                    val authorities = element.getAttribute("android:authorities")
                    if (authorities == "net.codeocean.cheese.mlkitinitprovider") {
                        element.setAttribute("android:authorities", "$newAuthorities.mlkitinitprovider")
                    } else if (authorities == "net.codeocean.cheese.fx.provider") {
                        element.setAttribute("android:authorities", "$newAuthorities.fx.provider")
                    } else if (authorities == "net.codeocean.cheese.androidx-startup") {
                        element.setAttribute("android:authorities", "$newAuthorities.androidx-startup")
                    } else if (authorities == "net.codeocean.cheese.provider") {
                        element.setAttribute("android:authorities", "$newAuthorities.provider")
                    }

                }
            }

            // 保存修改后的 XML 文件
            val transformer = TransformerFactory.newInstance().newTransformer()


            // 将 DOMSource 和 StreamResult 作为参数传递给 transform 方法
            val source = DOMSource(document)
            val result = StreamResult(file) // 直接覆盖原文件

            transformer.transform(source, result)

            println("XML 文件已更新: $filePath")
        } catch (e: Exception) {
            println("处理文件时出错: ${e.message}")
            e.printStackTrace()
        }
    }

    fun modifyStringInXmlFile(
        filePath: String,
        nameToModify: String,
        newValue: String
    ) {
        val xmlFile = File(filePath)
        if (!xmlFile.exists()) {
            println("文件不存在: ${xmlFile.absolutePath}")
            return
        }

        // 创建 DocumentBuilderFactory 和 DocumentBuilder
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()

        // 解析 XML 文件
        val document = builder.parse(xmlFile)

        // 获取根元素
        val root = document.documentElement

        // 修改指定 name 的 value
        val nodes = root.getElementsByTagName("string")
        var modified = false
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node is Element && node.getAttribute("name") == nameToModify) {
                node.textContent = newValue
                modified = true
                break
            }
        }

        if (!modified) {
            println("没有找到 name 为 '$nameToModify' 的元素")
            return
        }

        // 将修改后的 XML 写回文件
        val transformer = TransformerFactory.newInstance().newTransformer()
        val source = DOMSource(document)
        val result = StreamResult(xmlFile)
        transformer.transform(source, result)

    }

    fun apkSing(jks: String, apk: String, alias: String, ks: String, key: String) {
        val clazz = com.android.apksigner.ApkSignerTool::class.java
        val signMethod: Method = clazz.getDeclaredMethod("sign", Array<String>::class.java)
        signMethod.isAccessible = true
        val commandLineArgs = arrayOf(
            "--ks", jks,
            "--ks-key-alias", alias,
            "--ks-pass", "pass:${ks}",
            "--key-pass", "pass:${key}",
            apk
        )

        try {
            signMethod.invoke(null, commandLineArgs)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun extractResourceToLocal(path: String, destination: File): String {
        val classLoader = BuildProject::class.java.classLoader
        val resource = classLoader.getResourceAsStream(path)
        return if (resource != null) {
            // 确保目标文件所在的目录存在
            destination.parentFile?.mkdirs()

            // 使用 Files.copy 从 InputStream 复制到目标文件
            Files.copy(resource, destination.toPath(), StandardCopyOption.REPLACE_EXISTING)
            resource.close()

            // 返回目标文件的绝对路径
            destination.absolutePath
        } else {
            println("sb")
            // 如果资源未找到，返回一个提示信息
            "Resource not found: $path"
        }
    }

}