package net.kuama.documentscanner.domain

import android.graphics.Bitmap
import net.kuama.documentscanner.support.Either
import net.kuama.documentscanner.support.Left
import net.kuama.documentscanner.support.Right
import net.kuama.scanner.data.Corners
import org.opencv.android.Utils
import org.opencv.core.*
import org.opencv.imgproc.Imgproc


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
        var dst = Mat()
        var cdst = Mat()

        // Original Image
        var src = Mat()

        //val bmp32 = params.bitmap.copy(Bitmap.Config.ARGB_8888, true)

        //Utils.bitmapToMat(bmp32, src)
        Imgproc.cvtColor(params.matrix, cdst, Imgproc.COLOR_BGR2GRAY)

        // Canny Egde detection
        Imgproc.Canny(cdst, dst, 50.0, 80.0, 3, false)

        // Copio i contorni trovati sull'immagine originale


        // Trasformata probabilistica
        val linesP: Mat = Mat()
        Imgproc.HoughLinesP(dst, linesP, 1.0, Math.PI / 180, 100, 200.0, 100.0)

        val indices = linesP.rows()
        var a: Double = src.height().toDouble()
        var b: Double = 0.0
        var c: Double = 0.0
        var d: Double = src.width().toDouble()

        for (x in 0 until indices) {
            val l: DoubleArray? = linesP.get(x, 0)
            if (l!![1] < a) {
                a = l!![1]
            }

            if (l!![1] > c) {
                c = l!![1]
            }

            if (l!![0] > b) {
                b = l!![0]
            }

            if (l!![0] < d) {
                d = l!![0]
            }
        }

        var va = Point(d, a)
        var vb = Point(b, a)
        var vc = Point(b, c)
        var vd = Point(d, c)

        val conf = Bitmap.Config.ARGB_8888 // see other conf types

        val points: List<Point?> = listOf(va, vb, vc, vd)
        var contours: MutableList<Int> = ArrayList()
        contours.add(1)

        Right(contours.firstOrNull()?.let {
            Pair(
                params.bitmap,
                Corners(
                    points, params.matrix.size()
                )
            )
        } ?: Pair(params.bitmap, null))

    } catch (throwable: Throwable) {
        Left(Failure(throwable))
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