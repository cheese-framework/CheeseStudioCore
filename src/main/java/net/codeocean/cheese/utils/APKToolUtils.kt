package net.codeocean.cheese.utils

import brut.androlib.ApkBuilder
import brut.androlib.ApkDecoder
import brut.androlib.Config
import brut.directory.ExtFile
import java.io.File

object APKToolUtils {


    // 极简解码配置
    private fun createDecodeConfig(): Config {
        return Config.getDefaultConfig().apply {
            // 核心配置（保持资源解码但限制范围）
/*            setDecodeResources(256)       // 必须为1才能修改资源*/
//            setForceDecodeManifest(1)   // 必须解码清单文件
            setDecodeSources(0)         // 跳过代码解码
//            useAapt2 = true
//            noCrunch = true
//            copyOriginalFiles = true
//            // 通过资源类型限制
//            decodeAssets = 0            // 跳过assets
//            analysisMode = true         // 启用分析模式（减少操作）
        }
    }

    // 极简构建配置
    private fun createBuildConfig(): Config {
        return Config.getDefaultConfig().apply {
            forceBuildAll = false
            useAapt2 = true
            noCrunch = true
        }
    }

    /**
     * 快速解码APK（仅解压清单和资源）
     */
    fun decodeApk(apkFile: File, outputDir: File) {
        ApkDecoder(createDecodeConfig(), ExtFile(apkFile)).decode(outputDir)
    }

    /**
     * 快速构建APK
     */
    fun builderApk(inputDir: File, outputApk: File) {
        ApkBuilder(createBuildConfig(), ExtFile(inputDir)).build(outputApk)
    }


}