package net.codeocean.cheese.project

import net.codeocean.cheese.server.Log
import net.codeocean.cheese.utils.FileUtils
import net.codeocean.cheese.utils.FileUtils.convertPath
import java.io.File
import java.io.File.separator
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardOpenOption

class CreateProject(config: Map<String, String>, val sdkPath: String, val output: String) : FreemarkerConfiguration() {
    val projectConfig = initializeProjectConfig(config)

    init {
        if (projectConfig.getPlatform() =="ios"){
            writeFile("", output, "cheese.toml", "cheese_ios.ftl", projectConfig)
        }else{
            writeFile("", output, "cheese.toml", "cheese.ftl", projectConfig)
        }

//        val type = projectConfig.getLanguage()
        val type = projectConfig.getBindings()


        when {
            type!!.contains("ts") -> {
                writeFile("", output, "tsconfig.json", "tsconfig.ftl", projectConfig)
            }

            type.contains("js") -> {
                writeFile("", output, "jsconfig.json", "jsconfig.ftl", projectConfig)
            }

            else -> println("Unknown type")
        }

        generateProjectCode()

    }


    fun generateProjectCode() {
        val project_path =
            "${sdkPath}${separator}components${separator}project${separator}template${separator}script"
        val node_modules =
            "${sdkPath}${separator}components${separator}project${separator}node_modules${separator}cheese-node"
        val node_modules1 =
            "${sdkPath}${separator}components${separator}project${separator}node_modules${separator}cheese-js"
        val node_modules2 =
            "${sdkPath}${separator}components${separator}project${separator}node_modules_ios"
        val python_venv =
            "${sdkPath}${separator}components${separator}project${separator}packages"
        val python_ios_venv =
            "${sdkPath}${separator}components${separator}project${separator}packages_ios"
        val js_dev =
            "${sdkPath}${separator}components${separator}project${separator}template${separator}dev${separator}dev-js"
        val package_json =
            "${sdkPath}${separator}components${separator}project${separator}template${separator}script${separator}package_node.json"
        val package_json1 =
            "${sdkPath}${separator}components${separator}project${separator}template${separator}script${separator}package_js.json"
        val fileCls = if (projectConfig.getBindings()?.contains("js") == true) {
            ".js"
        } else if (projectConfig.getBindings()?.contains("ts") == true) {
            ".ts"
        } else {
            ".py"
        }

        val ml = if (projectConfig.getBindings()?.contains("js") == true) {
            "js"
        } else if (projectConfig.getBindings()?.contains("ts") == true) {
            "ts"
        } else {
            "python"
        }

        val ms = if (projectConfig.getBindings()?.contains("node") == true) {
            "node"
        } else if (projectConfig.getBindings()?.contains("js")==true||projectConfig.getBindings()?.contains("ts")==true)  {
            "js"
        } else {
            "python"
        }


            if (projectConfig.getPlatform()=="ios"){
                FileUtils.copyDirectory("$project_path${separator}${DataState.getProjectConfigVO().getUi()}", output)
            }else{
                FileUtils.copyDirectory("$project_path${separator}${DataState.getProjectConfigVO().getUi()}", output)
            }






        if (projectConfig.getBindings()?.contains("node") == true){
            FileUtils.copyDirectory(node_modules, output + separator + "node_modules"+separator+"cheese-node")
            FileUtils.copyFile(package_json, output + "${separator}package.json")
        }else if (projectConfig.getBindings()?.contains("js")==true||projectConfig.getBindings()?.contains("ts")==true){
            if(projectConfig.getPlatform()=="ios"){
                FileUtils.copyDirectory(node_modules2, output + separator + "node_modules")
            }else{
                FileUtils.copyDirectory(node_modules1, output + separator + "node_modules"+separator+"cheese-js")
            }

            FileUtils.copyFile(package_json1, output + "${separator}package.json")
            FileUtils.copyDirectory(js_dev, "$output${separator}dev")
        }else {
            if(projectConfig.getPlatform()=="ios"){
                FileUtils.copyDirectory(python_ios_venv, "$output${separator}packages")
            }else{
                FileUtils.copyDirectory(python_venv, "$output${separator}packages")
            }

        }
        writeFile(
            "${separator}src${separator}main${separator}${ml}",
            output,
            "main$fileCls",
            "ui_${projectConfig.getUi()}_${ms}_${projectConfig.getPlatform()}.ftl",
            projectConfig
        )




        if (projectConfig.getBindings()?.contains("ts") == true) {

            Log.logger?.info(
                """
                您选的开发语言为typescript，请检查环境:
                   nodejs https://nodejs.org
                   npm install -g typescript
                    """.trimIndent()
            )
        }

        if (projectConfig.getUi()!!.contains("vue")) {
            Log.logger?.info(
                """
                运行指令安装UI依赖:
                   cd  ${convertPath(output)}${separator}src${separator}main${separator}ui
                   npm install
                   npm run dev
                    """.trimIndent()
            )
        }

    }

    /**
     * 使用模板生成文件并写入指定目录
     */
    fun writeFile(packageName: String, entryPath: String, name: String, ftl: String, dataModel: Any) {
        try {
            val template = super.getTemplate(ftl)
            val content = generateTemplateContent(template, dataModel)
            val filePath = prepareFilePath(packageName, entryPath, name)
            writeContentToFile(filePath, content)
        } catch (e: Exception) {
            Log.logger?.error(e.message, e)
        }
    }

    /**
     * 根据包名和路径构建完整目录路径，并返回目标文件路径
     */
    private fun prepareFilePath(packageName: String, entryPath: String, name: String): Path {
        val dirPath = createPackageDir(packageName, entryPath)
        return dirPath.resolve(name)
    }

    /**
     * 创建包目录（如不存在）
     */
    private fun createPackageDir(packageName: String, entryPath: String): Path {
        val dirPath = Paths.get("$entryPath/${packageName.replace(".", "/")}")
        if (Files.notExists(dirPath)) {
            Files.createDirectories(dirPath)
        }
        return dirPath
    }

    /**
     * 使用模板引擎生成内容
     */
    private fun generateTemplateContent(template: freemarker.template.Template, dataModel: Any): ByteArray {
        return StringWriter().use { writer ->
            template.process(dataModel, writer)
            writer.toString().toByteArray(Charsets.UTF_8)
        }
    }

    /**
     * 写入文件内容
     */
    private fun writeContentToFile(filePath: Path, content: ByteArray) {
        Files.write(filePath, content, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING)
    }

    /**
     * 初始化项目配置
     */
    private fun initializeProjectConfig(config: Map<String, String>): ProjectConfigVO {
        val projectConfig = DataState.getProjectConfigVO()
        projectConfig.setProjectname(config["name"])
        projectConfig.setPkg(config["pkg"])
        projectConfig.setBindings(config["bindings"])
        projectConfig.setPlatform(config["platform"])

        val cleanedUi = config["ui"]?.substringBefore('[') ?: ""
        projectConfig.setUi(cleanedUi)
        return projectConfig
    }
}
