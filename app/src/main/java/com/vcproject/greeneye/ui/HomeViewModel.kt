package com.vcproject.greeneye.ui

import android.app.Application
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vcproject.greeneye.data.DetectionResult
import com.vcproject.greeneye.util.YoloDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application){
    private val detector = YoloDetector(application)

    private val _uiState = MutableStateFlow<Bitmap?>(null)
    val uiState: StateFlow<Bitmap?> = _uiState.asStateFlow()

    private val _detectionResults = MutableStateFlow<List<DetectionResult>>(emptyList())
    val detectionResults = _detectionResults.asStateFlow()

    init {
        detector.init()
    }

    fun processImage(bitmap: Bitmap) {
        viewModelScope.launch(Dispatchers.IO) {
            //start to detect
            val resizedForModel = Bitmap.createScaledBitmap(bitmap, 640, 640, true)
            val results = detector.detect(resizedForModel)

            _detectionResults.value = results

            val scaleX = bitmap.width / 640f
            val scaleY = bitmap.height / 640f

            val mappedResults = results.map { res ->
                res.copy(
                    left = res.left * scaleX,
                    top = res.top * scaleY,
                    right = res.right * scaleX,
                    bottom = res.bottom * scaleY
                )
            }
            val resultBitmap = drawRects(bitmap, mappedResults)

           //update UI
            _uiState.value = resultBitmap
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

            val label = "${res.className} ${(res.score * 100).toInt()}%"

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

    fun clearResults() {
        _detectionResults.value = emptyList()
        _uiState.value = null
    }

    override fun onCleared() {
        super.onCleared()
        detector.close()
    }
}