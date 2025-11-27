package com.vcproject.greeneye.data

data class DetectionResult(
    val classIndex: Int,
    val className: String,
    val score: Float,
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {

}
