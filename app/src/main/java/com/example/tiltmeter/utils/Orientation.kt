package com.example.tiltmeter.utils

import android.app.Activity
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.view.Surface
import android.view.WindowManager

// class for managing and receiving sensor data
class Orientation(activity: Activity) : SensorEventListener {

    interface Listener {
        // interface for orientation sensor changed
        fun onOrientationChanged(pitch: Float, roll: Float, pitchDegree: Float, rollDegree: Float)
        // interface for sensor availability callback
        fun onSensorNotAvailable()
    }

    // instance for window manager
    private val mWindowManager: WindowManager = activity.window.windowManager
    // instance for sensor manager
    private val mSensorManager: SensorManager = activity.getSystemService(Activity.SENSOR_SERVICE) as SensorManager
    // instance for rotation vector sensor
    private val mRotationSensor: Sensor? = mSensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    // instance for Orientation Listener
    private var mListener: Listener? = null

    // function to start listening to sensor
    fun startListening(listener: Listener) {
        if (mListener === listener) {
            return
        }
        mListener = listener
        if (mRotationSensor == null) {
            // notify the listener if no sensor available
            mListener?.onSensorNotAvailable()
            return
        }
        // register sensor manager with rotation sensor and delay 16ms
        mSensorManager.registerListener(this, mRotationSensor, SENSOR_DELAY_MICROS)
    }

    // function to stop get sensor data
    fun stopListening() {
        mSensorManager.unregisterListener(this)
        mListener = null
    }

    // interface from SensorEventListener to get data from sensor accuracy
    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
    }

    // interface from SensorEventListener to get SensorEvent such as sensor values
    override fun onSensorChanged(event: SensorEvent) {
        if (mListener == null) {
            return
        }
        if (event.sensor == mRotationSensor) {
            updateOrientation(event.values)
        }
    }

    // function for process sensor data
    private fun updateOrientation(rotationVector: FloatArray) {
        // convert rotation vector to rotation matrix
        val rotationMatrix = FloatArray(9)
        SensorManager.getRotationMatrixFromVector(rotationMatrix, rotationVector)

        // adjust the rotation matrix based on device orientation
        val (worldAxisForDeviceAxisX, worldAxisForDeviceAxisY) = when (mWindowManager.defaultDisplay.rotation) {
            Surface.ROTATION_0 -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Z)
            Surface.ROTATION_90 -> Pair(SensorManager.AXIS_Z, SensorManager.AXIS_MINUS_X)
            Surface.ROTATION_180 -> Pair(SensorManager.AXIS_MINUS_X, SensorManager.AXIS_MINUS_Z)
            Surface.ROTATION_270 -> Pair(SensorManager.AXIS_MINUS_Z, SensorManager.AXIS_X)
            else -> Pair(SensorManager.AXIS_X, SensorManager.AXIS_Z)
        }

        val adjustedRotationMatrix = FloatArray(9)
        SensorManager.remapCoordinateSystem(
            rotationMatrix, worldAxisForDeviceAxisX,
            worldAxisForDeviceAxisY, adjustedRotationMatrix
        )

        // transform rotation matrix into azimuth/pitch/roll
        val orientation = FloatArray(3)
        SensorManager.getOrientation(adjustedRotationMatrix, orientation)

        // convert radians to degrees
        val pitch = orientation[1] * -57
        val roll = orientation[2] * -57

        // convert radians to degrees and format to one decimal place
        val pitchDegrees = String.format("%.1f", Math.toDegrees(orientation[1].toDouble())).toFloat()
        val rollDegrees = String.format("%.1f", Math.toDegrees(orientation[2].toDouble())).toFloat()

        // notify the listener about the orientation change
        mListener?.onOrientationChanged(pitch, roll, pitchDegrees, rollDegrees)
    }
}