package com.vcproject.greeneye.ui

import android.app.Application
import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.provider.MediaStore
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vcproject.greeneye.data.DetectionResult
import com.vcproject.greeneye.util.YoloDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class HomeViewModel(application: Application) : AndroidViewModel(application){
    private val detector = YoloDetector(application)
    private var isCaptureRequested = false

    private val _detectionResults = MutableStateFlow<List<DetectionResult>>(emptyList())
    val detectionResults = _detectionResults.asStateFlow()

    private val _isCameraActive = MutableStateFlow(false)
    val isCameraActive = _isCameraActive.asStateFlow()

    init {
        detector.init()
    }

    fun capturePhoto() {
        isCaptureRequested = true
    }

    fun startDetection() {
        _isCameraActive.value = true
    }

    // close camera
    fun stopDetection() {
        _isCameraActive.value = false
        _detectionResults.value = emptyList()
    }

    fun processCameraFrame(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            // resize to 640* 640
            val results = detector.detect(bitmap)

            _detectionResults.value = results

            if (isCaptureRequested) {
                isCaptureRequested = false

                val bitmapToSave = drawRects(bitmap, results)

                saveBitmapToGallery(getApplication(), bitmapToSave)
            }
        }
    }

    private fun drawRects(bitmap: Bitmap, results: List<DetectionResult>): Bitmap {
        val mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(mutableBitmap)
        val boxPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 8f * (bitmap.width / 640f)
        }
        val textPaint = Paint().apply {
            color = Color.GREEN
            textSize = 40f * (bitmap.width / 640f)
            style = Paint.Style.FILL
        }

        val textBgPaint = Paint().apply {
            color = Color.parseColor("#80000000")
            style = Paint.Style.FILL
        }

        for (res in results) {
            // get the coordinate
            val rect = RectF(res.left, res.top, res.right, res.bottom)

            canvas.drawRect(rect, boxPaint)

            val label = "${res.category} (${res.specificName})${(res.score * 100).toInt()}%"

            val textHeight = textPaint.descent() - textPaint.ascent()
            val textOffset = 10f

            var textY = res.top - textOffset

            if (textY < textHeight) {
                textY = res.top + textHeight + textOffset
            }

            val textWidth = textPaint.measureText(label)
            canvas.drawRect(
                res.left,
                textY + textPaint.ascent(),
                res.left + textWidth + 20f,
                textY + textPaint.descent(),
                textBgPaint
            )

            canvas.drawText(label, res.left, textY, textPaint)
        }
        return mutableBitmap
    }

    private suspend fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
        try {
            val filename = "GreenEye_Overlay_${System.currentTimeMillis()}.jpg"

            val contentValues = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "DCIM/GreenEye")
                }
            }

            val resolver = context.contentResolver
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

            uri?.let {
                resolver.openOutputStream(it).use { stream ->
                    if (stream != null) {
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
                    }
                }
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, "Saved Image", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            withContext(Dispatchers.Main) {
                Toast.makeText(context, "Save Failure: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}