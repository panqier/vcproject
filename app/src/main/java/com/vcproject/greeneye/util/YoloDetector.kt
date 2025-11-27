package com.vcproject.greeneye.util

import android.content.Context
import android.graphics.Bitmap
import com.vcproject.greeneye.data.DetectionResult
import org.tensorflow.lite.Interpreter
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.channels.FileChannel
import kotlin.math.max
import kotlin.math.min


class YoloDetector(private val context: Context) {

    private val MODEL_PATH = "bestloss_float32.tflite"
    private val INPUT_SIZE = 640
    private val LABELS = listOf(
        "BIODEGRADABLE", "CARDBOARD", "GLASS", "METAL", "PAPER", "PLASTIC"
    )
    private val OUTPUT_CHANNELS = 4 + LABELS.size
    private val OUTPUT_ANCHORS = 8400
    private val CONFIDENCE_THRESHOLD = 0.45f
    private val IOU_THRESHOLD = 0.5f

    private var interpreter: Interpreter? = null

    // init model
    fun init() {
        try {
            val options = Interpreter.Options().apply {
                numThreads = 4
            }
            //load model
            val modelFile = loadModelFile(MODEL_PATH)
            interpreter = Interpreter(modelFile, options)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // detect function
    fun detect(bitmap: Bitmap): List<DetectionResult> {
        if (interpreter == null) return emptyList()

        val resizedBitmap = Bitmap.createScaledBitmap(bitmap, INPUT_SIZE, INPUT_SIZE, true)
        val inputBuffer = convertBitmapToByteBuffer(resizedBitmap)

        val outputBuffer = ByteBuffer.allocateDirect(1 * OUTPUT_CHANNELS * OUTPUT_ANCHORS * 4)
        outputBuffer.order(ByteOrder.nativeOrder())

        interpreter?.run(inputBuffer, outputBuffer)

        return parseOutput(outputBuffer)
    }

    fun close() {
        interpreter?.close()
        interpreter = null
    }


    private fun loadModelFile(path: String): java.nio.MappedByteBuffer {
        val fileDescriptor = context.assets.openFd(path)
        val inputStream = FileInputStream(fileDescriptor.fileDescriptor)
        val fileChannel = inputStream.channel
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, fileDescriptor.startOffset, fileDescriptor.declaredLength)
    }

    private fun convertBitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
        val byteBuffer = ByteBuffer.allocateDirect(1 * INPUT_SIZE * INPUT_SIZE * 3 * 4)
        byteBuffer.order(ByteOrder.nativeOrder())
        val intValues = IntArray(INPUT_SIZE * INPUT_SIZE)
        bitmap.getPixels(intValues, 0, INPUT_SIZE, 0, 0, INPUT_SIZE, INPUT_SIZE)
        for (pixelValue in intValues) {
            byteBuffer.putFloat(((pixelValue shr 16) and 0xFF) / 255.0f)
            byteBuffer.putFloat(((pixelValue shr 8) and 0xFF) / 255.0f)
            byteBuffer.putFloat((pixelValue and 0xFF) / 255.0f)
        }
        return byteBuffer
    }

    private fun parseOutput(byteBuffer: ByteBuffer): List<DetectionResult> {
        byteBuffer.rewind()
        val floatArray = FloatArray(OUTPUT_CHANNELS * OUTPUT_ANCHORS)
        byteBuffer.asFloatBuffer().get(floatArray)
        val detections = ArrayList<DetectionResult>()
        for (i in 0 until OUTPUT_ANCHORS) {
            var maxScore = 0f
            var classIndex = -1
            for (j in 0 until (OUTPUT_CHANNELS - 4)) {
                val score = floatArray[(4 + j) * OUTPUT_ANCHORS + i]
                if (score > maxScore) {
                    maxScore = score
                    classIndex = j
                }
            }
            if (maxScore > CONFIDENCE_THRESHOLD) {
                val x = floatArray[0 * OUTPUT_ANCHORS + i]
                val y = floatArray[1 * OUTPUT_ANCHORS + i]
                val w = floatArray[2 * OUTPUT_ANCHORS + i]
                val h = floatArray[3 * OUTPUT_ANCHORS + i]
                val left = (x - w / 2) * INPUT_SIZE
                val top = (y - h / 2) * INPUT_SIZE
                val right = (x + w / 2) * INPUT_SIZE
                val bottom = (y + h / 2) * INPUT_SIZE
                detections.add(DetectionResult(
                    classIndex,
                    className = LABELS.getOrElse(classIndex) { "Unknown" },
                    maxScore,
                    left, top, right, bottom)
                )
            }
        }
        return nms(detections)
    }

    private fun nms(detections: ArrayList<DetectionResult>): List<DetectionResult> {
        val nmsList = ArrayList<DetectionResult>()
        detections.sortByDescending { it.score }
        while (detections.isNotEmpty()) {
            val best = detections.removeAt(0)
            nmsList.add(best)
            val iterator = detections.iterator()
            while (iterator.hasNext()) {
                val other = iterator.next()
                if (calculateIoU(best, other) > IOU_THRESHOLD) iterator.remove()
            }
        }
        return nmsList
    }

    private fun calculateIoU(a: DetectionResult, b: DetectionResult): Float {
        val areaA = (a.right - a.left) * (a.bottom - a.top)
        val areaB = (b.right - b.left) * (b.bottom - b.top)
        val intersectionLeft = max(a.left, b.left)
        val intersectionTop = max(a.top, b.top)
        val intersectionRight = min(a.right, b.right)
        val intersectionBottom = min(a.bottom, b.bottom)
        val intersectionArea = max(0f, intersectionRight - intersectionLeft) * max(0f, intersectionBottom - intersectionTop)
        return intersectionArea / (areaA + areaB - intersectionArea)
    }
}