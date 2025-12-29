package coco.cheese.ide.test

import org.opencv.core.*
import org.opencv.highgui.HighGui
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import java.awt.Color
import kotlin.math.abs

object ColorTest {

    fun hexToColor(hex: String): Color {
        // 移除可能的前缀（#）
        val cleanedHex = hex.replace("#", "")

        // 解析 HEX 值
        val red = Integer.parseInt(cleanedHex.substring(0, 2), 16)
        val green = Integer.parseInt(cleanedHex.substring(2, 4), 16)
        val blue = Integer.parseInt(cleanedHex.substring(4, 6), 16)

        return Color(red, green, blue)
    }


    fun parseColor(colorString: String): Int {
        return when (colorString.length) {
            7 -> { // #RRGGBB format
                val rgb = colorString.substring(1)
                val red = rgb.substring(0, 2).toInt(16)
                val green = rgb.substring(2, 4).toInt(16)
                val blue = rgb.substring(4, 6).toInt(16)
                (0xFF shl 24) or (red shl 16) or (green shl 8) or blue
            }

            9 -> { // #AARRGGBB format
                val argb = colorString.substring(1)
                val alpha = argb.substring(0, 2).toInt(16)
                val red = argb.substring(2, 4).toInt(16)
                val green = argb.substring(4, 6).toInt(16)
                val blue = argb.substring(6, 8).toInt(16)
                (alpha shl 24) or (red shl 16) or (green shl 8) or blue
            }

            else -> throw IllegalArgumentException("Invalid color format")
        }
    }

    fun findMultiColors(
        img: Mat,
        firstColor: String,
        paths: Array<IntArray>,
        options: Map<String, Any>? = null
    ): Point? {

        val effectiveOptions = options ?: emptyMap()
        val list = paths.flatMap { p -> listOf(p[0], p[1], p[2]) }.toIntArray()
        val distance = effectiveOptions["distance"] as? Int ?: 30
        val firstPoints = findColorInRange(img, hexToColor(firstColor), distance, null)?.toArray() ?: emptyArray()


        return firstPoints.firstOrNull { point ->
            point != null && checksPath(img, point, distance, list)
        }.also {
            if (it != null) println("成功1")
        }
    }


    fun lowerBound(color: Color, threshold: Int): Scalar {
        val red = color.red.toDouble()
        val green = color.green.toDouble()
        val blue = color.blue.toDouble()

        return Scalar(
            (red - threshold).coerceAtLeast(0.0),
            (green - threshold).coerceAtLeast(0.0),
            (blue - threshold).coerceAtLeast(0.0),
            255.0
        )
    }

    fun upperBound(color: Color, threshold: Int): Scalar {
        val red = color.red.toDouble()
        val green = color.green.toDouble()
        val blue = color.blue.toDouble()

        return Scalar(
            (red + threshold).coerceAtMost(255.0),
            (green + threshold).coerceAtMost(255.0),
            (blue + threshold).coerceAtMost(255.0),
            255.0
        )
    }

    fun findColorInRange(
        image: Mat,
        color: Color,
        threshold: Int,
        rect: Rect?
    ): MatOfPoint? {
        val lowerBound = lowerBound(color, threshold)
        val upperBound = upperBound(color, threshold)
        val bi = Mat()
        val matToProcess = if (rect != null) Mat(image, rect) else image
        Core.inRange(matToProcess, lowerBound, upperBound, bi)

        if (rect != null) matToProcess.release()


        val nonZeroPos = Mat()
        Core.findNonZero(bi, nonZeroPos)
        val result = if (nonZeroPos.empty()) null else {
            // Extract points from Mat
            val points = mutableListOf<Point>()
            for (i in 0 until nonZeroPos.rows()) {

                val point = nonZeroPos.get(i, 0)
                if (point != null) {
                    points.add(Point(point[0], point[1]))
                }
            }
            MatOfPoint(*points.toTypedArray())
        }

        bi.release()
        nonZeroPos.release()

        return result
    }

    fun checksPath(
        image: Mat,
        startingPoint: Point,
        distance: Int,
        points: IntArray
    ): Boolean {

        for (i in points.indices step 3) {
            val x = points[i] + startingPoint.x.toInt()
            val y = points[i + 1] + startingPoint.y.toInt()
            val color = points[i + 2]
            val colorDetector = Detector(color, distance)
            if (x !in 0 until image.width() || y !in 0 until image.height()) {
                return false
            }

            val c = getPixel(image, x, y)
            val red = (c shr 16) and 0xFF
            val green = (c shr 8) and 0xFF
            val blue = c and 0xFF

            if (!colorDetector.Color(red, green, blue)) {
                return false
            }
        }
        return true
    }


    data class Detector(val color: Int, val distance: Int) {
        private val mDistance = distance * 3
        val R: Int = (color shr 16) and 0xFF
        val G: Int = (color shr 8) and 0xFF
        val B: Int = color and 0xFF

        fun Color(R: Int, G: Int, B: Int): Boolean {
            return abs(R - this.R) + abs(G - this.G) + abs(B - this.B) <= mDistance
        }
    }

    fun getPixel(mat: Mat, x: Int, y: Int): Int {
        require(!mat.empty()) { "Image is empty" }
        require(x in 0 until mat.cols() && y in 0 until mat.rows()) {
            "Coordinates ($x, $y) are out of bounds."
        }
        val channels = mat.get(y, x) ?: throw NullPointerException("Channels data is null for coordinates ($x, $y)")
        return when (channels.size) {
            3 -> argb(255, channels[0].toInt(), channels[1].toInt(), channels[2].toInt()) // BGR image
            4 -> argb(255, channels[0].toInt(), channels[1].toInt(), channels[2].toInt()) // ARGB image
            else -> throw IllegalStateException("Unexpected number of channels: ${channels.size}")
        }
    }

    fun argb(alpha: Int, red: Int, green: Int, blue: Int): Int {
        return (alpha shl 24) or (red shl 16) or (green shl 8) or blue
    }


}