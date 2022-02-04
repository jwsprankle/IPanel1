package com.example.i_panel1

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.Window
import android.widget.ImageView
import java.util.*
import kotlin.concurrent.timerTask

class MainActivity : AppCompatActivity() {

    var rotation = 0.0F

    lateinit var hdg_gauge: ImageView

    private val TOUCH_SCALE_FACTOR: Float = 180.0f / 320f
    private var previousX: Float = 0f
    private var previousY: Float = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar()?.hide();
        setContentView(R.layout.activity_main)

        hdg_gauge = findViewById(R.id.imageView)

        startTimeCounter()
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        val touchX: Float = e.x
        val touchY: Float = e.y

        val displayMetrics = DisplayMetrics()
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        var width = displayMetrics.widthPixels
        var height = displayMetrics.heightPixels

        when (e.action) {
            MotionEvent.ACTION_MOVE -> {

                var dx: Float = touchX - previousX
                var dy: Float = touchY - previousY

                // reverse direction of rotation above the mid-line
                if (touchY > height / 2) {
                    dx *= -1
                }

                // reverse direction of rotation to left of the mid-line
                if (touchX < width / 2) {
                    dy *= -1
                }

//                renderer.angle += (dx + dy) * TOUCH_SCALE_FACTOR
//                requestRender()
            }
        }

        previousX = touchX
        previousY = touchY
        return true
    }

    fun rotateMe() {
        rotation = (rotation + 0.3F).mod(360F)
        hdg_gauge.setRotation(rotation)
//        hdg_gauge.setRotationX(45.0f)
    }

    fun startTimeCounter() {

        Timer().scheduleAtFixedRate(timerTask {
            rotateMe()
        },0,8)
    }
}