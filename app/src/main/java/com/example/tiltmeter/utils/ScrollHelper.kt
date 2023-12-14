package com.example.tiltmeter.utils

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.core.widget.NestedScrollView

@SuppressLint("ClickableViewAccessibility")
class ScrollHelper : View {

    var outerScrollView: NestedScrollView? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    init {
        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    // Disallow ScrollView to intercept touch events.
                    outerScrollView?.requestDisallowInterceptTouchEvent(true)
                    // Disable touch on transparent view
                    false
                }

                MotionEvent.ACTION_UP -> {
                    // Allow ScrollView to intercept touch events.
                    outerScrollView?.requestDisallowInterceptTouchEvent(true)
                    false
                }

                MotionEvent.ACTION_MOVE -> {
                    outerScrollView?.requestDisallowInterceptTouchEvent(true)
                    false
                }
                else -> true
            }
        }
    }
}
