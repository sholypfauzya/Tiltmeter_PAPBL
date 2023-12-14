package com.example.tiltmeter

import android.app.Application
import com.orhanobut.hawk.Hawk

class TiltMeterApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        // initialize Hawk library for shared preference
        Hawk.init(this).build()
    }
}