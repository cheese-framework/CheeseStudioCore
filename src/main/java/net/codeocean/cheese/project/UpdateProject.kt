package net.codeocean.cheese.project

import net.codeocean.cheese.utils.FileUtils
import net.codeocean.cheese.utils.FileUtils.copyFile
import org.tomlj.Toml
import org.tomlj.TomlParseError
import org.tomlj.TomlParseResult
import java.io.File
import java.io.File.separator
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer

class UpdateProject(val sdkPath: String, val baseDir: String) {
    init {

        if (hasFolder(baseDir + separator + "node_modules", "cheese-node")) {
            File(baseDir + separator + "node_modules" + separator + "cheese-node").delete()
            FileUtils.copyDirectory(
                sdkPath + separator + "components" + separator + "project" + separator + "node_modules" + separator + "cheese-node",
                baseDir + separator + "node_modules" + separator + "cheese-node"
            )
        } else if (hasFolder(baseDir + separator + "node_modules", "cheese-js")) {
            File(baseDir + separator + "node_modules" + separator + "cheese-js").delete()
            FileUtils.copyDirectory(
                sdkPath + separator + "components" + separator + "project" + separator + "node_modules" + separator + "cheese-js",
                baseDir + separator + "node_modules" + separator + "cheese-js"
            )
        } else {

            val source: Path = Paths.get("${baseDir}${separator}cheese.toml")
            val result: TomlParseResult = Toml.parse(source)
            result.errors().forEach(Consumer { error: TomlParseError ->
                System.err.println(
                    error.toString()
                )
            })
            val bindings = result.getString("bindings")
            if (bindings?.contains("node") == true) {
                File(baseDir + separator + "node_modules" + separator + "cheese-node").delete()
                FileUtils.copyDirectory(
                    sdkPath + separator + "components" + separator + "project" + separator + "node_modules" + separator + "cheese-node",
                    baseDir + separator + "node_modules" + separator + "cheese-node"
                )
            } else if (bindings?.toString() == "js" || bindings?.toString() == "ts") {
                File(baseDir + separator + "node_modules" + separator + "cheese-js").delete()
                FileUtils.copyDirectory(
                    sdkPath + separator + "components" + separator + "project" + separator + "node_modules" + separator + "cheese-js",
                    baseDir + separator + "node_modules" + separator + "cheese-js"
                )
            } else {
                println("更新失败，项目配置文件非最新版！")
            }

        }

    }

    fun hasFolder(parentPath: String, folderName: String): Boolean {
        val parentDir = File(parentPath)
        if (!parentDir.exists() || !parentDir.isDirectory) {
            // 如果指定路径不存在或不是文件夹，直接返回 false
            return false
        }
        val targetFolder = File(parentDir, folderName)
        // 判断目标文件夹是否存在且为目录
        return targetFolder.exists() && targetFolder.isDirectory
    }

}