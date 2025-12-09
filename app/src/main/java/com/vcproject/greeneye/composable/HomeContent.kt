package com.vcproject.greeneye.composable

import android.graphics.Bitmap
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vcproject.greeneye.R
import com.vcproject.greeneye.data.DetectionResult
import com.vcproject.greeneye.util.YoloAnalyzer

@Composable
fun HomeContent(
    displayBitmap: Bitmap?,
    onTakePhotoClick: () -> Unit
) {

    var showDialog by remember { mutableStateOf(false) }


    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {
        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (displayBitmap != null) {
                Image(
                    bitmap = displayBitmap.asImageBitmap(),
                    contentDescription = "Result",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Please open the camera to identify the type of trash.",
                    modifier = Modifier.padding(20.dp),
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
            Image(
                painter = painterResource(id = R.mipmap.garbage_bin),
                contentDescription = "Garbage Bin Illustration",
                modifier = Modifier
                    .fillMaxWidth(0.9f)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))


        Button(
            onClick = {
                showDialog = false
                onTakePhotoClick()
            },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 30.dp)
                .fillMaxWidth(0.6f)
                .height(56.dp),
        ) {
            Text(text = "Open camera to detect", style = MaterialTheme.typography.titleMedium)
        }

    }
}

// RealTimeDetectionScreen
@Composable
fun RealTimeDetectionScreen(
    detectionResults: List<DetectionResult>,
    onFrameAnalyzed: (Bitmap) -> Unit,
    onBackClick: () -> Unit,
    onCaptureClick: () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    var debugBitmap by remember { mutableStateOf<Bitmap?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        // 1. camera prediction
        AndroidView(
            factory = { ctx ->
                PreviewView(ctx).apply {
                    scaleType = PreviewView.ScaleType.FILL_CENTER

                    val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
                    cameraProviderFuture.addListener({
                        val cameraProvider = cameraProviderFuture.get()

                        val preview = Preview.Builder().build()
                        preview.setSurfaceProvider(this.surfaceProvider)

                        val imageAnalysis = ImageAnalysis.Builder()
                            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                            .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                            .build()

                        imageAnalysis.setAnalyzer(
                            ContextCompat.getMainExecutor(ctx),
                            YoloAnalyzer { bitmap ->
                                debugBitmap = bitmap
                                onFrameAnalyzed(bitmap)
                            }
                        )

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis,
                            )
                        } catch (e: Exception) {
                            Log.e("Camera", "Fail to bind camera", e)
                        }
                    }, ContextCompat.getMainExecutor(ctx))
                }
            },
            modifier = Modifier.fillMaxSize()
        )

        // 2. Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val scaleX = size.width / 640f
            val scaleY = size.height / 640f

            val boxPaint = androidx.compose.ui.graphics.Paint().asFrameworkPaint().apply {
                color = android.graphics.Color.GREEN
                style = android.graphics.Paint.Style.STROKE
                strokeWidth = 5f
            }
            val textPaint = android.graphics.Paint().apply {
                color = android.graphics.Color.GREEN
                textSize = 40f
                style = android.graphics.Paint.Style.FILL
            }

            for (res in detectionResults) {
                // index
                val left = res.left * scaleX
                val top = res.top * scaleY
                val right = res.right * scaleX
                val bottom = res.bottom * scaleY

                // draw box
                drawContext.canvas.nativeCanvas.drawRect(
                    android.graphics.RectF(left, top, right, bottom),
                    boxPaint
                )

                // label name
                val label = "${res.category} (${res.specificName})${(res.score * 100).toInt()}%"
                drawContext.canvas.nativeCanvas.drawText(
                    label,
                    left,
                    top - 10f,
                    textPaint
                )
            }
        }

        CameraControls(
            modifier = Modifier.align(Alignment.BottomCenter),
            onCaptureClick = onCaptureClick,
            onBackClick = onBackClick
        )
        //this is only used when debug
        /*if (debugBitmap != null) {
            Box(
                modifier = Modifier
                    .size(150.dp)
                    .align(Alignment.TopStart)
                    .padding(16.dp)
                    .background(Color.Red)
                    .border(2.dp, Color.White)
            ) {
                Image(
                    bitmap = debugBitmap!!.asImageBitmap(),
                    contentDescription = "Model View",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.FillBounds
                )

                Text(
                    text = "Model Input\n${debugBitmap!!.width}x${debugBitmap!!.height}",
                    color = Color.Green,
                    fontSize = 10.sp,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f))
                )
            }
        }*/
    }
}


@Composable
fun CameraControls(
    modifier: Modifier = Modifier,
    onCaptureClick: () -> Unit,
    onBackClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(120.dp)
            .background(Color.Black.copy(alpha = 0.5f))
            .padding(horizontal = 30.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {


        IconButton(
            onClick = onBackClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = Color.White,
                modifier = Modifier.size(32.dp)
            )
        }

        Box(
            modifier = Modifier
                .size(80.dp)
                .clickable { onCaptureClick() },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawCircle(
                    color = Color.White,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4.dp.toPx())
                )
            }
            Canvas(modifier = Modifier.size(60.dp)) {
                drawCircle(color = Color.White)
            }
        }
        Spacer(modifier = Modifier.size(48.dp))
    }
}
