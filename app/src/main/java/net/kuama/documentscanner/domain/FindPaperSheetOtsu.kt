package net.kuama.documentscanner.domain

import android.graphics.Bitmap
import net.kuama.documentscanner.support.Either
import net.kuama.documentscanner.support.Left
import net.kuama.documentscanner.support.Right
import net.kuama.documentscanner.support.shape
import net.kuama.scanner.data.Corners
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgcodecs.Imgcodecs
import org.opencv.imgproc.Imgproc
import org.opencv.imgproc.Imgproc.threshold
import java.util.*
import kotlin.Comparator
import kotlin.collections.ArrayList
import kotlin.math.*


internal const val NOTHRESHOLD = 48.0

class FindPaperSheetOtsu : UseCase<Pair<Bitmap, Corners?>, FindPaperSheetOtsu.Params>() {

    class Params(
        val matrix: Mat,
        val bitmap: Bitmap,
        val sensitivity: Double = NOTHRESHOLD,
        val returnOriginalMat: Boolean = false
    )

    override suspend fun run(params: FindPaperSheetOtsu.Params): Either<Failure, Pair<Bitmap, Corners?>>  =
    try {
        var src = Mat()
        var dst = Mat()
        val size = Size((params.matrix.width() + 2).toDouble(), (params.matrix.height() +2).toDouble())



        val kernel = Mat.ones(3, 3, CvType.CV_8UC1)
        // Pre elaborazione
        Imgproc.cvtColor(params.matrix, src, Imgproc.COLOR_RGBA2GRAY)
        var cdst = Mat(src.height() +2, src.width() + 2, CvType.CV_8U)
        //Imgproc.medianBlur(src, cdst, 5);
        Imgproc.floodFill(src, cdst, Point(0.0, 0.0), Scalar(123.0))
        Imgproc.Canny(cdst, dst, 100.0, 200.0, 3, false)
        //threshold(dst, dst, 30.0, 200.0, Imgproc.THRESH_OTSU);
        //Imgproc.morphologyEx(dst, dst, Imgproc.MORPH_GRADIENT, kernel)

        Imgcodecs.imwrite("/sdcard/Documents/result.jpg", dst)

//        val matop : MatOfPoint = MatOfPoint()
//
//        Imgproc.goodFeaturesToTrack(dst, matop, 4, 0.5, 10.0, Mat(), 3, 3, false, 0.01)

        var contours: MutableList<MatOfPoint> = ArrayList()
        val hierarchy = Mat()
        Imgproc.findContours(
            dst,
            contours,
            hierarchy,
            Imgproc.RETR_EXTERNAL,
            Imgproc.CHAIN_APPROX_TC89_KCOS
        )

        //

        hierarchy.release()
        contours = contours
            .filter { it.shape.size == 4 }
            .toTypedArray()
            .toMutableList()

        contours.sortWith(Comparator { lhs, rhs ->
            Imgproc.contourArea(rhs).compareTo(Imgproc.contourArea(lhs))
        })

        //val prova = sortMat(matop)
        // Trasformata probabilistica di Hough
        //val linesP: Mat = Mat()
        //Imgproc.HoughLinesP(cdst, linesP, 1.0, Math.PI / 180, 800, 180.0, 5.0)

        //val tmp = sortLines(linesP)
//        var longestLineIndex = -1
//        var secondSegmentIndex = -1
//
//        if(tmp.isNotEmpty()) {
//            longestLineIndex = tmp[tmp.size - 2].first
//            secondSegmentIndex = getNearestSegment(linesP.get(0, 0), linesP, 0)
//        }


//        var a: Double = src.height().toDouble()/4
//        var b: Double = src.width() - src.width().toDouble()/4
//        var c: Double = src.height() - src.height().toDouble()/4
//        var d: Double = src.width().toDouble()/4
//
//        var pnts: Array<Point>
//        var va = Point(d, a)
//        var vb = Point(b, a)
//        var vc = Point(b, c)
//        var vd = Point(d, c)
//
//        if(matop.size().height == 4.0) {
//            pnts = sortMat(matop).toTypedArray()
//        } else {
//            pnts = arrayOf(va, vb, vc, vd)
//        }


//        if (secondSegmentIndex != -1) {
//            val points = getSortedCorners(linesP.get(longestLineIndex, 0), linesP.get(secondSegmentIndex, 0))
//
//
//            if(points.size == 4 ) {
//                val roi = norm(points[0], points[1]) * norm(points[1], points[2])
//                if(roi > a*b*1.2){
//
//                }
//                pnts = points.toTypedArray()
//
//            }
//        }

        val bitmap = Bitmap.createBitmap(params.matrix.cols(), params.matrix.rows(), Bitmap.Config.ARGB_8888)

        if (params.returnOriginalMat) {
            Utils.matToBitmap(params.matrix, bitmap)
        } else {
            //params.bitmap.recycle()
        }

        //var contours: MutableList<Int> = ArrayList()

        Right(contours.firstOrNull()?.let {
            val foundPoints: Array<Point> = sortPoints(it.shape)
            Pair(
                bitmap,
                Corners(
                    foundPoints.toList(),
                    params.matrix.size()
                )
            )
        } ?: Pair(bitmap, null))

    } catch (throwable: Throwable) {
        Left(Failure(throwable))
    }

    private fun sortPoints(src: Array<Point>): Array<Point> {
        val srcPoints = src.toList()
        val result = arrayOf<Point?>(null, null, null, null)
        val sumComparator: Comparator<Point> =
            Comparator { lhs, rhs ->
                java.lang.Double.valueOf(lhs.y + lhs.x).compareTo(rhs.y + rhs.x)
            }
        val diffComparator: Comparator<Point> =
            Comparator { lhs, rhs ->
                java.lang.Double.valueOf(lhs.y - lhs.x).compareTo(rhs.y - rhs.x)
            }

        // top-left corner = minimal sum
        result[0] = Collections.min(srcPoints, sumComparator)

        // bottom-right corner = maximal sum
        result[2] = Collections.max(srcPoints, sumComparator)

        // top-right corner = minimal difference
        result[1] = Collections.min(srcPoints, diffComparator)

        // bottom-left corner = maximal difference
        result[3] = Collections.max(srcPoints, diffComparator)
        return result.filterNotNull().reversed().toTypedArray()
    }

    private fun sortMat(matOfPoint: MatOfPoint): List<Point> {
        var a = arrayOf(0.0)
        var b = arrayOf(0.0)
        var c = arrayOf(0.0)
        var d = arrayOf(0.0)
        val sortedPoints: MutableList<Pair<Int, Double>> = ArrayList<Pair<Int, Double>>()

        if(matOfPoint.size().height == 4.0) {
            a = matOfPoint[0,0].toTypedArray()
            b = matOfPoint[1,0].toTypedArray()
            c = matOfPoint[2,0].toTypedArray()
            d = matOfPoint[3,0].toTypedArray()

            sortedPoints.add(Pair<Int, Double>(first = 1 , second = norm(Point(0.0, 0.0), Point(a[0], a[1]))))
            sortedPoints.add(Pair<Int, Double>(first = 2 , second = norm(Point(0.0, 0.0), Point(b[0], b[1]))))
            sortedPoints.add(Pair<Int, Double>(first = 3 , second = norm(Point(0.0, 0.0), Point(c[0], c[1]))))
            sortedPoints.add(Pair<Int, Double>(first = 4 , second = norm(Point(0.0, 0.0), Point(d[0], d[1]))))

            sortedPoints.sortedBy { it.second }
            //val normComparator = compareBy<Pair<Int, Double>>() { it.second }.thenBy{it.first}
            val ciao = "ciao"

        }

        return listOf<Point>(Point(0.0, 0.0))
    }

    private fun norm(a: Point, b: Point): Double {
        return sqrt( (a.x - b.y).pow(2) +(a.y - b.y).pow(2))
    }

    private fun sortLines(lines: Mat): List<Pair<Int, Double>> {
        val indices = lines.rows()
        val sortedLines: MutableList<Pair<Int, Double>> = ArrayList<Pair<Int, Double>>()

        if(indices < 200) {
            for (x in 0 until indices) {
                val l: DoubleArray? = lines.get(x, 0)
                sortedLines.add(
                    Pair(
                        first = x,
                        second = norm(Point(l!![0], l!![1]), Point(l!![2], l!![3]))
                    )
                )
            }

            val normComparator = compareBy<Pair<Int, Double>>() { it.second }
            return sortedLines.sortedWith(normComparator)
        }
        return sortedLines

    }

    private fun getNearestSegment(longest: DoubleArray, lines: Mat, index: Int): Int {
        var min = 100.0
        var idx = -1
        val indices = lines.rows()

        for (x in 0 until indices) {
            if(x != index) {
                val l: DoubleArray? = lines.get(x, 0)
                val difference = diff(longest, l!!)

                if(difference < min) {
                    min = difference
                    idx = x
                }
            }
        }
        return idx
    }

    private fun diff(longest: DoubleArray, line: DoubleArray): Double {
        val x1 = longest[0]
        val y1 = longest[1]
        val x2 = longest[2]
        val y2 = longest[3]

        val a1 = line[0]
        val b1 = line[1]
        val a2 = line[2]
        val b2 = line[3]

        val diff1 = sqrt((x1 - a1).pow(2) + (y1 - b1).pow(2))
        val diff2 = sqrt((x1 - a2).pow(2) + (y1 - b2).pow(2))
        val diff3 = sqrt((x2 - a1).pow(2) + (y2 - b1).pow(2))
        val diff4 = sqrt((x2 - a2).pow(2) + (y2 - b2).pow(2))

        val min1 = min(diff1, diff2)
        val min2 = min(diff3, diff4)

        return min(abs(min1), abs(min2))
    }

    private fun getSortedCorners(long: DoubleArray, short: DoubleArray): List<Point> {
        val epsilon = 0.03

        val x1 = long[0]
        val y1 = long[1]
        val x2 = long[2]
        val y2 = long[3]

        val a1 = short[0]
        val b1 = short[1]
        val a2 = short[2]
        val b2 = short[3]

        var approx = 100
        var case = "z"

        if(x1 - a1 < approx) {
            if(y1 < b1) {
                case = "a"
            } else {
                case = "b"
            }
        }

        if(x1 - a2 < approx) {
            if(y1 < b2) {
                case = "c"
            } else {
                case = "d"
            }
        }

        when(case) {
            "a" -> return listOf<Point>(Point(min(x1, a2), max(y1, y2)), Point(max(x1, a2), max(y1, y2)), Point(max(x1, a2), min(y1, b2)), Point(min(x1, a2), min(y1, b2)))
            "b" -> return listOf<Point>(Point(min(x1, a1), max(y1, y2)), Point(max(x1, a1), max(y2, y1)), Point(max(x2, a1), min(b1, y1)), Point(min(x2, a1), min(y2, y1)))
            "c" -> return listOf<Point>(Point(min(a1, a2), max(y1, y2)), Point(max(a2, a1), max(y1, y2)), Point(max(a1, a2), min(y1, y2)),  Point(min(a1, a2), min(y1, y2)))
            "d" -> return listOf<Point>(Point(min(a1, a2), max(y1, y2)), Point(max(a1, a2), max(y1, y2)), Point(max(a1, a2), min(y1, y2)), Point(min(a1, a2), min(y1, y2)))
        }
        return listOf(Point())
    }


}
/*  Ultimo BCK
fun run() {
        // Input file
        var dst = Mat()
        var cdstP = Mat()
        var cdst = Mat()

        val filename = "/storage/emulated/0/Download/IMG_20210309_122923.jpg"

        // Original Image
        var src = Imgcodecs.imread(filename, Imgcodecs.IMREAD_GRAYSCALE)
        var srcOr = Imgcodecs.imread(filename)

        // Canny Egde detection
        Imgproc.Canny(src, dst, 50.0, 200.0, 3, false)

        // Copio i contorni trovati sull'immagine originale
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR565)
        cdstP = cdst.clone()

        // Trasformata probabilistica
        val linesP: Mat = Mat()
        Imgproc.HoughLinesP(dst, linesP, 1.0, Math.PI / 180, 160, 200.0, 100.0)

        val indices = linesP.rows()
        var a: Double = src.height().toDouble()
        var b: Double = 0.0
        var c: Double = 0.0
        var d: Double = src.width().toDouble()

        for (x in 0 until indices) {
            val l: DoubleArray? = linesP.get(x, 0)
                if(l!![1] < a) {
                    a = l!![1]
                }

                if(l!![1] > c) {
                    c = l!![1]
                }

                if(l!![0] > b) {
                    b = l!![0]
                }

                if(l!![0] < d) {
                    d = l!![0]
                }
        }

//        Imgproc.line(
//            srcOr, Point(0.0, a), Point(src.width().toDouble(), a), Scalar(
//                0.0,
//                0.0,
//                255.0
//            ), 10, Imgproc.LINE_4, 0
//        )
//
//        Imgproc.line(
//            srcOr, Point(b, 0.0), Point(b, src.height().toDouble()), Scalar(
//                0.0,
//                0.0,
//                255.0
//            ), 10, Imgproc.LINE_4, 0
//        )
//
//        Imgproc.line(
//            srcOr, Point(0.0, c), Point(src.width().toDouble(), c), Scalar(
//                0.0,
//                0.0,
//                255.0
//            ), 10, Imgproc.LINE_4, 0
//        )
//
//        Imgproc.line(
//            srcOr, Point(d, 0.0), Point(d, src.height().toDouble()), Scalar(
//                0.0,
//                0.0,
//                255.0
//            ), 10, Imgproc.LINE_4, 0
//        )

        var va = Point(d, a)
        var vb = Point(b, a)
        var vc = Point(b, c)
        var vd = Point(d, c)



        var bmp: Bitmap? = null
        val tmp = Mat(src.height(), src.width(), CvType.CV_8U)
        try {
            //Imgproc.cvtColor(cdstP, tmp, Imgproc.COLOR_GRAY2BGR
            bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(srcOr, bmp)
            findViewById<ImageView>(R.id.image).setImageBitmap(bmp)

        } catch (e: CvException) {
            Log.d("Exception", e.message.toString())
        }

        return
    }

 */




/*
// Input file
        var dst = Mat()
        var cdstP = Mat()
        var cdst = Mat()

        val filename = "/storage/emulated/0/Download/IMG_20210309_122923.jpg"

        // Original Image
        var src = Imgcodecs.imread(filename, Imgcodecs.IMREAD_GRAYSCALE)

        // Canny Egde detection
        Imgproc.Canny(src, dst, 50.0, 200.0, 3, false)

        // Copio i contorni trovati sull'immagine originale
        Imgproc.cvtColor(dst, cdst, Imgproc.COLOR_GRAY2BGR565)
        cdstP = cdst.clone()

        // Trasformata probabilistica
        val linesP: Mat = Mat()
        Imgproc.HoughLinesP(dst, linesP, 1.0, Math.PI / 180, 50, 50.0, 10.0)

        val indices = linesP.rows()

        for (x in 0 until indices) {
            val l: DoubleArray? = linesP.get(x, 0)
            Imgproc.line(
                cdstP, Point(l!![0], l!![1]), Point(l!![2], l!![3]), Scalar(
                    0.0,
                    0.0,
                    255.0
                ), 3, Imgproc.LINE_AA, 0
            )
        }

        var bmp: Bitmap? = null
        val tmp = Mat(src.height(), src.width(), CvType.CV_8U)
        try {
            //Imgproc.cvtColor(cdstP, tmp, Imgproc.COLOR_GRAY2BGR
            bmp = Bitmap.createBitmap(tmp.cols(), tmp.rows(), Bitmap.Config.ARGB_8888)
            Utils.matToBitmap(cdstP, bmp)

        } catch (e: CvException) {
            Log.d("Exception", e.message)
        }
 */