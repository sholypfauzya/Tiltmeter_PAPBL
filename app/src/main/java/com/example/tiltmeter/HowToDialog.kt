package com.example.tiltmeter

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.example.tiltmeter.databinding.DialogHowToMeasureBinding

class HowToDialog(context: Context) : Dialog(context) {

    private lateinit var binding: DialogHowToMeasureBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DialogHowToMeasureBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val v = this.window?.decorView
        v?.setBackgroundResource(android.R.color.transparent)

        binding.btnUnderstand.setOnClickListener {
            dismiss()
        }
    }
}