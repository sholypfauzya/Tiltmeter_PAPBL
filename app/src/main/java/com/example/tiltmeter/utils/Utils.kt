package com.example.tiltmeter.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.Log
import androidx.annotation.DrawableRes
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory

fun doDebug(text: String) {
    Log.d("TiltMeter", text)
}

// function to check if WRITE_EXTERNAL_STORAGE permission is granted
fun checkStoragePermission(context: Context): Boolean {
    // check if the device is running on Android version TIRAMISU or later
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        // permissions are automatically granted on Android TIRAMISU and later
        true
    } else {
        // check if WRITE_EXTERNAL_STORAGE permission is granted on earlier versions
        ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}

// function to check if WRITE_EXTERNAL_STORAGE permission is granted
fun checkLocationPermission(context: Context): Boolean {
    // check if the device is running on Android version TIRAMISU or later
    val fineLocationPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
    val coarseLocationPermission = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
    return fineLocationPermission == PackageManager.PERMISSION_GRANTED && coarseLocationPermission == PackageManager.PERMISSION_GRANTED
}

// function to request WRITE_EXTERNAL_STORAGE permission
fun requestStoragePermission(activity: Activity) {
    // check if WRITE_EXTERNAL_STORAGE permission is not granted
    if (!checkStoragePermission(activity)) {
        // request permission based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // request permission using the new approach for Android R and later
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                REQUEST_STORAGE_PERMISSION
            )
        } else {
            // request permission using the traditional approach for earlier Android versions
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(Manifest.permission.WRITE_EXTERNAL_STORAGE),
                REQUEST_STORAGE_PERMISSION
            )
        }
    }
}

// function to request WRITE_EXTERNAL_STORAGE permission
fun requestLocationPermission(activity: Activity) {
    // check if WRITE_EXTERNAL_STORAGE permission is not granted
    if (!checkLocationPermission(activity)) {
        // request permission based on Android version
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // request permission using the new approach for Android R and later
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
                REQUEST_STORAGE_PERMISSION
            )
        } else {
            // request permission using the traditional approach for earlier Android versions
            ActivityCompat.requestPermissions(
                activity,
                arrayOf(
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION,
                ),
                REQUEST_STORAGE_PERMISSION
            )
        }
    }
}

fun getBitmapDescriptorFromResource(context: Context, @DrawableRes drawableRes: Int): BitmapDescriptor? {
    val drawable: Drawable? = ContextCompat.getDrawable(context, drawableRes)
    val canvas = Canvas()
    var bitmap: Bitmap? = null
    if (drawable != null) {
        bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        canvas.setBitmap(bitmap)
        drawable.setBounds(0, 0, drawable.intrinsicWidth, drawable.intrinsicHeight)
        drawable.draw(canvas)
    }
    return bitmap?.let { BitmapDescriptorFactory.fromBitmap(it) }
}

inline fun <reified T : Context> Context.newIntent(): Intent =
    Intent(this, T::class.java)

inline fun <reified T : Activity> Activity.startActivity(): Unit =
    this.startActivity(newIntent<T>())

class TimeValueFormatter : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        // Format the timeInSeconds value as needed for X-axis labels
        return "${value.toInt()}s"
    }
}

class DegreeValueFormatter : ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        // Format the timeInSeconds value as needed for X-axis labels
        return "${value.toInt()}\u00B0"
    }
}