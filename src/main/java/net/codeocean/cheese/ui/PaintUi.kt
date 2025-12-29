package net.codeocean.cheese.ui


import coco.cheese.ide.test.ColorTest.findMultiColors
import coco.cheese.ide.test.ColorTest.parseColor
import coco.cheese.ide.test.ImagesTest.findImgByResize
import coco.cheese.ide.test.ImagesTest.findImgBySift
import net.codeocean.cheese.Env.cvInit
import net.codeocean.cheese.Env.sdkPath
import net.codeocean.cheese.server.Log
import net.codeocean.cheese.server.clientSessions
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.json.JSONArray
import org.json.JSONObject
import org.opencv.core.Core
import org.opencv.core.CvType
import org.opencv.core.Mat
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.*
import java.awt.geom.AffineTransform
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte
import java.awt.image.DataBufferInt
import java.io.File
import java.io.File.separator
import java.util.*
import java.util.regex.Pattern
import javax.imageio.ImageIO
import javax.swing.*
import javax.swing.event.DocumentEvent
import javax.swing.event.DocumentListener
import javax.swing.filechooser.FileNameExtensionFilter
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min


class DeviceImagePanel : JPanel() {
    var image: BufferedImage? = null
    private val imageLabel = JLabel().apply {
        horizontalAlignment = JLabel.CENTER // 使图片居中显示
    }

    init {
        layout = BorderLayout() // 设置布局为 BorderLayout
        val scrollPane = JScrollPane(imageLabel).apply {
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED

        }
        add(scrollPane, BorderLayout.CENTER)
    }

    fun loadImage(bufferedImage: BufferedImage) {
        image = bufferedImage
        imageLabel.icon = ImageIcon(image)
        revalidate()
        repaint()
    }
}

object PaintUi {
    val deviceImagePanel = DeviceImagePanel()
    var h: Int? = null
    var w: Int? = null
    var imagePanel: PaintUi.ImagePanel? = null
    fun createTransparentImageIcon(width: Int, height: Int): ImageIcon {
        // 创建一个具有透明背景的 BufferedImage
        val bufferedImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB)

        // 使用 BufferedImage 创建 ImageIcon
        return ImageIcon(bufferedImage)
    }

    val customPanel = CustomPanel()

    val buttonPanel12 = JPanel().apply {
        layout = BorderLayout()
        border = BorderFactory.createEmptyBorder(0, 0, 10, 10) // 顶部边距为0，底部边距为10

        var font = Font("Arial", Font.PLAIN, 14)

        // 创建按钮并添加到面板
        val buttons = listOf(
            JButton("截图").apply {
                font = font
                addActionListener {
                    GlobalScope.launch(Dispatchers.Default) {  // 在主线程中更新 UI
                        clientSessions["device"]?.send("4")
                    }
                }
            },
            JButton("保存图片").apply {
                font = font
                addActionListener { saveImage(imagePanel!!) }
            },
        )

        // 创建一个面板来放置按钮
        val buttonPanel = JPanel().apply {
            layout = GridLayout(1, 2, 3, 3) // 一行四个按钮，间距5
            buttons.forEach { button -> add(button) }
        }

        // 将按钮面板添加到顶部，将图片面板添加到中心
        add(buttonPanel, BorderLayout.NORTH) // 按钮面板放在顶部
        add(deviceImagePanel, BorderLayout.CENTER) // 图片面板放在中间
    }

    fun getFrame(): JFrame {
        // 创建主窗口
        val frame = JFrame("Cheese 图色工具").apply {
            defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
            setSize(700, 500) // 调整窗口大小以适应需要
            // 检查图像文件是否存在
            val imagePath = createTransparentImageIcon(700, 500)
            imagePanel = ImagePanel(imagePath)

            // 创建按钮面板
            val buttonPanel = createButtonPanel(imagePanel as ImagePanel)

            val scrollPane = JScrollPane(customPanel)
            scrollPane.verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_ALWAYS
            scrollPane.horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED

            val shtPanel = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, buttonPanel, buttonPanel12).apply {
                dividerSize = 3 // 设置分隔条的大小
                resizeWeight = 0.4 // 初始按钮面板占 70% 的高度
            }


            // 创建一个 JSplitPane 用于分隔按钮面板和文本区域
            val rightPanel = JSplitPane(JSplitPane.VERTICAL_SPLIT, shtPanel, scrollPane).apply {
                dividerSize = 3 // 设置分隔条的大小
                resizeWeight = 0.6 // 初始按钮面板占 70% 的高度
            }

            // 创建一个 JSplitPane 用于分隔图片面板和按钮面板
            val splitPane = JSplitPane(JSplitPane.HORIZONTAL_SPLIT, JScrollPane(imagePanel), rightPanel).apply {
                resizeWeight = 0.8 // 初始图片面板占 80% 的宽度
//            isOneTouchExpandable = true // 显示展开/折叠按钮
                dividerSize = 3 // 设置分隔条的大小
            }
            // 将 JSplitPane 添加到主窗口的内容面板
            contentPane.add(splitPane)

            // 不设置 isVisible = true
        }
        return frame
    }

    class ColorPanel : JPanel() {
        private var color: Color? = null

        init {
            // 监听面板隐藏事件
            addComponentListener(object : ComponentAdapter() {
                override fun componentHidden(e: ComponentEvent?) {
                    closeRender()
                }
            })
        }

        // 更新颜色方法
        fun updateColor(hexColor: String) {
            SwingUtilities.invokeLater {
                try {
                    // 将 Hex 颜色转换为 Color 对象
                    color = Color.decode(hexColor)
                    repaint() // 触发重绘，更新颜色
                } catch (e: NumberFormatException) {
                    println("Invalid color hex: $hexColor")
                }
            }
        }

        // 清空内容
        fun closeRender() {
            color = null
            repaint() // 清空颜色块
        }

        // 重写 paintComponent 方法绘制颜色块
        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            color?.let {
                g.color = it
                g.fillRect(0, 0, width, height) // 绘制填充矩形
            }
        }
    }

    class CustomPanel : JPanel(GridBagLayout()) {
        private val rowComponents = mutableMapOf<Int, List<Component>>()
        val checkBoxes = mutableMapOf<Int, JCheckBox>() // 用于跟踪每一行的 JCheckBox

        init {
            val gbc = GridBagConstraints().apply {
                insets = Insets(5, 5, 5, 5)
                fill = GridBagConstraints.HORIZONTAL
            }

            fun addComponent(
                component: Component,
                gridx: Int,
                gridy: Int,
                weightx: Double = 0.0,
                fill: Int = GridBagConstraints.NONE
            ) {
                gbc.gridx = gridx
                gbc.gridy = gridy
                gbc.weightx = weightx
                gbc.fill = fill
                add(component, gbc)
            }

            fun applyColorOffset(baseColorHex: String, offsetColor: String): String {
                // 移除偏色增量前缀并解析
                val offsetHex = offsetColor.removePrefix("0x")

                // 解析基础颜色的 RGB 分量
                val baseR = Integer.parseInt(baseColorHex.substring(1, 3), 16)
                val baseG = Integer.parseInt(baseColorHex.substring(3, 5), 16)
                val baseB = Integer.parseInt(baseColorHex.substring(5, 7), 16)


                // 解析偏色增量的 RGB 分量
                val offsetR = Integer.parseInt(offsetHex.substring(0, 2), 16)
                val offsetG = Integer.parseInt(offsetHex.substring(2, 4), 16)
                val offsetB = Integer.parseInt(offsetHex.substring(4, 6), 16)


                // 计算新的 RGB 分量，应用增量
                val newR = (baseR + offsetR - 128).coerceIn(0, 255)
                val newG = (baseG + offsetG - 128).coerceIn(0, 255)
                val newB = (baseB + offsetB - 128).coerceIn(0, 255)


                // 返回新的颜色的十六进制表示
                return String.format("#%02X%02X%02X", newR, newG, newB)
            }

            fun createTextField(rowIndex: Int, placeholder: String) = JTextField(10).apply {
                this.placeholder = placeholder
                this.document.addDocumentListener(object : DocumentListener {
                    override fun insertUpdate(e: DocumentEvent) {
                        onTextChange()
                    }

                    override fun removeUpdate(e: DocumentEvent) {
                        onTextChange()
                    }

                    override fun changedUpdate(e: DocumentEvent) {
                        onTextChange()
                    }

                    fun onTextChange() {
                        if (validateInput(text)) {
                            val textField = getRowComponents(rowIndex)?.get(1) as JTextField
                            textField.text = applyColorOffset(textField.text, text)
                            val colorPanel = customPanel.getRowComponents(rowIndex)?.get(2) as ColorPanel
                            colorPanel.updateColor(textField.text)
                        }

                    }

                    fun validateInput(input: String): Boolean {
                        val hexPattern = Pattern.compile("^0x[0-9A-Fa-f]{6}$")

                        return if (hexPattern.matcher(input).matches()) {
                            // 解析十六进制数
                            val hexValue = input.substring(2).toIntOrNull(16)
                            if (hexValue != null && hexValue in 0..0xFFFFFF) {
                                true
                            } else {
                                false
                            }
                        } else {
                            println("Invalid format: $input")
                            false
                        }
                    }
                })

            }


            fun createTextField(placeholder: String) = JTextField(10).apply {
                this.placeholder = placeholder
                this.document.addDocumentListener(object : DocumentListener {
                    override fun insertUpdate(e: DocumentEvent) {
                        onTextChange()
                    }

                    override fun removeUpdate(e: DocumentEvent) {
                        onTextChange()
                    }

                    override fun changedUpdate(e: DocumentEvent) {
                        onTextChange()
                    }

                    private fun onTextChange() {


                    }
                })
            }

            fun deleteButton(rowIndex: Int) = JButton("删除").apply {
                addActionListener {
                    val textField = getRowComponents(rowIndex)?.get(1) as JTextField
                    textField.text = ""
                    val textField1 = getRowComponents(rowIndex)?.get(3) as JTextField
                    textField1.text = ""
                    val textField2 = getRowComponents(rowIndex)?.get(4) as JTextField
                    textField2.text = ""
                    val colorPanel = customPanel.getRowComponents(rowIndex)?.get(2) as ColorPanel
                    colorPanel.closeRender()
                    val jCheckBox = getRowComponents(rowIndex)?.get(5) as JCheckBox
                    jCheckBox.setSelected(false)
                }
            }

            fun addRow(rowIndex: Int) {
                val checkBox = JCheckBox()
                checkBoxes[rowIndex] = checkBox // 存储每行的 JCheckBox

                val components = listOf(
                    JLabel(rowIndex.toString()),
                    createTextField("颜色值"),
                    ColorPanel(),
                    createTextField(rowIndex, "偏色"),
                    createTextField("坐标"),
                    checkBox,
                    deleteButton(rowIndex)
                )
                rowComponents[rowIndex] = components

                components.forEachIndexed { index, component ->
                    addComponent(
                        component,
                        index,
                        rowIndex,
                        if (index == 1 || index == 3) 1.0 else 0.0,
                        if (index == 1 || index == 3) GridBagConstraints.HORIZONTAL else GridBagConstraints.NONE
                    )
                }

            }

            fun addButton(rowIndex: Int) {

                val clearButton = JButton("清除所有颜色")
                clearButton.addActionListener(object : ActionListener {
                    override fun actionPerformed(e: ActionEvent) {

                        for (i in 1 until 9) {
                            val textField = customPanel.getRowComponents(i)?.get(1) as JTextField
                            val textField1 = customPanel.getRowComponents(i)?.get(3) as JTextField
                            val textField2 = customPanel.getRowComponents(i)?.get(4) as JTextField
                            val colorPanel = customPanel.getRowComponents(i)?.get(2) as ColorPanel
                            val jCheckBox = customPanel.getRowComponents(i)?.get(5) as JCheckBox
                            if (textField.text.isNotEmpty()) {
                                textField.text = ""
                                textField1.text = ""
                                textField2.text = ""
                                colorPanel.closeRender()
                                jCheckBox.setSelected(false)

                            }
                        }


                    }
                })
                val components = listOf(
                    JLabel(""),
                    clearButton
                )



                components.forEachIndexed { index, component ->
                    addComponent(
                        component,
                        index,
                        rowIndex,
                        if (index == 1 || index == 3) 1.0 else 0.0,
                        if (index == 1 || index == 3) GridBagConstraints.HORIZONTAL else GridBagConstraints.NONE
                    )
                }

            }
            addButton(0)
            addRow(1)
            addRow(2)
            addRow(3)
            addRow(4)
            addRow(5)
            addRow(6)
            addRow(7)
            addRow(8)
            addRow(9)

            // 监听所有 JCheckBox 状态变化
            checkBoxes.values.forEach { checkBox ->
                checkBox.addActionListener {

                }
            }
        }

        fun getRowComponents(rowIndex: Int): List<Component>? {
            return rowComponents[rowIndex]
        }
    }

    var JTextField.placeholder: String
        get() = UIManager.getLookAndFeelDefaults().getString("TextField.placeholderText")
        set(value) {
            // 没有内置支持的占位符，需要自定义实现
            val placeholderText = value
            val placeholderComponent = object : JComponent() {
                override fun paintComponent(g: Graphics) {
                    super.paintComponent(g)
                    if (text.isEmpty()) {
                        g.color = Color.GRAY
                        g.drawString(placeholderText, 5, g.fontMetrics.ascent)
                    }
                }
            }
            placeholderComponent.isOpaque = false
            placeholderComponent.size = preferredSize
            add(placeholderComponent, BorderLayout.CENTER)

            // 监听文本框的变化以显示和隐藏占位符
            addCaretListener {
                placeholderComponent.repaint()
            }
            addFocusListener(object : FocusAdapter() {
                override fun focusGained(e: FocusEvent) {
                    placeholderComponent.isVisible = false
                }

                override fun focusLost(e: FocusEvent) {
                    placeholderComponent.isVisible = text.isEmpty()
                }
            })

            placeholderComponent.isVisible = text.isEmpty()
        }

    fun createButtonPanel(imagePanel: ImagePanel): JPanel {
        // 创建主面板，使用网格布局来组织按钮、输入框和下拉框
        val mainPanel = JPanel().apply {
            layout = GridBagLayout()
            border = BorderFactory.createEmptyBorder(10, 10, 10, 10) // 添加边距
        }
        val gbc = GridBagConstraints().apply {
            fill = GridBagConstraints.HORIZONTAL
            insets = Insets(5, 5, 5, 5)
            weightx = 1.0
            weighty = 1.0

        }


        val textArea = RSyntaxTextArea(20, 50).apply {
            lineWrap = false
            wrapStyleWord = false
            border = BorderFactory.createLineBorder(Color.GRAY)
            syntaxEditingStyle = SyntaxConstants.SYNTAX_STYLE_JAVASCRIPT
            font = Font("宋体", Font.PLAIN, 14)
        }


        val textAreaScroll = JScrollPane(textArea).apply {
            horizontalScrollBarPolicy = JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED
            verticalScrollBarPolicy = JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED
        }


        data class Point(val x: Int, val y: Int)

        fun stringToPoint(str: String): Point {
            val (x, y) = str.split(',').map { it.trim().toInt() }
            return Point(x, y)
        }

        fun calculateDifference(value1: Int, value2: Int): Int {
            return value2 - value1
        }

        // 创建下拉框
        val comboBox = JComboBox(arrayOf("findMultiColors", "findImgByResize", "findImgBySift")).apply {
            preferredSize = Dimension(150, 30)
            font = Font("Arial", Font.PLAIN, 14)
        }

        fun parseParams(params: String): List<String> {
            val result = mutableListOf<String>()
            var current = StringBuilder()
            var bracketLevel = 0
            var inQuotes = false

            for (char in params) {
                when {
                    char == '"' -> inQuotes = !inQuotes
                    char == '[' -> if (!inQuotes) bracketLevel++
                    char == ']' -> if (!inQuotes) bracketLevel--
                    char == ',' && bracketLevel == 0 && !inQuotes -> {
                        result.add(current.toString().trim())
                        current = StringBuilder()
                        continue
                    }
                }
                current.append(char)
            }

            if (current.isNotEmpty()) {
                result.add(current.toString().trim())
            }

            return result
        }

        val generateButton = JButton("生成").apply {
            preferredSize = Dimension(150, 40)
            font = Font("宋体", Font.PLAIN, 14) // 使用宋体字体，普通样式，大小为14
            addActionListener {
                when (comboBox.selectedItem) {
                    "findMultiColors" -> {
                        var o = false
                        val sb = StringBuilder()
                        var ozb: Point? = null
                        var lastSelectedKey: Int? = null
                        // 遍历所有的 CheckBox
                        customPanel.checkBoxes.entries.forEach { entry ->
                            val (key, checkBox) = entry
                            if (checkBox.isSelected) {
                                lastSelectedKey = key // 更新最后选中的 CheckBox 的键
                            }
                        }
                        customPanel.checkBoxes.values.forEach { checkBox ->
                            val row = customPanel.checkBoxes.entries.find { it.value == checkBox }?.key
                            val textField = customPanel.getRowComponents(row!!)?.get(1) as JTextField
                            val textField2 = customPanel.getRowComponents(row)?.get(4) as JTextField
                            val isLast = row == lastSelectedKey
                            if (checkBox.isSelected) {
                                if (!o) {
                                    sb.append("\"${textField.text}\", [")
                                    ozb = stringToPoint(textField2.text)
                                    o = true
                                } else if (isLast) {
                                    val point = stringToPoint(textField2.text)
                                    sb.append(
                                        "[${calculateDifference(ozb!!.x, point.x)},${
                                            calculateDifference(
                                                ozb!!.y,
                                                point.y
                                            )
                                        },\"${textField.text}\"]"
                                    )
                                } else {
                                    val point = stringToPoint(textField2.text)
                                    sb.append(
                                        "[${calculateDifference(ozb!!.x, point.x)},${
                                            calculateDifference(
                                                ozb!!.y,
                                                point.y
                                            )
                                        },\"${textField.text}\"],"
                                    )
                                }
                            }
                        }
                        sb.append("]")
                        textArea.text = """
                    const core = require('cheese-js');
                    const base = core.base;
                    const color = core.color;
                    const recordscreen = core.recordscreen;
                    if (recordscreen.requestPermission(3)) {
                        let bit = recordscreen.captureScreen(3, 0, 0, 0, -1)
                        console.log("坐标：",  color.findMultiColors(bit, ${sb},{
                        "maxDistance":30
                    }))
                        base.release(bit)
                    }

                """.trimIndent()

                    }

                    "findImgByResize" -> {


                        textArea.text = """
                            const core = require('cheese-js');
                            const base = core.base;
                            const recordscreen = core.recordscreen;
                            const converters = core.converters;
                            const image  = core.image ;
                            if (recordscreen.requestPermission(3)) {
                                let target = converters.streamToBitmap(converters.assetsToStream("image.png"))
                                let bit = recordscreen.captureScreen(3, 0, 0, 0, -1)
                                console.log(image.findImgByResize(bit,target,0.5,${w},${h}))
                                base.release(target)
                                base.release(bit)
                            }
                     
                        """.trimIndent()


                    }

                    "findImgBySift" -> {

                        textArea.text = """
                            const core = require('cheese-js');
                            const base = core.base;
                            const recordscreen = core.recordscreen;
                            const converters = core.converters;
                            const image = core.image;
                            if (recordscreen.requestPermission(3)) {
                                let target = converters.streamToBitmap(converters.assetsToStream("image.png"))
                                let bit = recordscreen.captureScreen(3, 0, 0, 0, -1)
                                console.log(image.findImgBySift(bit,target,0.5))
                                base.release(target)
                                base.release(bit)
                            }
                     
                        """.trimIndent()

                    }

                    else -> println("无效的天数")
                }


            }
        }

        fun jsonToMap(jsonString: String): Map<String, Any> {
            val jsonObject = JSONObject(jsonString)
            val map = mutableMapOf<String, Any>()
            jsonObject.keys().forEach { key ->
                val value = jsonObject.get(key.toString())
                map[key.toString()] = value
            }

            return map
        }

        fun convertBGRtoARGB(bgrMat: Mat): Mat {
            val argbMat = Mat()
            Imgproc.cvtColor(bgrMat, argbMat, Imgproc.COLOR_BGR2RGBA)
            return argbMat
        }

        fun bufferedImageToMat(image: BufferedImage): Mat {
            val mat = Mat(image.height, image.width, CvType.CV_8UC3)
            val data = IntArray(image.width * image.height)

            // 获取图像的 RGB 数据
            image.getRGB(0, 0, image.width, image.height, data, 0, image.width)

            // 将 RGB 数据转换为 Mat 格式
            val byteData = ByteArray(image.width * image.height * 3)
            for (i in data.indices) {
                byteData[i * 3] = (data[i] and 0xFF).toByte()           // B
                byteData[i * 3 + 1] = ((data[i] shr 8) and 0xFF).toByte()  // G
                byteData[i * 3 + 2] = ((data[i] shr 16) and 0xFF).toByte() // R
            }

            mat.put(0, 0, byteData)
            return mat
        }


        val generateButton1 = JButton("测试").apply {
            preferredSize = Dimension(150, 40)
            font = Font("宋体", Font.PLAIN, 14) // 使用宋体字体，普通样式，大小为14
            addActionListener {
                if (!File("${sdkPath}${separator}components${separator}opencv${separator}x64${separator}${Core.NATIVE_LIBRARY_NAME}.dll").exists()) {

                    Log.logger?.error("OPENCV 环境不存在 ")
                    return@addActionListener
                }
                if (!cvInit) {
                    if (File("${sdkPath}${separator}components${separator}opencv${separator}x64${separator}${Core.NATIVE_LIBRARY_NAME}.dll").exists()) {
                        System.load("${sdkPath}${separator}components${separator}opencv${separator}x64${separator}${Core.NATIVE_LIBRARY_NAME}.dll")
                    } else {
                        Log.logger?.error("OPENCV 环境不存在 ")
                        return@addActionListener
                    }
                    cvInit = true
                }
                val str =
                    textArea.text
                if (str.isNotEmpty()) {
                    when (comboBox.selectedItem) {
                        "findMultiColors" -> {
                            val regex = Regex("""colors\.findMultiColors\(([^)]+)\)""")
                            val match = regex.find(str)
                            if (match != null) {
                                val params = match.groupValues[1]
                                val parameters = parseParams(params)[2]
                                val jsonArray = JSONArray(parameters)
                                val pathList = mutableListOf<IntArray>()
                                // 遍历外层的 JSONArray
                                for (i in 0 until jsonArray.length()) {
                                    val innerArray = jsonArray.getJSONArray(i)

                                    // 提取数据
                                    val x = innerArray.getInt(0)
                                    val y = innerArray.getInt(1)
                                    val color = parseColor(innerArray.getString(2))

                                    // 添加到 pathList
                                    pathList.add(intArrayOf(x, y, color))
                                }

                                val paths = pathList.toTypedArray()
                                val options = mapOf(
                                    "region" to arrayOf(50, 100, 200, 150),
                                    "maxDistance" to jsonToMap(parseParams(params)[3]).get("maxDistance") as Int
                                )
                                val result = findMultiColors(
                                    convertBGRtoARGB(bufferedImageToMat(imagePanel.image)),
                                    parseParams(params)[1].trim('"'),
                                    paths,
                                    options
                                )
                                if (result != null) {
                                    imagePanel.setRedPoint(result.x.toInt(), result.y.toInt())
                                } else {
                                    println("No matching points found.")
                                }
                            }
                        }

                        "findImgByResize" -> {
                            val regex = Regex("""images\.findImgByResize\(([^)]+)\)""")
                            val match = regex.find(str)
                            if (match != null) {
                                val params = match.groupValues[1] // This will be the parameters as a string
                                val parameters = parseParams(params)
                                val result = findImgByResize(
                                    convertBGRtoARGB(bufferedImageToMat(imagePanel.image)),
                                    convertBGRtoARGB(bufferedImageToMat(deviceImagePanel.image!!)),
                                    parameters[2].toDouble(),
                                    parameters[3].toInt(),
                                    parameters[4].toInt()
                                )
                                if (result != null) {

                                    imagePanel.setRedPoint(result.x.toInt(), result.y.toInt())
                                } else {
                                    println("No matching points found.")
                                }
                            }
                        }

                        "findImgBySift" -> {
                            val regex = Regex("""images\.findImgBySift\(([^)]+)\)""")
                            val match = regex.find(str)
                            if (match != null) {
                                val params = match.groupValues[1] // This will be the parameters as a string
                                val parameters = parseParams(params)
                                val result = findImgBySift(
                                    convertBGRtoARGB(bufferedImageToMat(imagePanel.image)),
                                    convertBGRtoARGB(bufferedImageToMat(deviceImagePanel.image!!)),
                                    parameters[2].toDouble()
                                )
                                if (result != null) {
                                    imagePanel.setRedPoint(result.x.toInt(), result.y.toInt())
                                } else {
                                    println("No matching points found.")
                                }


                            }
                        }

                        else -> println("无效的天数")

                    }

                }


            }
        }
        val generateButton2 = JButton("清除").apply {
            preferredSize = Dimension(150, 40)
            font = Font("宋体", Font.PLAIN, 14) // 使用宋体字体，普通样式，大小为14
            addActionListener {

                imagePanel.clearRedPoint()

            }
        }

        gbc.gridx = 1
        gbc.gridy = 0
        gbc.gridheight = 3
        gbc.weighty = 3.0
        gbc.gridwidth = GridBagConstraints.REMAINDER // 占据剩余列
        gbc.fill = GridBagConstraints.BOTH
        mainPanel.add(textAreaScroll, gbc)

        // 添加下拉框
        gbc.weighty = 1.0
        gbc.gridx = 1
        gbc.gridy = 3
        gbc.gridheight = 1
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0 // 确保下拉框能够扩展占据剩余水平空间
        mainPanel.add(comboBox, gbc)

// 添加生成按钮

        gbc.gridy = 4
        gbc.gridwidth = GridBagConstraints.REMAINDER
        gbc.fill = GridBagConstraints.HORIZONTAL
        mainPanel.add(generateButton, gbc)

// 添加测试按钮

        gbc.gridy = 5
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.weightx = 1.0  // 使组件在水平方向上扩展以填满可用空间
        gbc.gridx = 1
        gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.HORIZONTAL
        mainPanel.add(generateButton1, gbc)
        gbc.gridx = 2
        gbc.gridwidth = 1
        gbc.fill = GridBagConstraints.HORIZONTAL

        mainPanel.add(generateButton2, gbc)

        return mainPanel
    }

    private fun saveImage(imagePanel: ImagePanel) {
        val chooser = JFileChooser().apply {
            dialogTitle = "保存图片"
            fileFilter = FileNameExtensionFilter("PNG 图片", "png")
            selectedFile = File("image.png")
        }

        // 显示保存对话框
        val userSelection = chooser.showSaveDialog(null)
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            var fileToSave = chooser.selectedFile

            // 检查文件是否有扩展名，如果没有，则加上 .png
            if (!fileToSave.name.lowercase().endsWith(".png")) {
                fileToSave = File(fileToSave.parentFile, "${fileToSave.name}.png")
            }

            // 如果文件存在，提示用户确认是否覆盖
            if (fileToSave.exists()) {
                val response = JOptionPane.showConfirmDialog(
                    null,
                    "文件已存在，是否覆盖？",
                    "确认",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.WARNING_MESSAGE
                )
                if (response != JOptionPane.YES_OPTION) {
                    return
                }
            }

            // 尝试保存图像
            try {

                ImageIO.write(deviceImagePanel.image!!, "png", fileToSave)
                JOptionPane.showMessageDialog(null, "图片保存成功: ${fileToSave.absolutePath}")
            } catch (e: Exception) {
                JOptionPane.showMessageDialog(null, "图片保存失败: ${e.message}", "错误", JOptionPane.ERROR_MESSAGE)
            }
        }
    }


    class ImagePanel(imageIcon: ImageIcon) : JPanel() {
        // 存储图像路径
        var imagePath: String? = null

        // 将传入的 ImageIcon 转换为 BufferedImage
        var image: BufferedImage = createBufferedImage(imageIcon)

        // 保存原始图像，以便于后续恢复或操作
        private var originalImage: BufferedImage = image

        // 默认缩放比例
        private var scale = 1.0

        // 用于记录鼠标的位置
        private var mousePoint: Point? = null

        // 用于记录选择区域的矩形
        private var selectionRect: Rectangle? = null

        // 判断是否正在进行选择操作
        private var selecting = false

        // 用于记录图像偏移的水平坐标
        private var x = 0

        // 用于记录图像偏移的垂直坐标
        private var y = 0

        // 判断鼠标是否被拖动
        private var dragged = false // 长按阈值，单位：毫秒

        // 缩放倍数，用于图像的放大缩小，通常定义为整数
        private val ZOOM_SIZE = 9 // 缩放大小

        // 放大镜倍数，用于放大镜的放大效果
        private val MAGNIFICATION = 20 // 放大镜的放大倍数

        // 放大镜偏移量，在 x 方向上的偏移，用于调整放大镜的位置
        private val MAGNIFIER_OFFSET_X = 30 // 放大镜 x 方向的偏移量

        // 放大镜偏移量，在 y 方向上的偏移，用于调整放大镜的位置
        private val MAGNIFIER_OFFSET_Y = 60 // 放大镜 y 方向的偏移量

        // 网格线的颜色，用于图像中绘制网格线
        private val GRID_COLOR = Color.BLACK // 网格线的颜色为黑色

        // 高亮区域的颜色，用于标记和突出显示的区域
        private val HIGHLIGHT_COLOR = Color.RED // 高亮区域的颜色为红色

        // 信息框的背景色，用于显示信息的框的颜色
        private val INFO_BOX_COLOR = Color.WHITE // 信息框的背景颜色为白色

        // 信息文本的颜色，用于显示信息的文本颜色
        private val INFO_TEXT_COLOR = Color.BLACK // 信息文本的颜色为黑色


        // 初始化代码块，定义鼠标事件处理器
        init {
            // 创建一个自定义的 MouseAdapter，用于监听鼠标事件
            val mouseAdapter = object : MouseAdapter() {
                // 记录鼠标按下时的起始位置
                private var startX = 0
                private var startY = 0

                // 鼠标按下事件：记录起始位置
                override fun mousePressed(e: MouseEvent) {
                    startX = e.x // 记录鼠标按下时的 x 坐标
                    startY = e.y // 记录鼠标按下时的 y 坐标
                }

                // 鼠标点击事件：检测双击左键并执行特定操作
                override fun mouseClicked(e: MouseEvent) {
                    // 判断是否是左键双击事件
                    if (SwingUtilities.isLeftMouseButton(e) && e.clickCount == 2) {
                        addRGB() // 如果是双击左键，调用 addRGB() 方法
                    }
                }

                // 鼠标拖动事件：处理不同按键的拖动行为
                override fun mouseDragged(e: MouseEvent) {
                    when {
                        SwingUtilities.isRightMouseButton(e) -> handleRightDrag(e) // 右键拖动，执行选择操作
                        SwingUtilities.isLeftMouseButton(e) -> handleLeftDrag(e)  // 左键拖动，处理拖动行为
                    }
                }

                // 鼠标释放事件：处理右键菜单显示与拖动结束
                override fun mouseReleased(e: MouseEvent) {
                    // 如果没有拖动且是右键释放，显示右键菜单
                    if (!dragged && SwingUtilities.isRightMouseButton(e)) {
                        showPopupMenu(e.point)
                    }
                    dragged = false // 拖动结束，重置标志
                }

                // 鼠标移动事件：更新鼠标位置并重绘界面
                override fun mouseMoved(e: MouseEvent) {
                    mousePoint = e.point // 更新鼠标位置
                    repaint() // 重新绘制界面
                }

                // 处理右键拖动：更新选择矩形
                private fun handleRightDrag(e: MouseEvent) {
                    if (!selecting) {
                        selecting = true // 标记为正在选择区域
                        originalImage = image // 保存原始图像
                        selectionRect = Rectangle(startX, startY, 0, 0) // 初始化选择矩形区域
                    }
                    updateSelectionRect(e) // 更新选择矩形
                    mousePoint = e.point // 更新鼠标位置
                    repaint() // 重新绘制界面

                    dragged = true // 标记为拖动状态
                }

                // 处理左键拖动：模拟滚动视口
                private fun handleLeftDrag(e: MouseEvent) {
                    val viewport = parent as? JViewport ?: return // 获取父级视口，如果没有则返回
                    val viewPosition = viewport.viewPosition
                    viewPosition.translate(startX - e.x, startY - e.y) // 计算视口的新位置
                    scrollRectToVisible(Rectangle(viewPosition, viewport.size)) // 滚动视口使矩形可见
                    startX = e.x // 更新起始位置
                    startY = e.y
                    dragged = true // 标记为拖动状态
                }

                // 更新选择矩形的大小和位置
                private fun updateSelectionRect(e: MouseEvent) {
                    val x1 = min(startX, e.x) // 选择矩形的左上角 x 坐标
                    val y1 = min(startY, e.y) // 选择矩形的左上角 y 坐标
                    val width = abs(startX - e.x) // 选择矩形的宽度
                    val height = abs(startY - e.y) // 选择矩形的高度
                    selectionRect?.setBounds(x1, y1, width, height) // 更新选择矩形的大小和位置
                }
            }

            // 添加鼠标监听器：监听鼠标点击和拖动事件
            addMouseListener(mouseAdapter)
            addMouseMotionListener(mouseAdapter)

            // 添加鼠标滚轮事件监听器：处理缩放操作
            addMouseWheelListener { e ->
                scale *= if (e.preciseWheelRotation < 0) 1.1 else 0.9 // 根据滚轮旋转方向调整缩放比例
                revalidate() // 重新验证布局
                repaint() // 重新绘制界面
            }
        }


        private fun finalizeSelection() {
            // 获取缩放后的选择矩形，如果选择矩形为空则返回
            val scaledRect = getScaledRectangle(selectionRect ?: return)

            // 获取原始图像的宽度和高度
            val imageWidth = image.width
            val imageHeight = image.height

            // 初始化选择区域的坐标和大小
            var x = scaledRect.x
            var y = scaledRect.y
            var width = scaledRect.width
            var height = scaledRect.height

            // 确保选择区域在图像范围内，避免越界
            // 1. 处理左上角坐标越界：x 和 y 应大于等于 0
            if (x < 0) x = 0
            if (y < 0) y = 0

            // 2. 处理右下角坐标越界：确保选择区域的宽度和高度不超出原图的尺寸
            if (x + width > imageWidth) width = imageWidth - x
            if (y + height > imageHeight) height = imageHeight - y

            // 3. 确保选择区域的有效性，宽度和高度都必须大于 0
            if (width > 0 && height > 0) {
                // 4. 从原始图像中提取选中的子图像区域
                val selectedImage = image.getSubimage(x, y, width, height)

                // 5. 创建一个新的 BufferedImage，大小与选区相同
                val newImage = BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB).apply {
                    createGraphics().apply {
                        // 在新的图像上绘制选中的部分
                        drawImage(selectedImage, 0, 0, null)
                        dispose() // 完成绘制后释放资源
                    }
                }

                // 6. 加载选中的图像到显示面板
                deviceImagePanel.loadImage(newImage)

                // 7. 重新验证布局和重绘界面，确保界面正确显示新的图像
                revalidate()
                repaint()
            }
        }


        private fun getScaledRectangle(rect: Rectangle): Rectangle {
            val startX = ((rect.x - x) / scale).toInt()
            val startY = ((rect.y - y) / scale).toInt()
            val endX = ((rect.x + rect.width - x) / scale).toInt()
            val endY = ((rect.y + rect.height - y) / scale).toInt()
            return Rectangle(startX, startY, endX - startX, endY - startY)
        }


        fun loadImage(imagePath: String) {
            if (File(imagePath).exists()) {
                this.imagePath = imagePath
                image = createBufferedImage(ImageIcon(imagePath))
                revalidate()
                repaint()
            }

        }


        private fun createBufferedImage(imageIcon: ImageIcon): BufferedImage {
            return BufferedImage(imageIcon.iconWidth, imageIcon.iconHeight, BufferedImage.TYPE_INT_ARGB).apply {
                createGraphics().apply {
                    imageIcon.paintIcon(null, this, 0, 0)
                    dispose()
                }
            }
        }

        private fun showPopupMenu(point: Point) {
            val popupMenu = JPopupMenu().apply {
                // 设置菜单项的字体和大小
                val font = Font("宋体", Font.PLAIN, 14)

                // 创建菜单项并添加到弹出菜单中
                add(createMenuItem("复制坐标", font) { copyCoordinates() })
                add(createMenuItem("复制范围", font) {

                    val ra = copyRange()
                    Toolkit.getDefaultToolkit().systemClipboard.setContents(
                        StringSelection("${ra?.left}, ${ra?.top}, ${ra?.right}, ${ra?.bottom}"),
                        null
                    )
                    revalidate()
                    repaint()
                    selecting = false
                    selectionRect = null

                })
                add(createMenuItem("复制颜色", font) { copyRGB() })
                add(createMenuItem("剪切图片", font) { cutImage() })
                addSeparator() // 添加分隔线
                add(createMenuItem("取消", font) {
                    isVisible = false
                    selecting = false

                    selectionRect = null
                })
            }
            popupMenu.show(this, point.x, point.y)
        }

        private fun createMenuItem(text: String, font: Font, action: () -> Unit): JMenuItem {
            return JMenuItem(text).apply {
                this.font = font
                addActionListener { action() }
            }
        }

        private fun cutImage() {
            h = imagePanel!!.image.height
            w = imagePanel!!.image.width
            finalizeSelection()
            selecting = false
            selectionRect = null

        }

        private fun copyRange(): Range? {
            val scaledRect = getScaledRectangle(selectionRect ?: return null)

            // 获取原始图像的宽度和高度
            val imageWidth = image.width
            val imageHeight = image.height

            // 确保子图像区域在原始图像范围内
            var x = scaledRect.x
            var y = scaledRect.y
            var width = scaledRect.width
            var height = scaledRect.height

            if (x < 0) x = 0
            if (y < 0) y = 0
            if (x + width > imageWidth) width = imageWidth - x
            if (y + height > imageHeight) height = imageHeight - y
            val top = y
            val left = x
            val bottom = y + height
            val right = x + width

            return Range(left = left, top = top, bottom = bottom, right = right)
        }

        data class Range(val left: Int, val top: Int, val bottom: Int, val right: Int)

        private fun copyCoordinates() {
            mousePoint?.let {
                val text = formatCoordinates(it)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            }
        }

        private fun copyRGB() {
            mousePoint?.let {
                val text = getRGBText(it)
                Toolkit.getDefaultToolkit().systemClipboard.setContents(StringSelection(text), null)
            }
        }

        private fun addRGB() {
            mousePoint?.let {
                val text = getRGBText(it)
                val zb = formatCoordinates(it)


                for (i in 1 until 9) {
                    val textField = customPanel.getRowComponents(i)?.get(1) as JTextField
                    val textField1 = customPanel.getRowComponents(i)?.get(3) as JTextField
                    val textField2 = customPanel.getRowComponents(i)?.get(4) as JTextField
                    val colorPanel = customPanel.getRowComponents(i)?.get(2) as ColorPanel
                    val jCheckBox = customPanel.getRowComponents(i)?.get(5) as JCheckBox
                    if (textField.text.isEmpty()) {
                        jCheckBox.setSelected(true)
                        textField.text = text
                        textField2.text = zb
                        colorPanel.updateColor(text)
                        break
                    }
                }

            }
        }


        private fun getRGBText(point: Point): String {
            val x = ((point.x - this.x) / scale).toInt()
            val y = ((point.y - this.y) / scale).toInt()
            return if (isInBounds(x, y)) {
                val rgb = image.getRGB(x, y)
                val color = Color(rgb)
                String.format("#%02X%02X%02X", color.red, color.green, color.blue)
            } else {
                "HEX: Out of bounds"
            }
        }

        private fun formatCoordinates(point: Point): String {
            val x = ((point.x - this.x) / scale).toInt()
            val y = ((point.y - this.y) / scale).toInt()
            return "$x, $y"
        }

        private var redPoint: Point? = null
        fun setRedPoint(x: Int, y: Int) {
            redPoint = Point(x, y)
            repaint()
        }

        fun clearRedPoint() {
            redPoint = null
            repaint()
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)
            val g2d = g as Graphics2D
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            val at = AffineTransform().apply {
                translate(this@ImagePanel.x.toDouble(), this@ImagePanel.y.toDouble())
                scale(scale, scale)
            }
            g2d.drawImage(image, at, this)

            mousePoint?.let {
                drawMagnifier(g2d)
            }

            selectionRect?.let {
                g2d.color = Color.RED
                g2d.draw(it)

            }

            redPoint?.let { point ->
                val panelX = (point.x * scale + this.x).toInt()
                val panelY = (point.y * scale + this.y).toInt()
                g2d.color = Color.RED
                g2d.fillOval(panelX - 5, panelY - 5, 10, 10) // 画一个半径为 5 的圆点
            }

        }

        private fun drawMagnifier(g2d: Graphics2D) {
            // 计算鼠标在图像中的坐标
            val mouseXInImage = ((mousePoint!!.x - x) / scale).toInt()
            val mouseYInImage = ((mousePoint!!.y - y) / scale).toInt()

            // 放大镜的宽度和高度
            val zoomedWidth = ZOOM_SIZE * MAGNIFICATION
            val zoomedHeight = ZOOM_SIZE * MAGNIFICATION

            // 创建放大镜图像
            val zoomedImage = createZoomedImage(mouseXInImage, mouseYInImage)

            // 计算放大镜的默认位置
            var xOffset = mousePoint!!.x + MAGNIFIER_OFFSET_X
            var yOffset = mousePoint!!.y + MAGNIFIER_OFFSET_Y

            // 获取图像的实际显示区域
            val imageDisplayRect = getVisibleImageAreaRect()

            // 确保鼠标位置在图像显示区域内
            if (!printMouseCoordinatesInVisibleRange()) {
                // 鼠标位置不在图像显示区域内，不显示放大镜

                return
            }
            val scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, this) as? JScrollPane
            val scrollOffsetX = scrollPane?.horizontalScrollBar?.value ?: 0
            val scrollOffsetY = scrollPane?.verticalScrollBar?.value ?: 0
            // 将鼠标的屏幕坐标转换为相对于当前可见区域的坐标
            val mouseXInVisibleImage = mousePoint!!.x - scrollOffsetX
            val mouseYInVisibleImage = mousePoint!!.y - scrollOffsetY


            // 确保放大镜不会超出图像显示区域的边界

            // 如果放大镜超出图像显示区域底部，将其移动到鼠标上方
            val (horizontalOffsets, verticalOffsets) = calculateOffsets(
                imageDisplayRect.width,
                imageDisplayRect.height,
                300
            )
            val (left, right) = horizontalOffsets
            val (top, bottom) = verticalOffsets


            if (mouseYInVisibleImage > bottom) {
                yOffset = mousePoint!!.y - zoomedHeight - MAGNIFIER_OFFSET_Y
            }

            if (mouseYInVisibleImage < top) {
                yOffset = mousePoint!!.y + MAGNIFIER_OFFSET_Y
            }

//        // 如果放大镜超出图像显示区域右边界，将其移动到鼠标左侧
            if (mouseXInVisibleImage > right) {
                xOffset = mousePoint!!.x - zoomedWidth - MAGNIFIER_OFFSET_X
            }


            // 如果放大镜超出图像显示区域左边界，将其移动到鼠标右侧
            if (mouseXInVisibleImage < left) {
                xOffset = mousePoint!!.x + MAGNIFIER_OFFSET_X
            }

            // 绘制放大镜
            g2d.drawImage(zoomedImage, xOffset, yOffset, this)
            g2d.color = HIGHLIGHT_COLOR
            g2d.drawRect(xOffset, yOffset, zoomedWidth, zoomedHeight)

            drawGrid(g2d, xOffset, yOffset)
            drawHighlight(g2d, xOffset, yOffset)
            drawInfoBox(g2d, xOffset, yOffset + zoomedHeight + 10)
        }

        fun calculateOffsets(width: Int, height: Int, offset: Int): Pair<Pair<Int, Int>, Pair<Int, Int>> {
            val leftOffset = offset
            val rightOffset = width - offset
            val topOffset = offset
            val bottomOffset = height - offset

            val horizontalOffsets = Pair(leftOffset, rightOffset)
            val verticalOffsets = Pair(topOffset, bottomOffset)

            return Pair(horizontalOffsets, verticalOffsets)
        }


        private fun printMouseCoordinatesInVisibleRange(): Boolean {
            // 获取图像的实际显示区域
            val imageDisplayRect = getVisibleImageAreaRect()

            // 获取滚动条的偏移量
            val scrollPane = SwingUtilities.getAncestorOfClass(JScrollPane::class.java, this) as? JScrollPane
            val scrollOffsetX = scrollPane?.horizontalScrollBar?.value ?: 0
            val scrollOffsetY = scrollPane?.verticalScrollBar?.value ?: 0
            // 将鼠标的屏幕坐标转换为相对于当前可见区域的坐标
            val mouseXInVisibleImage = mousePoint!!.x - scrollOffsetX
            val mouseYInVisibleImage = mousePoint!!.y - scrollOffsetY

            // 检查鼠标的可见图像坐标是否在图像的可见区域内
            val isInsideVisibleRange = imageDisplayRect.contains(mouseXInVisibleImage, mouseYInVisibleImage)
            return isInsideVisibleRange
        }


        private fun getVisibleImageAreaRect(): Rectangle {
            // 面板的宽度和高度
            val panelWidth = parent.width
            val panelHeight = parent.height

            // 缩放后的图像宽度和高度
            val scaledImageWidth = (image.width * scale).toInt()
            val scaledImageHeight = (image.height * scale).toInt()

            // 图像在面板上的位置偏移量
            val imageX = x.toInt()
            val imageY = y.toInt()

            // 计算图像在面板上的显示区域的左上角坐标
            val visibleX = max(0, imageX)
            val visibleY = max(0, imageY)

            // 计算图像在面板上的可见区域的宽度和高度
            val visibleWidth = min(panelWidth - visibleX, scaledImageWidth)
            val visibleHeight = min(panelHeight - visibleY, scaledImageHeight)

            return Rectangle(visibleX, visibleY, visibleWidth, visibleHeight)
        }


        private fun createZoomedImage(mouseX: Int, mouseY: Int): BufferedImage {
            val zoomedWidth = ZOOM_SIZE * MAGNIFICATION
            val zoomedHeight = ZOOM_SIZE * MAGNIFICATION
            return BufferedImage(zoomedWidth, zoomedHeight, BufferedImage.TYPE_INT_ARGB).apply {
                for (i in 0 until ZOOM_SIZE) {
                    for (j in 0 until ZOOM_SIZE) {
                        val srcX = mouseX - ZOOM_SIZE / 2 + i
                        val srcY = mouseY - ZOOM_SIZE / 2 + j
                        if (isInBounds(srcX, srcY)) {
                            val rgb = image.getRGB(srcX, srcY)
                            for (k in 0 until MAGNIFICATION) {
                                for (l in 0 until MAGNIFICATION) {
                                    setRGB(i * MAGNIFICATION + k, j * MAGNIFICATION + l, rgb)
                                }
                            }
                        }
                    }
                }
            }
        }

        private fun isInBounds(x: Int, y: Int): Boolean {
            return x >= 0 && x < image.width && y >= 0 && y < image.height
        }

        private fun drawGrid(g2d: Graphics2D, x: Int, y: Int) {
            g2d.color = GRID_COLOR
            for (i in 0..ZOOM_SIZE) {
                val gridX = x + i * MAGNIFICATION
                g2d.drawLine(gridX, y, gridX, y + ZOOM_SIZE * MAGNIFICATION)
            }
            for (j in 0..ZOOM_SIZE) {
                val gridY = y + j * MAGNIFICATION
                g2d.drawLine(x, gridY, x + ZOOM_SIZE * MAGNIFICATION, gridY)
            }
        }

        private fun drawHighlight(g2d: Graphics2D, x: Int, y: Int) {
            val highlightX = (ZOOM_SIZE / 2) * MAGNIFICATION
            val highlightY = (ZOOM_SIZE / 2) * MAGNIFICATION
            g2d.color = HIGHLIGHT_COLOR
            g2d.drawRect(x + highlightX, y + highlightY, MAGNIFICATION, MAGNIFICATION)
        }

        private fun drawInfoBox(g2d: Graphics2D, x: Int, y: Int) {
            val infoWidth = 180
            val infoHeight = 50
            g2d.color = INFO_BOX_COLOR
            g2d.fillRect(x, y, infoWidth, infoHeight)
            g2d.color = INFO_TEXT_COLOR
            g2d.drawRect(x, y, infoWidth, infoHeight)

            mousePoint?.let {
                val coordinatesText = formatCoordinates(it)
                val rgbText = getRGBText(it)
                val fontMetrics = g2d.fontMetrics
                val lineHeight = fontMetrics.height

                // 绘制坐标信息
                g2d.drawString(coordinatesText, x + 10, y + 20)
                // 绘制 RGB 信息
                g2d.drawString(rgbText, x + 10, y + 20 + lineHeight)
            }
        }

        override fun getPreferredSize(): Dimension {
            return Dimension((image.width * scale).toInt(), (image.height * scale).toInt())
        }
    }
}