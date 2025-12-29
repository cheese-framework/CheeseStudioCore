package coco.cheese.ide.test

import org.opencv.calib3d.Calib3d
import org.opencv.core.*
import org.opencv.features2d.DescriptorMatcher
import org.opencv.features2d.SIFT
import org.opencv.imgproc.Imgproc

class CPoint(x: Double, y: Double, maxVal: Double) : Point(x, y) {
    private var maxVal: Double

    init {
        this.maxVal = maxVal
    }

    fun getMaxVal(): Double {
        return maxVal
    }

    fun setMaxVal(maxVal: Double) {
        this.maxVal = maxVal
    }

    override fun toString(): String {
        return (("CPoint{" +
                "x=" + x).toString() +
                ", y=" + y).toString() +
                ", maxVal='" + maxVal + '\'' +
                '}'
    }
}

object ImagesTest {

    fun findImgByResize(bigImage: Mat, smallImage: Mat, similarityThreshold: Double, width: Int, height: Int): CPoint? {
        val originalTopLeft: Point

        val originalBottomRight: Point
        val originalCenter: Point
        // 确认图像读取成功
        if (bigImage.empty()) {
            return null
        }
        if (smallImage.empty()) {
            return null
        }

        // 获取大图的原始尺寸
        val originalHeight = bigImage.rows()
        val originalWidth = bigImage.cols()

        // 指定缩放后的分辨率
        val newWidth = width
        val newHeight = height
        val dim = Size(newWidth.toDouble(), newHeight.toDouble())

        // 缩放大图
        val resizedBigImage = Mat()
        Imgproc.resize(bigImage, resizedBigImage, dim, 0.0, 0.0, Imgproc.INTER_AREA)

        // 使用模板匹配
        val result = Mat()
        Imgproc.matchTemplate(resizedBigImage, smallImage, result, Imgproc.TM_CCOEFF_NORMED)

        // 获取匹配结果中的最大值及其位置
        val mmr = Core.minMaxLoc(result)
        val topLeft = mmr.maxLoc
        if (mmr.maxVal < similarityThreshold) return null

        // 获取小图的尺寸
        val h = smallImage.rows()
        val w = smallImage.cols()

        // 计算匹配位置的右下角坐标
        val bottomRight = Point(topLeft.x + w, topLeft.y + h)

        // 将坐标转换回原始大图中的坐标
        val scaleX = originalWidth.toDouble() / newWidth
        val scaleY = originalHeight.toDouble() / newHeight
        originalTopLeft = Point(topLeft.x * scaleX, topLeft.y * scaleY)
        originalBottomRight = Point(bottomRight.x * scaleX, bottomRight.y * scaleY)
        originalCenter =
            Point((originalTopLeft.x + originalBottomRight.x) / 2, (originalTopLeft.y + originalBottomRight.y) / 2)
        return CPoint(originalCenter.x, originalCenter.y, mmr.maxVal)
    }

    fun findImgBySift(imgScene: Mat, imgObject: Mat, distance: Double): Point? {
        // 读取输入图像和目标图像


        // 检查图像是否为空
        if (imgScene.empty() || imgObject.empty()) {

            return null
        }


        // 创建 SIFT 检测器
        val sift = SIFT.create()

        // 检测和计算描述符
        val keypointsObject = MatOfKeyPoint()
        val keypointsScene = MatOfKeyPoint()
        val descriptorsObject = Mat()
        val descriptorsScene = Mat()

        sift.detectAndCompute(imgObject, Mat(), keypointsObject, descriptorsObject)
        sift.detectAndCompute(imgScene, Mat(), keypointsScene, descriptorsScene)


        // 创建 BFMatcher（暴力匹配器）
        val matcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE)
        val matches = MatOfDMatch()
        matcher.match(descriptorsObject, descriptorsScene, matches)


        // 筛选匹配点
        val matchesList = matches.toList()
        val goodMatchesList = matchesList.filter { it.distance < distance }


        // 如果找到足够的匹配点，计算图像的位置
        if (goodMatchesList.size >= 5) {
            val keypointsObjectList = keypointsObject.toList()
            val keypointsSceneList = keypointsScene.toList()

            val objList = mutableListOf<Point>()
            val sceneList = mutableListOf<Point>()
            goodMatchesList.forEach { match ->
                objList.add(keypointsObjectList[match.queryIdx].pt)
                sceneList.add(keypointsSceneList[match.trainIdx].pt)
            }

            val obj = MatOfPoint2f()
            obj.fromList(objList)

            val scene = MatOfPoint2f()
            scene.fromList(sceneList)

            val H = Calib3d.findHomography(obj, scene, Calib3d.RANSAC, 3.0)

            val objCorners = Mat(4, 1, CvType.CV_32FC2)
            val sceneCorners = Mat(4, 1, CvType.CV_32FC2)

            // 定义目标图像的四个角点
            objCorners.put(0, 0, 0.0, 0.0)
            objCorners.put(1, 0, imgObject.cols().toDouble(), 0.0)
            objCorners.put(2, 0, imgObject.cols().toDouble(), imgObject.rows().toDouble())
            objCorners.put(3, 0, 0.0, imgObject.rows().toDouble())

            // 计算目标图像的四个角点在输入图像中的位置
            Core.perspectiveTransform(objCorners, sceneCorners, H)

            val sceneCornersList = mutableListOf<Point>()
            for (i in 0 until sceneCorners.rows()) {
                val point = sceneCorners.get(i, 0)
                sceneCornersList.add(Point(point[0], point[1]))
            }

            // 计算矩形的中心点
            val tl = sceneCornersList[0]
            val br = sceneCornersList[2]
            val centerX = (tl.x + br.x) / 2
            val centerY = (tl.y + br.y) / 2
//            val minDistance = goodMatchesList.minOf { it.distance }
//            println(minDistance)
            // 释放资源
            imgScene.release()
            imgObject.release()
            keypointsObject.release()
            keypointsScene.release()
            descriptorsObject.release()
            descriptorsScene.release()
            matches.release()

            // 返回矩形的中心点坐标
            return Point(centerX, centerY)
        } else {
            // 释放资源
            imgScene.release()
            imgObject.release()
            keypointsObject.release()
            keypointsScene.release()
            descriptorsObject.release()
            descriptorsScene.release()
            matches.release()

            return null
        }

    }


}