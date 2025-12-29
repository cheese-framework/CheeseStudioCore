package net.codeocean.cheese.utils



import org.yaml.snakeyaml.DumperOptions
import org.yaml.snakeyaml.Yaml
import org.yaml.snakeyaml.nodes.MappingNode
import org.yaml.snakeyaml.representer.Representer
import java.io.BufferedWriter
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.charset.StandardCharsets



object YamlUtils {

    data class PackageInfo(
        var forcedPackageId: String? = null,
        var renameManifestPackage: String? = null
    )


    data class UsesFramework(
        var ids: List<Int>? = null,
        var tag: String? = null
    )

    data class VersionInfo(
        var versionCode: String? = null,
        var versionName: String? = null
    )

    /**
     * 加载 apktool.yml 到 ApktoolYml 类中
     *
     * @param ymlPath apktool.yml 文件路径
     * @return ApktoolYml 实例
     */
    fun loadYml(ymlPath: String): APKToolYml? {
        FileInputStream(ymlPath).use { fis ->
            return try {
                Yaml().loadAs(fis, APKToolYml::class.java)
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }
        }
    }

    /**
     * 将 ApktoolYml 实例写入 apktool.yml 文件
     *
     * @param data ApktoolYml 实例
     * @param ymlPath apktool.yml 文件路径
     */
    fun write(data: APKToolYml, ymlPath: String) {
        FileOutputStream(ymlPath).use { fos ->
            OutputStreamWriter(fos, StandardCharsets.UTF_8).use { outputStreamWriter ->
                BufferedWriter(outputStreamWriter).use { writer ->
                    val options = DumperOptions().apply {
                        defaultFlowStyle = DumperOptions.FlowStyle.BLOCK
                    }
                    val representer = CustomRepresenter(options)
                    try {
                        val yaml = Yaml(representer, options)
                        yaml.dump(data, writer)
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            }
        }
    }

    // Custom Representer to maintain property order
    class CustomRepresenter(dumperOptions: DumperOptions) : Representer(dumperOptions) {
        init {
            this.addClassTag(APKToolYml::class.java, org.yaml.snakeyaml.nodes.Tag.MAP)
        }

        // 保持原始属性顺序
        override fun representJavaBean(properties: MutableSet<org.yaml.snakeyaml.introspector.Property>?, javaBean: Any?): MappingNode? {
            val sortedProperties = LinkedHashMap<org.yaml.snakeyaml.introspector.Property, Any>()
            properties?.forEach { property ->
                val value = property.get(javaBean)
                sortedProperties[property] = value
            }
            return super.representJavaBean(sortedProperties.keys, javaBean)
        }

    }
    /**
     * 合并 apktool.yml 文件
     *
     * @param srcYmlPath 需要合并的 apktool.yml 文件
     * @param targetYmlPath 合并到的目标 apktool.yml 文件
     */
    fun merge(srcYmlPath: String, targetYmlPath: String) {
        val mergeYml = loadYml(srcYmlPath) ?: return
        val targetYml = loadYml(targetYmlPath) ?: return
        targetYml.addDoNotCompress(mergeYml.doNotCompress ?: emptyList())
        targetYml.putUnknownFiles(mergeYml.unknownFiles ?: emptyMap())
        write(targetYml, targetYmlPath)
    }
}

