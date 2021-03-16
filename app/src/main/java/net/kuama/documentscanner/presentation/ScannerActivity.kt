package net.kuama.documentscanner.presentation

import android.Manifest
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import net.kuama.documentscanner.R
import net.kuama.documentscanner.exceptions.NullCorners
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException

class ScannerActivity : BaseScannerActivity() {

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        for (hasPermission in permissions.values) {
            if (!hasPermission) {
                Toast.makeText(this, "Missing Required Permissions", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

//        requestPermissionLauncher.launch(
//            arrayOf(
//                Manifest.permission.READ_EXTERNAL_STORAGE,
//                Manifest.permission.WRITE_EXTERNAL_STORAGE,
//                Manifest.permission.CAMERA
//            )
//        )
    }

    override fun onError(throwable: Throwable) {
        when (throwable) {
            is NullCorners -> Toast.makeText(
                this,
                R.string.null_corners, Toast.LENGTH_LONG
            )
                .show()
            else -> Toast.makeText(this, throwable.message, Toast.LENGTH_LONG).show()
        }
    }

    override fun onDocumentAccepted(bitmap: Bitmap) {
        saveBitmapAsJpeg(bitmap, getOutputDirectory())
    }

    override fun onClose() {
        finish()
    }

    private fun saveBitmapAsJpeg(bitmap: Bitmap, folder: File) {
        try {
            val imageFile = File(folder, File.createTempFile("image", ".png").name)

            imageFile.createNewFile()
            val os = FileOutputStream(imageFile)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, os)
            os.flush()
            os.close()

        } catch (e: FileNotFoundException) {
            Log.e("MyTag", "FileNotFoundException : " + e.message)
        } catch (e: IOException) {
            Log.e("MyTag", "IOException : " + e.message)
        }
    }
}
