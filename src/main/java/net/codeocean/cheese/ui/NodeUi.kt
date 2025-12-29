package net.codeocean.cheese.ui


import net.codeocean.cheese.server.clientSessions
import io.ktor.websocket.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.fife.ui.rsyntaxtextarea.RSyntaxTextArea
import org.fife.ui.rsyntaxtextarea.SyntaxConstants
import org.w3c.dom.Document
import org.w3c.dom.Node
import java.awt.*
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import java.awt.image.BufferedImage
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.InputStream
import java.util.*
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel
import javax.swing.tree.TreePath
import javax.swing.tree.TreeSelectionModel
import javax.xml.parsers.DocumentBuilderFactory

class nui {
    // 初始化组件
    val panel: JPanel = JPanel()
    val nodeButton: JButton = JButton("获取节点")

    //    val editorPane: JEditorPane = JEditorPane()
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

    init {
        // 设置布局
        panel.layout = BorderLayout()
        panel.add(nodeButton, BorderLayout.NORTH)

        panel.add(textAreaScroll, BorderLayout.CENTER)
    }

}

object NodeUi {

    var imagePanel: ImagePanel? = null
    var document: Document? = null
    fun getFrame(): JFrame {
        val frame = JFrame("Cheese 节点工具").apply {
            try {
                defaultCloseOperation = JFrame.EXIT_ON_CLOSE
                setSize(700, 500)  // 设置窗口大小
                // 创建左侧面板：显示图片
                imagePanel = ImagePanel()
                val imageScrollPane = JScrollPane(imagePanel)

                val root = DefaultMutableTreeNode("null")

                // 创建一个空的树模型
                val treeModel = DefaultTreeModel(root)
                tree = JTree(treeModel)

                tree!!.selectionModel.selectionMode = TreeSelectionModel.DISCONTIGUOUS_TREE_SELECTION
                addRightClickListener(tree!!)
                val treeScrollPane = JScrollPane(tree)

                val buttonPanel = bu.panel
                bu.nodeButton.addActionListener { e ->
                    GlobalScope.launch(Dispatchers.Default) {  // 在主线程中更新 UI
                        clientSessions["device"]?.send("5")
                    }
                }
                buttonPanel.minimumSize = Dimension(0, 0) // 确保最小尺寸允许缩放
                val popupMenu = JPopupMenu()
                val copyItem = JMenuItem("复制代码")
                copyItem.addActionListener {
                    val selectedText = bu.textArea.text as String
                    val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                    clipboard.setContents(StringSelection(selectedText), null)
                }
                popupMenu.add(copyItem)
                bu.textArea.addMouseListener(object : MouseAdapter() {
                    override fun mousePressed(e: MouseEvent) {
                        if (e.button == MouseEvent.BUTTON3) { // 右键点击
                            popupMenu.show(e.component, e.x, e.y)
                        }
                    }

                    override fun mouseReleased(e: MouseEvent) {
                        if (e.button == MouseEvent.BUTTON3) { // 右键释放
                            popupMenu.show(e.component, e.x, e.y)
                        }
                    }
                })


                // 创建左右分隔面板
                val splitPane1 = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
                splitPane1.leftComponent = imageScrollPane
                splitPane1.rightComponent = treeScrollPane
                splitPane1.dividerLocation = width / 3 // 初始分隔位置
                splitPane1.dividerSize = 3

                // 创建整体分隔面板
                val splitPane2 = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
                splitPane2.leftComponent = splitPane1
                splitPane2.rightComponent = buttonPanel
                splitPane2.dividerLocation = width / 2 // 调整右侧按钮面板的宽度
                splitPane2.dividerSize = 3
                splitPane2.addPropertyChangeListener(JSplitPane.DIVIDER_LOCATION_PROPERTY) { e ->
                    buttonPanel.revalidate()
                    buttonPanel.repaint()
                }
                // 将整体分隔面板添加到窗口
                contentPane.add(splitPane2, BorderLayout.CENTER)


            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return frame
    }

    var tree: JTree? = null
    var rootNode: DefaultMutableTreeNode? = null
    fun createContextMenu(node: DefaultMutableTreeNode): JPopupMenu {
        val popupMenu = JPopupMenu()
        val copyMenuItem = JMenuItem("复制属性")
        copyMenuItem.addActionListener {
            val selectedText = node.userObject as String
            val parts = selectedText.split("=").map { it.trim() }
            val clipboard = Toolkit.getDefaultToolkit().systemClipboard
            var selection: StringSelection? = null
            if (parts.size == 2) {
                selection = StringSelection(parts[1])
            } else {
                selection = StringSelection(selectedText)
            }


            clipboard.setContents(selection, null)
        }
        val makeCodeClick = JMenuItem("生成点击代码")
        makeCodeClick.addActionListener {
            val selectedPaths = tree!!.selectionPaths
            val selectedNodes = selectedPaths?.mapNotNull { path ->
                path.lastPathComponent as? DefaultMutableTreeNode
            }?.map { node ->
                node.userObject as? String
            } ?: emptyList()
            val selectedText = selectedNodes.joinToString("\n")

            val map = selectedText.split("\n")
                .map { it.trim() } // 去除每行的前后空白
                .filter { it.isNotEmpty() } // 过滤掉空行
                .map {
                    val (key, value) = it.split("=").map { part -> part.trim() }
                    key to value
                }
                .toMap()
            var text: String? = null
            val sb = StringBuilder()
            for (key in map.keys) {
                when (key) {
                    "text" -> {

                        sb.append("e.getText()===${map[key]} && ")
                    }

                    "pkg" -> {
                        sb.append("e.getPackageName()===${map[key]} && ")
                    }

                    "id" -> {
                        sb.append("e.getViewIdResourceName()===${map[key]} && ")
                    }

                    "bounds" -> {
                        val regex = "\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]".toRegex()
                        val matchResult = regex.find(map[key].toString())

                        if (matchResult != null) {
                            val num1 = matchResult.groupValues[1].toInt()
                            val num2 = matchResult.groupValues[2].toInt()
                            val num3 = matchResult.groupValues[3].toInt()
                            val num4 = matchResult.groupValues[4].toInt()
                            sb.append("e.getBoundsInScreen().left===$num1 && e.getBoundsInScreen().top===$num2 && e.getBoundsInScreen().right===$num3 && e.getBoundsInScreen().bottom===$num4 && ")
                        }

                    }

                    "desc" -> {
                        sb.append("e.getContentDescription()===${map[key]} && ")
                    }


                    else -> println("Invalid day")
                }
            }





            text = """
        const core = require('cheese-js');
        const uinode = new core.uinode()
        let u =uinode.forEachNode((e) => {
        return ${sb.toString().trim().removeSuffix("&&").trim()}
        })
        if (u.size() > 0) {
            let firstElement = u.get(0);
            if (firstElement && firstElement.tryClick()) {
                console.log("点击成功");
            }
        }
        """.trimIndent()

            bu.textArea.text = text

        }
        val makeCodeClick1 = JMenuItem("生成与点击代码")
        makeCodeClick1.addActionListener {
            val selectedPaths = tree!!.selectionPaths
            val selectedNodes = selectedPaths?.mapNotNull { path ->
                path.lastPathComponent as? DefaultMutableTreeNode
            }?.map { node ->
                node.userObject as? String
            } ?: emptyList()
            val selectedText = selectedNodes.joinToString("\n")

            val map = selectedText.split("\n")
                .map { it.trim() } // 去除每行的前后空白
                .filter { it.isNotEmpty() } // 过滤掉空行
                .map {
                    val (key, value) = it.split("=").map { part -> part.trim() }
                    key to value
                }
                .toMap()
            var text: String? = null
            val sb = StringBuilder()
            sb.append("e.")
            for (key in map.keys) {
                when (key) {
                    "text" -> {

                        sb.append("text(\"${map[key]}\").")
                    }

                    "package" -> {
                        sb.append("package(\"${map[key]}\").")
                    }

                    "resource-id" -> {
                        sb.append("id(\"${map[key]}\").")
                    }

                    "bounds" -> {
                        val regex = "\\[(\\d+),(\\d+)\\]\\[(\\d+),(\\d+)\\]".toRegex()
                        val matchResult = regex.find(map[key].toString())

                        if (matchResult != null) {
                            val num1 = matchResult.groupValues[1].toInt()
                            val num2 = matchResult.groupValues[2].toInt()
                            val num3 = matchResult.groupValues[3].toInt()
                            val num4 = matchResult.groupValues[4].toInt()
                            sb.append("bounds(base.Rect($num1,$num2,$num3,$num4)).")
                        } else {
                            sb.append("bounds(\"${map[key]}\").")
                        }

                    }

                    "desc" -> {
                        sb.append("desc(\"${map[key]}\").")
                    }


                    else -> println("Invalid day")
                }
            }




            sb.append("and")
            text = """
          const core = require('cheese-js');
          const uinode = new core.uinode()
          let d =uinode.forEachNode((e) => {
                return $sb
          }).find()
          if (d.get(0).click()) {
              console.log("点击成功")
          }   
        """.trimIndent()

            bu.textArea.text = text

        }
        popupMenu.add(copyMenuItem)
//        popupMenu.add(makeCodeClick1)
        popupMenu.add(makeCodeClick)
        return popupMenu
    }

    fun addRightClickListener(tree: JTree) {
        tree.addMouseListener(object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                maybeShowPopup(e)
            }

            override fun mouseReleased(e: MouseEvent) {
                maybeShowPopup(e)
            }

            private fun maybeShowPopup(e: MouseEvent) {
                if (e.isPopupTrigger) {
                    val path = tree.getPathForLocation(e.x, e.y)
                    val selectedNode = path?.lastPathComponent as? DefaultMutableTreeNode
                    selectedNode?.let {
                        val popupMenu = createContextMenu(it)
                        popupMenu.show(e.component, e.x, e.y)
                    }
                }
            }
        })
    }

    val bu = nui()


    fun findNode(node: DefaultMutableTreeNode, targetName: String): DefaultMutableTreeNode? {

        if (node.userObject == targetName) {
            return node.parent as? DefaultMutableTreeNode
        }

        for (i in 0 until node.childCount) {
            val childNode = node.getChildAt(i) as DefaultMutableTreeNode
            val foundNode = findNode(childNode, targetName)
            if (foundNode != null) {
                return foundNode
            }
        }

        return null
    }

    fun expandNode(tree: JTree, targetNode: DefaultMutableTreeNode) {
        val model = tree.model as DefaultTreeModel
        val root = model.root as DefaultMutableTreeNode
        val path = findPath(root, targetNode) ?: return


        tree.expandPath(path)

        // 确保目标节点可见
        tree.scrollPathToVisible(path)

        // 计算节点的实际位置并调整滚动位置以使节点居中
        SwingUtilities.invokeLater {
            val treePath = tree.getPathBounds(path)
            if (treePath != null) {
                val visibleRect = tree.visibleRect
                val nodeRect = tree.getPathBounds(path)
                if (nodeRect != null) {
                    // 计算滚动位置
                    val centerY = (visibleRect.height - nodeRect.height) / 2 + nodeRect.y
                    tree.scrollRectToVisible(Rectangle(nodeRect.x, centerY + 300, nodeRect.width, nodeRect.height))
                }
            }
        }
    }


    private fun findPath(root: DefaultMutableTreeNode, targetNode: DefaultMutableTreeNode): TreePath? {
        val stack = Stack<TreePath>()
        stack.push(TreePath(root))

        while (stack.isNotEmpty()) {
            val path = stack.pop()
            val node = path.lastPathComponent as DefaultMutableTreeNode

            if (node == targetNode) return path

            for (i in 0 until node.childCount) {
                stack.push(path.pathByAddingChild(node.getChildAt(i)))
            }
        }
        return null
    }


    @Throws(Exception::class)
    fun parseXML(inputStream: InputStream): Document {
        val factory = DocumentBuilderFactory.newInstance()
        val builder = factory.newDocumentBuilder()
        return builder.parse(inputStream)
    }

    var i = 0
    fun buildTree(node: Node): DefaultMutableTreeNode {
        val treeNode = DefaultMutableTreeNode(i)
        i++
        if (node.hasAttributes()) {
            val attributes = node.attributes
            for (i in 0 until attributes.length) {
                val attr = attributes.item(i)
                val attrNode = DefaultMutableTreeNode(
                    "${attr.nodeName}=${attr.nodeValue}"
                )
                treeNode.add(attrNode)
            }
        }

        val children = node.childNodes
        for (i in 0 until children.length) {
            val child = children.item(i)
            if (child.nodeType == Node.ELEMENT_NODE) {
                val childNode = buildTree(child)
                treeNode.add(childNode)
            }
        }

        return treeNode
    }

    class ImagePanel : JPanel() {
        private var image: BufferedImage? = null
        private var rectangle: Rectangle? = null  // 用于存储要绘制的矩形

        init {
            // 添加鼠标监听器
            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    val panelWidth = width
                    val panelHeight = height
                    image?.let { img ->
                        val imageWidth = img.width
                        val imageHeight = img.height

                        // 计算缩放比例
                        val xScale = panelWidth.toDouble() / imageWidth
                        val yScale = panelHeight.toDouble() / imageHeight
                        val scale = minOf(xScale, yScale)

                        // 计算缩放后的图片尺寸
                        val scaledWidth = (imageWidth * scale).toInt()
                        val scaledHeight = (imageHeight * scale).toInt()

                        // 计算居中位置
                        val x = (panelWidth - scaledWidth) / 2
                        val y = (panelHeight - scaledHeight) / 2

                        // 计算点击位置在图片上的坐标
                        val imgX = ((e.x - x) / scale).toInt()
                        val imgY = ((e.y - y) / scale).toInt()

//                    println("Clicked on image coordinates: ($imgX, $imgY)")
                        val b = findNode(document!!, imgX, imgY)
                        val targetNode =
                            findNode(rootNode!!, "bounds=[${b!!.x},${b.y}][${b.x + b.width},${b.y + b.height}]")
                        if (targetNode != null) {

                            expandNode(tree!!, targetNode)
                        }
                        // 示例：绘制一个矩形
                        rectangle = b
                        repaint()
                    }
                }
            })
        }

        override fun paintComponent(g: Graphics) {
            super.paintComponent(g)

            image?.let { img ->
                val panelWidth = width
                val panelHeight = height
                val imageWidth = img.width
                val imageHeight = img.height

                // 计算缩放比例
                val xScale = panelWidth.toDouble() / imageWidth
                val yScale = panelHeight.toDouble() / imageHeight
                val scale = minOf(xScale, yScale)

                // 计算缩放后的图片尺寸
                val scaledWidth = (imageWidth * scale).toInt()
                val scaledHeight = (imageHeight * scale).toInt()

                // 计算居中位置
                val x = (panelWidth - scaledWidth) / 2
                val y = (panelHeight - scaledHeight) / 2

                // 绘制缩放后的图片
                g.drawImage(img, x, y, scaledWidth, scaledHeight, this)

                // 绘制矩形
                rectangle?.let {
                    val scaledRectangle = Rectangle(
                        (x + it.x * scale).toInt(),
                        (y + it.y * scale).toInt(),
                        (it.width * scale).toInt(),
                        (it.height * scale).toInt()
                    )
                    g.color = Color.RED  // 设置矩形的颜色
                    g.drawRect(scaledRectangle.x, scaledRectangle.y, scaledRectangle.width, scaledRectangle.height)
                }
            }
        }

        fun setImage(image: BufferedImage) {
            this.image = image
            revalidate()
            repaint()
        }

        fun loadImage(imagePath: String) {
            try {
                if (File(imagePath).exists()) {
                    image = createBufferedImage(ImageIcon(imagePath))
                    revalidate()
                    repaint()
                }


            } catch (e: Exception) {
                e.printStackTrace()
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

    }

    fun loadXML(filePath: String) {
        try {
            val xmlInputStream = loadFile(filePath)
            document = parseXML(xmlInputStream)
            val newRootNode = buildTree(document!!.documentElement)
            tree?.model = DefaultTreeModel(newRootNode)
            rootNode = newRootNode
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun loadFile(filePath: String): InputStream {
        val file = File(filePath)
        if (!file.exists() || !file.isFile) {
            throw FileNotFoundException("File not found: $filePath")
        }
        return FileInputStream(file)
    }

    fun findNode(document: Document, x: Int, y: Int): Rectangle? {


        val coveredBounds = findCoveredNodeBounds(document.documentElement, x, y)
        if (coveredBounds.isNotEmpty()) {
            val smallestRectangle = findSmallestRectangle(coveredBounds)
            if (smallestRectangle != null) {
//                println("最小的矩形是: [${smallestRectangle.x},${smallestRectangle.y}][${smallestRectangle.x + smallestRectangle.width},${smallestRectangle.y + smallestRectangle.height}]")
                return smallestRectangle
            } else {
                return null
            }

        } else {
            return null
        }
    }

    fun findSmallestRectangle(coveredBounds: List<Rectangle>): Rectangle? {
        return coveredBounds.minByOrNull { it.width * it.height }
    }

    fun findCoveredNodeBounds(node: Node, targetX: Int, targetY: Int): List<Rectangle> {
        val coveredBounds = mutableListOf<Rectangle>()

        fun search(node: Node) {
            // 递归遍历所有子节点
            for (i in 0 until node.childNodes.length) {
                val child = node.childNodes.item(i)
                if (child.nodeType == Node.ELEMENT_NODE) {
                    search(child)
                }
            }

            // 检查当前节点
            val boundsAttr = node.attributes?.getNamedItem("bounds")?.nodeValue
            val bounds = boundsAttr?.let { parseBounds(it) }

            bounds?.let {
                // 检查当前节点是否覆盖目标点
                val x = it.x
                val y = it.y
                val width = it.width
                val height = it.height
                if (x <= targetX && targetX <= x + width && y <= targetY && targetY <= y + height) {
                    coveredBounds.add(it)
                }
            }
        }

        search(node)
        return coveredBounds
    }

    fun parseBounds(boundsAttr: String): Rectangle? {
        return try {
            val parts = boundsAttr
                .removePrefix("[")
                .removeSuffix("]")
                .split("][")
                .flatMap { it.split(",") }
                .map(String::toInt)
            val x1 = parts[0]
            val y1 = parts[1]
            val x2 = parts[2]
            val y2 = parts[3]
            Rectangle(x1, y1, x2 - x1, y2 - y1)
        } catch (e: Exception) {
            null
        }
    }

}

