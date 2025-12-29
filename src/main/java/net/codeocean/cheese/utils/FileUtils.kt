package net.codeocean.cheese.utils

import org.json.JSONObject
import java.io.*
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.security.KeyStore
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


object FileUtils {
    fun convertPath(path: String): String {
        val separator = File.separator
        // 替换所有的正斜杠和反斜杠为当前平台的路径分隔符
        return path.replace("/", separator).replace("\\", separator)
    }

    fun getFileMD5(filePath: String): String {

        val md5 = MessageDigest.getInstance("MD5")

        // 读取文件并更新 MessageDigest
        Files.newInputStream(Paths.get(filePath)).use { inputStream ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                md5.update(buffer, 0, bytesRead)
            }
        }

        // 返回 MD5 哈希值
        return md5.digest().joinToString("") { "%02x".format(it) }
    }


    fun getJksCertificateMd5(jksPath: String, jksPassword: String, alias: String): String? {
        try {
            // 加载 JKS 密钥库
            val keyStore = KeyStore.getInstance("JKS")
            val keyStoreInputStream = FileInputStream(jksPath)
            keyStore.load(keyStoreInputStream, jksPassword.toCharArray())
            keyStoreInputStream.close()

            // 获取指定别名的证书
            val certificate = keyStore.getCertificate(alias) as X509Certificate

            // 使用 MessageDigest 计算 MD5
            val messageDigest = MessageDigest.getInstance("MD5")
            val certBytes = certificate.encoded
            messageDigest.update(certBytes)

            val md5Bytes = messageDigest.digest()
            val sb = StringBuilder()
            for (b in md5Bytes) {
                sb.append(String.format("%02X", b))
            }

            return sb.toString().lowercase() // 转换为小写 MD5 格式
        } catch (e: java.lang.Exception) {
            e.printStackTrace()
        }
        return null
    }


    fun copyDirectory(sourceDir: String, targetDir: String) {
        val sourcePath = Paths.get(sourceDir)
        val targetPath = Paths.get(targetDir)
        if (!Files.exists(sourcePath)) {
           return
        }
        // 确保目标目录存在
        if (!Files.exists(targetPath)) {
            Files.createDirectories(targetPath)
        }

        // 遍历源目录下的所有文件和子目录
        Files.walk(sourcePath).forEach { source ->
            val target = targetPath.resolve(sourcePath.relativize(source))
            try {
                if (Files.isDirectory(source)) {
                    // 如果是目录，创建对应的目标目录
                    if (!Files.exists(target)) {
                        Files.createDirectories(target)
                    }
                } else {
                    // 如果是文件，复制到目标目录
                    Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }
    fun createDirectoryIfNotExists(directoryPath: String) {
        val directory = File(directoryPath)
        if (!directory.exists()) {
            directory.mkdirs()
        }
    }


    fun copyFile(sourcePath: String, destinationPath: String) {
        val sourceFile = File(sourcePath)
        val destinationFile = File(destinationPath)

        if (!sourceFile.exists()) {
            throw IOException("Source file does not exist: $sourcePath")
        }
        try {
            Files.copy(sourceFile.toPath(), destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } catch (e: IOException) {
            e.printStackTrace()
            throw e
        }
    }
    fun replaceCharInFile(filePath: String, oldChar: String, newChar: String) {
        val file = File(filePath)

        if (!file.exists()) {
            throw IllegalArgumentException("文件不存在: $filePath")
        }

        val tempFilePath = "$filePath.tmp"
        val tempFile = File(tempFilePath)

        file.bufferedReader().use { reader ->
            tempFile.bufferedWriter().use { writer ->
                reader.forEachLine { line ->
                    val modifiedLine = line.replace(oldChar, newChar)
                    writer.write(modifiedLine)
                    writer.newLine()
                }
            }
        }

        // 替换原文件
        Files.delete(Paths.get(filePath))
        Files.move(Paths.get(tempFilePath), Paths.get(filePath))
    }
    fun getJsonValue(filePath: String, key: String): Any? {
        try {
            // 读取文件内容
            val file = File(filePath)
            val reader = FileReader(file)

            // 解析 JSON 数据
            val jsonString = reader.readText()
            val jsonObject = JSONObject(jsonString)

            // 获取指定键的值
            return jsonObject.opt(key)  // opt() 会返回 null 如果键不存在
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    fun isFileSizeLessThan10MB(filePath: String): Boolean {
        val file = File(filePath)

        // 检查文件是否存在
        if (file.exists()) {
            // 获取文件大小（字节）
            val fileSizeInBytes = file.length()

            // 转换为 MB
            val fileSizeInMB = fileSizeInBytes.toDouble() / (1024 * 1024)

            // 判断文件大小是否小于 10MB
            return fileSizeInMB < 10
        }
        return false
    }

    fun zipDirectory(sourceDirPath: String, zipFilePath: String) {
        val sourceDir = File(sourceDirPath)
        if (!sourceDir.isDirectory) {
            throw IllegalArgumentException("Source path must be a directory")
        }

        ZipOutputStream(FileOutputStream(zipFilePath)).use { zipOut ->
            sourceDir.walkTopDown().forEach { file ->
                val relativePath = sourceDir.toURI().relativize(file.toURI()).path
                if (file.isFile) {
                    val zipEntry = ZipEntry(relativePath)
                    zipOut.putNextEntry(zipEntry)
                    file.inputStream().use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
            }
        }
    }
    fun mergeFiles(outputFilePath: String, vararg partFilePaths: String) {
        val outputFile = File(outputFilePath)
        if (outputFile.exists()) {
            println("Output file $outputFilePath already exists. It will be overwritten.")
        }

        FileOutputStream(outputFile).use { fos ->
            for (partFilePath in partFilePaths) {
                val partFile = File(partFilePath)
                if (!partFile.exists() || !partFile.isFile) {
                    throw IOException("Part file $partFilePath does not exist or is not a valid file")
                }

                partFile.inputStream().use { input ->
                    copyStream(input, fos)
                }
            }
        }

        // 删除部分文件
        for (partFilePath in partFilePaths) {
            val partFile = File(partFilePath)
            if (!partFile.delete()) {
                println("Failed to delete part file $partFilePath")
            }
        }
    }
    private const val BUFFER_SIZE = 1024 * 1024 // 1 MB
    private fun copyStream(input: InputStream, output: FileOutputStream) {
        val buffer = ByteArray(BUFFER_SIZE)
        var bytesRead: Int
        while (input.read(buffer).also { bytesRead = it } != -1) {
            output.write(buffer, 0, bytesRead)
        }
    }
    fun splitFile(filePath: String, numParts: Int) {
        if (numParts <= 0) throw IllegalArgumentException("Number of parts must be greater than zero")

        val file = File(filePath)
        if (!file.exists() || !file.isFile) throw IOException("File does not exist or is not a valid file")

        val fileSize = file.length()
        val partSize = fileSize / numParts
        val remainder = (fileSize % numParts).toInt()

        RandomAccessFile(file, "r").use { raf ->
            for (i in 0 until numParts) {
                val partFile = File("${filePath}.part$i")
                val start = (i * partSize).toLong()
                val end = if (i == numParts - 1) fileSize else start + partSize + if (i < remainder) 1 else 0
                val bufferSize = (end - start).toInt()

                RandomAccessFile(partFile, "rw").use { partRaf ->
                    val buffer = ByteArray(bufferSize)
                    raf.seek(start)
                    raf.readFully(buffer)
                    partRaf.write(buffer)
                }
            }
        }
    }
}
