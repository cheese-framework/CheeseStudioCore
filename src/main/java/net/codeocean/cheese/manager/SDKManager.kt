package net.codeocean.cheese.manager

import net.codeocean.cheese.utils.FileUtils.getJsonValue
import net.codeocean.cheese.utils.HttpUtils.getData
import java.io.File.separator

class SDKManager(val sdk:String) {

    fun compareVersions():Boolean{
       val remoteVersion= getData("http://cheese.codeocean.net/version.json")?.get("version").toString()
       val localVersion= getJsonValue("${sdk}${separator}config.json","version").toString()
        // 将版本号分割为数组
        val remoteVersionParts = remoteVersion.split(".")
        val localVersionParts = localVersion.split(".")

        // 比较每个版本号部分（例如主版本号、次版本号）
        for (i in 0 until minOf(remoteVersionParts.size, localVersionParts.size)) {
            val remotePart = remoteVersionParts[i].toIntOrNull() ?: continue
            val localPart = localVersionParts[i].toIntOrNull() ?: continue
            if (remotePart > localPart) {
                return true  // 远程版本大于本地版本，需要更新
            } else if (remotePart < localPart) {
                return false  // 本地版本大于远程版本，不需要更新
            }
        }

        return remoteVersionParts.size > localVersionParts.size

    }



}