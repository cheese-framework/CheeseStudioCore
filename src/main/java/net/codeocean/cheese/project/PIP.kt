package net.codeocean.cheese.project

import net.codeocean.cheese.utils.FileUtils.copyDirectory
import net.codeocean.cheese.utils.FileUtils.copyFile
import net.codeocean.cheese.utils.FileUtils.createDirectoryIfNotExists
import net.codeocean.cheese.utils.FileUtils.zipDirectory
import net.codeocean.cheese.utils.TerminalUtils.executeCommand
import com.google.gson.GsonBuilder
import org.tomlj.TomlParseResult
import java.io.File
import java.io.File.separator

class PIP(val sdkPath: String, val baseDir: String, val buildPath: String) {

    fun build(path: String, result: TomlParseResult) {
        setSdkPath(path)
        writeDependenciesJson(result)
        buildPy()
    }

    fun build1(path: String, result: TomlParseResult) {
        setSdkPath(path)
        writeDependenciesJson(result)
        updatePy()
    }


    fun setSdkPath(path: String) {
        val file = File("${sdkPath}/components/project/pip/local.properties")
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
        val dependenciesArray = result.getArray("pip")

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
        val json = gson.toJson(mapOf("install" to dependenciesList))
            // Gson 默认格式微调（精确匹配你的要求）
            .replace("\": [", "\": [")  // 移除冒号后多余空格
            .replace("  \"", "    \"")  // 调整缩进为2空格

        // 4. 写入文件
        val outputFile = File("${sdkPath}/components/project/pip/app/dependencies.json").apply {
            parentFile.mkdirs()
        }

        try {
            outputFile.writeText(json)
        } catch (e: Exception) {
            System.err.println("写入失败: ${e.message}")
        }
    }

    fun updatePy() {
        executeCommand(
            "gradlew.bat assembleRelease",
            File("${sdkPath}${separator}components${separator}project${separator}pip")
        ) { output ->
            println(output)
        }
        copyDirectory(
            "${sdkPath}${separator}components${separator}project${separator}pip${separator}app${separator}build${separator}python${separator}pip${separator}release${separator}common",
            "${baseDir}${separator}packages"
        )


    }

    fun buildPy() {
        executeCommand(
            "gradlew.bat assembleRelease",
            File("${sdkPath}${separator}components${separator}project${separator}pip")
        ) { output ->
            println(output)
        }

        File("${sdkPath}${separator}components${separator}project${separator}pip${separator}app${separator}build${separator}pip${separator}assets").deleteRecursively()


        createDirectoryIfNotExists("${sdkPath}${separator}components${separator}project${separator}pip${separator}app${separator}build${separator}pip${separator}assets")
        copyDirectory(
            "${sdkPath}${separator}components${separator}project${separator}pip${separator}app${separator}build${separator}intermediates${separator}assets${separator}release${separator}mergeReleaseAssets${separator}chaquopy",
            "${sdkPath}${separator}components${separator}project${separator}pip${separator}app${separator}build${separator}pip${separator}assets${separator}chaquopy"
        )

        zipDirectory(
            "${sdkPath}${separator}components${separator}project${separator}pip${separator}app${separator}build${separator}pip",
            "${buildPath}${separator}debug${separator}main${separator}assets${separator}pip"
        )

    }

}