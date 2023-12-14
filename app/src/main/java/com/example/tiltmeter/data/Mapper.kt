package com.example.tiltmeter.data

import com.github.mikephil.charting.data.Entry
import java.math.BigDecimal
import java.math.RoundingMode

fun Measurement.toEntry(): Entry {
    return Entry(seconds.toFloat(), degree.toFloat())
}

fun Entry.toMeasurement(): Measurement {
    return Measurement(x.toInt(), x.toInt(), BigDecimal(y.toDouble()).setScale(1, RoundingMode.HALF_UP).toDouble())
}