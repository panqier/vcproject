package com.vcproject.greeneye.composable

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.vcproject.greeneye.data.DetectionResult

@Composable
fun HomeContent(
    displayBitmap: android.graphics.Bitmap?,
    detectionResults: List<DetectionResult>,
    onTakePhotoClick: () -> Unit
) {

    var showDialog by remember { mutableStateOf(false) }

    LaunchedEffect(detectionResults) {
        if (detectionResults.isNotEmpty()) {
            showDialog = true
        }
    }


    Box(
        modifier = Modifier
            .fillMaxSize()

    ) {

        if (displayBitmap != null) {
            Image(
                bitmap = displayBitmap.asImageBitmap(),
                contentDescription = "Result",
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            )
        } else {

            Text(
                text = "Please click below button to click",
                modifier = Modifier.align(Alignment.Center)
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


        if (showDialog && detectionResults.isNotEmpty()) {
            ResultPopup(
                results = detectionResults,
                onDismiss = { showDialog = false }
            )
        }
    }
}


@Composable
fun ResultPopup(
    results: List<DetectionResult>,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Detection Result") },
        text = {
            Column {
                results.forEach { res ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {

                        Text(
                            text = res.className,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f)
                        )

                        Text(
                            text = "${(res.score * 100).toInt()}%",
                            color = Color(0xFF388E3C),
                            style = MaterialTheme.typography.bodyLarge
                        )
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = Color.LightGray)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("ok")
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(28.dp)
    )
}