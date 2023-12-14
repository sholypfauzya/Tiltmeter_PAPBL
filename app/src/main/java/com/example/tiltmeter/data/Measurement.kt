package com.example.tiltmeter.data

// object for measurement result
data class Measurement(
    val measurementId: Int,
    val seconds: Int,
    val degree: Double
)
