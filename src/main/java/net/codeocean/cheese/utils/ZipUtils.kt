package net.codeocean.cheese.utils

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

object ZipUtils {
    fun unzipFileToSameDirectory(zipFilePath: String) {
        val zipFile = File(zipFilePath)
        val zip = ZipFile(zipFile)
        val outputDir = zipFile.parentFile

        val entries = zip.entries()
        while (entries.hasMoreElements()) {
            val entry = entries.nextElement()
            val entryFile = File(outputDir, entry.name)
            val entryDir = entryFile.parentFile

            if (!entryDir.exists()) {
                entryDir.mkdirs()
            }

            if (!entry.isDirectory) {
                val inputStream = zip.getInputStream(entry)
                val outputStream = FileOutputStream(entryFile)
                inputStream.copyTo(outputStream)
                inputStream.close()
                outputStream.close()
            }
        }
        zip.close()
    }


    fun unzip(zipFilePath: String, destDirectory: String) {
        val destDir = File(destDirectory)
        if (!destDir.exists()) {
            destDir.mkdirs()  // 创建目标目录（如果不存在的话）
        }

        try {
            // 打开 ZIP 文件
            FileInputStream(zipFilePath).use { fis ->
                ZipInputStream(fis).use { zis ->
                    var entry: ZipEntry?
                    // 遍历 ZIP 文件中的每个文件
                    while (zis.nextEntry.also { entry = it } != null) {
                        val filePath = destDirectory + File.separator + entry!!.name
                        val newFile = File(filePath)

                        // 如果是目录，则创建目录
                        if (entry!!.isDirectory) {
                            newFile.mkdirs()
                        } else {
                            // 如果是文件，则创建文件并写入内容
                            newFile.parentFile.mkdirs()  // 创建父目录
                            FileOutputStream(newFile).use { fos ->
                                val buffer = ByteArray(1024)
                                var length: Int
                                while (zis.read(buffer).also { length = it } > 0) {
                                    fos.write(buffer, 0, length)
                                }
                            }
                        }
                        zis.closeEntry()
                    }
                }
            }
            println("解压完成！")
        } catch (e: IOException) {
            println("解压失败: ${e.message}")
        }
    }


}