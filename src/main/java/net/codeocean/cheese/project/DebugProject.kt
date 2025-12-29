package net.codeocean.cheese.project

import net.codeocean.cheese.server.Log
import net.codeocean.cheese.utils.FileUtils
import net.codeocean.cheese.utils.FileUtils.convertPath
import net.codeocean.cheese.utils.FileUtils.copyDirectory
import net.codeocean.cheese.utils.FileUtils.copyFile
import net.codeocean.cheese.utils.FileUtils.createDirectoryIfNotExists
import net.codeocean.cheese.utils.FileUtils.getJksCertificateMd5
import net.codeocean.cheese.utils.FileUtils.zipDirectory
import net.codeocean.cheese.utils.HttpUtils
import net.codeocean.cheese.utils.TerminalUtils.executeCommand
import net.codeocean.cheese.utils.ZipUtils
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.google.gson.GsonBuilder
import org.tomlj.Toml
import org.tomlj.TomlArray
import org.tomlj.TomlParseError
import org.tomlj.TomlParseResult
import java.io.File
import java.io.File.separator
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.function.Consumer


class DebugProject(val sdkPath: String, val baseDir: String) {


    fun extractResourceToLocal(path: String, destination: File): String {
        val classLoader = DebugProject::class.java.classLoader
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

    fun renameFile(sourcePath: String, targetPath: String) {
        val source = Path.of(sourcePath)
        val target = Path.of(targetPath)

        if (Files.exists(source)) {
            Files.move(source, target, StandardCopyOption.REPLACE_EXISTING)
            println("文件已重命名为: $targetPath")
        } else {
            println("源文件不存在: $sourcePath")
        }
    }

    fun pip() {
        val tomlPath = "$baseDir${separator}cheese.toml"
        val buildPath = "$baseDir${separator}build"
        val result: TomlParseResult = Toml.parse(Paths.get(tomlPath))
        result.errors().forEach(Consumer { error: TomlParseError ->
            System.err.println(
                error.toString()
            )
        })

        PIP(sdkPath, baseDir, buildPath).build1("${sdkPath}${separator}components${separator}android-sdk", result)
    }


    fun hot() {
        val mainPath = "$baseDir${separator}src${separator}main"
        val tomlPath = "$baseDir${separator}cheese.toml"
        val buildPath = "$baseDir${separator}build"
        val node_modules = "$baseDir${separator}node_modules"
        val result: TomlParseResult = Toml.parse(Paths.get(tomlPath))
        result.errors().forEach(Consumer { error: TomlParseError ->
            System.err.println(
                error.toString()
            )
        })
        val bindings = result.getString("bindings")
        if (!File("${sdkPath}${separator}material${separator}jks${separator}cheese.jks").exists()) {
            createDirectoryIfNotExists("${sdkPath}${separator}material${separator}jks")
            extractResourceToLocal(
                "jks/cheese.jks",
                File("${sdkPath}${separator}material${separator}jks${separator}cheese.jks")
            )
        }
        Thread {
            File(buildPath).deleteRecursively()

            if (bindings == "python") {
                copyDirectory("$mainPath${separator}py", "$buildPath${separator}debug${separator}main${separator}py")
                renameFile(
                    "$buildPath${separator}debug${separator}main${separator}py${separator}main.py",
                    "$buildPath${separator}debug${separator}main${separator}py${separator}main_.py"
                )
            } else {
                if (bindings?.contains("ts") == true) {
                    val command = "tsc"
                    executeCommand(command, File(baseDir)) { output ->
                        println(output)
                    }



                    copyDirectory(
                        "$buildPath${separator}js",
                        "$buildPath${separator}debug${separator}main${separator}js"
                    )

                } else {
                    val buildTable = result.getTable("build")
                    val build_toolsTable = buildTable!!.getString("protection")


                    if (build_toolsTable != null) {

                        if (build_toolsTable == "obfuscator") {
                            val command = "vite build" // 替换为你想要执行的命令
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
                                "$buildPath${separator}debug${separator}main${separator}js"
                            )
                        }


                    } else {

                        copyDirectory(
                            "$mainPath${separator}js",
                            "$buildPath${separator}debug${separator}main${separator}js"
                        )
                    }


                }
                copyDirectory(node_modules, buildPath + "${separator}debug${separator}node_modules")
            }

            copyDirectory(
                "$mainPath${separator}assets",
                buildPath + "${separator}debug${separator}main${separator}assets"
            )
            val ui = result.getString("ui")
            if (ui == "xml") {
                copyDirectory("$mainPath${separator}ui", "$buildPath${separator}debug${separator}main${separator}ui")
            } else {
                executeCommand("npm run build", File(convertPath("$mainPath${separator}ui"))) { output ->
                    Log.logger?.info(output)
                }
                copyDirectory(
                    convertPath("$mainPath${separator}ui/dist"),
                    "$buildPath${separator}debug${separator}main${separator}ui"
                )

            }



            copyFile(tomlPath, buildPath + "${separator}debug${separator}cheese.toml")
            zipDirectory(buildPath + "${separator}debug", buildPath + "${separator}cheese.hot")
        }.start()
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

    fun run() {
        val mainPath = "$baseDir${separator}src${separator}main"
        val tomlPath = "$baseDir${separator}cheese.toml"
        val buildPath = "$baseDir${separator}build"
        val node_modules = "$baseDir${separator}node_modules"
//        val Lib = "$baseDir${separator}Lib"
        val result: TomlParseResult = Toml.parse(Paths.get(tomlPath))
        result.errors().forEach(Consumer { error: TomlParseError ->
            System.err.println(
                error.toString()
            )
        })


        val bindings = result.getString("bindings")
        val platform = result.getString("platform")

        if (!File("${sdkPath}${separator}material${separator}jks${separator}cheese.jks").exists()) {
            FileUtils.createDirectoryIfNotExists("${sdkPath}${separator}material${separator}jks")
            extractResourceToLocal(
                "jks/cheese.jks",
                File("${sdkPath}${separator}material${separator}jks${separator}cheese.jks")
            )
        }
        val key = getJksCertificateMd5(
            "${sdkPath}${separator}material${separator}jks${separator}cheese.jks",
            "123456",
            "key0"
        )
        Thread {
            File(buildPath).deleteRecursively()

            copyDirectory(
                "$mainPath${separator}assets",
                buildPath + "${separator}debug${separator}main${separator}assets"
            )

            if (bindings == "python") {
                println(platform)

                if (platform == "ios") {
                    copyDirectory(
                        "$mainPath${separator}python",
                        "$buildPath${separator}debug${separator}main${separator}python"
                    )
                    copyDirectory(
                        "$baseDir${separator}packages",
                        "$buildPath${separator}debug${separator}main${separator}python${separator}packages"
                    )
                } else {
                    copyDirectory(
                        "$mainPath${separator}python",
                        "${sdkPath}/components/project/pip/app/src/main/python"
                    )
                    createDirectoryIfNotExists("${sdkPath}/components/project/pip/app/src/main/python${separator}cheese_core")
                    copyDirectory(
                        "$baseDir${separator}packages${separator}cheese_core",
                        "${sdkPath}/components/project/pip/app/src/main/python${separator}cheese_core"
                    )
                    PIP(sdkPath, baseDir, buildPath).build(
                        "${sdkPath}${separator}components${separator}android-sdk",
                        result
                    )
                }


            } else {
                if (bindings?.contains("ts") == true) {
                    val command = "tsc"
                    executeCommand(command, File(baseDir)) { output ->
                        println(output)
                    }

                    val buildTable = result.getTable("build")


                    copyDirectory(
                        "$buildPath${separator}js",
                        "$buildPath${separator}debug${separator}main${separator}js"
                    )
                } else {
                    val buildTable = result.getTable("build")
                    val build_toolsTable = buildTable!!.getString("protection")


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
                                "$buildPath${separator}debug${separator}main${separator}js"
                            )
                        }


                    } else {
                        copyDirectory(
                            "$mainPath${separator}js",
                            "$buildPath${separator}debug${separator}main${separator}js"
                        )

                    }


                }
                copyDirectory(node_modules, buildPath + "${separator}debug${separator}node_modules")
            }

            copyFile(tomlPath, buildPath + "${separator}debug${separator}cheese.toml")

            val useJvm = result.getTable("build")!!.getBoolean("useJvm") ?: false
            if (useJvm) {
                writeDependenciesJson(result)
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
                    "${buildPath}${separator}debug${separator}main${separator}assets${separator}bp.apk"
                )
            }


            zipDirectory(buildPath + "${separator}debug", buildPath + "${separator}debug.zip")

        }.start()


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

    fun runUi() {
        val mainPath = "$baseDir${separator}src${separator}main"
        val tomlPath = "$baseDir${separator}cheese.toml"
        val buildPath = "$baseDir${separator}build"
        val node_modules = "$baseDir${separator}node_modules"
        val result: TomlParseResult = Toml.parse(Paths.get(tomlPath))
        result.errors().forEach(Consumer { error: TomlParseError ->
            System.err.println(
                error.toString()
            )
        })
        File(buildPath).deleteRecursively()
        val ui = result.getString("ui")

        if (ui == "xml") {
            copyDirectory("$mainPath${separator}ui", "$buildPath${separator}debug${separator}main${separator}ui")
        } else if (ui == "jui") {
            copyDirectory("$mainPath${separator}ui", "$buildPath${separator}debug${separator}main${separator}ui")
        } else {
            executeCommand("npm run build", File(convertPath("$mainPath${separator}ui"))) { output ->
                Log.logger?.info(output)
            }
            copyDirectory(
                convertPath("$mainPath${separator}ui/dist"),
                "$buildPath${separator}debug${separator}main${separator}ui"
            )

        }
        copyFile(tomlPath, buildPath + "${separator}debug${separator}cheese.toml")
        zipDirectory(buildPath + "${separator}debug", buildPath + "${separator}debug.zip")
    }

}