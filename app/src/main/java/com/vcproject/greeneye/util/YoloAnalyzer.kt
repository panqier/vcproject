package com.vcproject.greeneye.util

import android.graphics.Bitmap
import android.graphics.Matrix
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class YoloAnalyzer(
    private val onFrameDetected: (Bitmap) -> Unit
) : ImageAnalysis.Analyzer {

    private var lastAnalyzedTimestamp = 0L
    private val INTERVAL = 500L // 500ms detect once

    override fun analyze(image: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()

        if (currentTimestamp - lastAnalyzedTimestamp < INTERVAL) {
            image.close()
            return
        }

        lastAnalyzedTimestamp = currentTimestamp

        val bitmap = image.toBitmap()
        //rotation camerax
        val rotationDegrees = image.imageInfo.rotationDegrees

        val finalBitmap = if (rotationDegrees != 0) {
            val matrix = Matrix()
            matrix.postRotate(rotationDegrees.toFloat())
            // create a new bitmap with the rotated image
            Bitmap.createBitmap(
                bitmap,
                0, 0,
                bitmap.width,
                bitmap.height,
                matrix,
                true
            )
        } else {
            bitmap
        }

        onFrameDetected(finalBitmap)

        image.close()
    }
}