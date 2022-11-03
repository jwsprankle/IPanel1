package com.example.i_panel1

import android.app.Activity
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Icon
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.MotionEvent
import android.view.Window
import android.view.WindowManager
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.view.marginStart
import androidx.core.view.marginTop
import com.example.usb1.McuUsbInterface
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.util.*
import kotlin.concurrent.timerTask
import kotlin.math.atan2

class MainActivity : AppCompatActivity() {

    var rotation = 0.0F

    lateinit var hdg_gauge: ImageView

    var gyroAxes = Axes3()
    var magAxesAvgSum = Axes3()
    var magAxesAvg = Axes3()
    var magCalbrMin = Axes3(-6856.0,-883.0, -4474.0)
    var magCalbrMax = Axes3(-379.0,5733.0, 2865.0)
    var magCalbrMid = Axes3()
    var magCalbrScale = axes3_1


    private val TOUCH_SCALE_FACTOR: Float = 180.0f / 320f
    private var previousX: Float = 0f
    private var previousY: Float = 0f
    private val displayMetrics = DisplayMetrics()
    private var lastTouchAngleDeg = 0.0
    private var gaugeRotationDeg = 0.0
    private var magAvgStack = MutableList(1) {Axes3()}
    private var magAvgNdx = 0
    private val gyroScale = (2000.0 * 50 * 2.0) / (Math.pow(2.0, 15.0) * 3330.0)


    private lateinit var gyroX_Text: TextView
    private lateinit var gyroY_Text: TextView
    private lateinit var gyroZ_Text: TextView

    private lateinit var magX_Text: TextView
    private lateinit var magY_Text: TextView
    private lateinit var magZ_Text: TextView

    private lateinit var magCalMinX_Text: TextView
    private lateinit var magCalMaxX_Text: TextView

    private lateinit var magCalMinY_Text: TextView
    private lateinit var magCalMaxY_Text: TextView

    private lateinit var magCalMinZ_Text: TextView
    private lateinit var magCalMaxZ_Text: TextView

    private lateinit var reseetCalBtn: Button

    /* USB system service */
    lateinit var usbDemo: McuUsbInterface

    lateinit var comms: EthComm

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        requestWindowFeature(Window.FEATURE_NO_TITLE)
        getSupportActionBar()?.hide()
        setContentView(R.layout.activity_main)

        hdg_gauge = findViewById(R.id.imageView)
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        // Attach USB interface
        usbDemo = McuUsbInterface(this)

        // Init Gyro text references
        gyroX_Text = findViewById(R.id.gyroX)
        gyroY_Text = findViewById(R.id.gyroY)
        gyroZ_Text = findViewById(R.id.gyroZ)

        // Init Mag text references
        magX_Text = findViewById(R.id.magX)
        magY_Text = findViewById(R.id.magY)
        magZ_Text = findViewById(R.id.magZ)

        magCalMinX_Text = findViewById(R.id.magCalMinX)
        magCalMaxX_Text = findViewById(R.id.magCalMaxX)

        magCalMinY_Text = findViewById(R.id.magCalMinY)
        magCalMaxY_Text = findViewById(R.id.magCalMaxY)

        magCalMinZ_Text = findViewById(R.id.magCalMinZ)
        magCalMaxZ_Text = findViewById(R.id.magCalMaxZ)

        reseetCalBtn = findViewById(R.id.button1)
        reseetCalBtn.setOnClickListener {
            magCalbrMin = Axes3(9999.9,9999.9, 9999.9)
            magCalbrMax = Axes3(-9999.9,-9999.9, -9999.9)
            magCalbrMid = Axes3()
        }


        GlobalScope.launch(Dispatchers.IO) {
            comms = EthComm()
        }


        GlobalScope.launch(Dispatchers.Main) {
            startGyroInterface()
        }


//        startTimeCounter()
    }


    override fun onTouchEvent(e: MotionEvent): Boolean {
        // MotionEvent reports input details from the touch screen
        // and other input controls. In this case, you are only
        // interested in events where the touch position changed.

        val touchX: Float = e.x
        val touchY: Float = e.y
        val gaugeWidth = hdg_gauge.width
        val gaugeHeight = hdg_gauge.height
        val gaugeStartX = hdg_gauge.marginStart
        val gaugeStartY = hdg_gauge.marginTop
        val gaugeCenterX = gaugeStartX + (hdg_gauge.width / 2)
        val gaugeCenterY = gaugeStartY + (hdg_gauge.height / 2)

//        val width = displayMetrics.widthPixels
//        val height = displayMetrics.heightPixels

        val gaugeTouchX = touchX - gaugeCenterX
        val gaugeTouchY = gaugeCenterY - touchY

        var touchAngleDeg = Math.toDegrees(atan2(gaugeTouchY, gaugeTouchX).toDouble())


        when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                gaugeRotationDeg += (lastTouchAngleDeg - touchAngleDeg)
                hdg_gauge.setRotation(gaugeRotationDeg.toFloat())
                lastTouchAngleDeg = touchAngleDeg
            }

            MotionEvent.ACTION_DOWN -> {
                lastTouchAngleDeg = touchAngleDeg
            }
    }

//        when (e.action) {
//            MotionEvent.ACTION_MOVE -> {
//
//                var dx: Float = touchX - previousX
//                var dy: Float = touchY - previousY
//
//                var angleDeg = Math.toDegrees(atan2(touchY,touchX).toDouble())
//                hdg_gauge.setRotation(angleDeg.toFloat())
//
//
//                // reverse direction of rotation above the mid-line
//                if (touchY > height / 2) {
//                    dx *= -1
//                }
//
//                // reverse direction of rotation to left of the mid-line
//                if (touchX < width / 2) {
//                    dy *= -1
//                }
//
////                renderer.angle += (dx + dy) * TOUCH_SCALE_FACTOR
////                requestRender()
//            }
//        }

        previousX = touchX
        previousY = touchY
        return true
    }

    private fun rotateMe() {
        rotation = (rotation + 0.3F).mod(360F)
        hdg_gauge.setRotation(rotation)
//        hdg_gauge.setRotationX(45.0f)
    }

    private fun startTimeCounter() {

        Timer().scheduleAtFixedRate(timerTask {
            rotateMe()
        },0,33)
    }

    private fun updateText(axes: Axes3, xText:TextView, yText:TextView, zText: TextView ) {
        xText.text = String.format("%.3f", axes.axisX)
        yText.text = String.format("%.3f", axes.axisY)
        zText.text = String.format("%.3f", axes.axisZ)
    }

    private fun updateTextInt(axes: Axes3, xText:TextView, yText:TextView, zText: TextView ) {
        xText.text = axes.axisX.toInt().toString()
        yText.text = axes.axisY.toInt().toString()
        zText.text = axes.axisZ.toInt().toString()
    }

    private fun calcMagAvg(newMagAxes: Axes3) {

        // remove old element from average, replace with newMagAxes
        magAxesAvgSum = magAxesAvgSum.subtract(magAvgStack[magAvgNdx])
        magAxesAvgSum = magAxesAvgSum.add(newMagAxes)
        magAvgStack[magAvgNdx] = newMagAxes

        // Calc new magAxesAvg
        val divisor = Axes3(magAvgStack.size.toDouble(), magAvgStack.size.toDouble() ,magAvgStack.size.toDouble())
        magAxesAvg = magAxesAvgSum.divide(divisor)

        // Advance ndx and check for rollover
        magAvgNdx++
        if (magAvgNdx >= magAvgStack.size) {
            magAvgNdx = 0
        }
    }

    private fun updateMagScale(magVal: Axes3) {

        // Determine min
        if (magVal.axisX < magCalbrMin.axisX) {
            magCalbrMin.axisX = magVal.axisX
        }
        if (magVal.axisY < magCalbrMin.axisY) {
            magCalbrMin.axisY = magVal.axisY
        }
        if (magVal.axisZ < magCalbrMin.axisZ) {
            magCalbrMin.axisZ = magVal.axisZ
        }

        // Determine max
        if (magVal.axisX > magCalbrMax.axisX) {
            magCalbrMax.axisX = magVal.axisX
        }
        if (magVal.axisY > magCalbrMax.axisY) {
            magCalbrMax.axisY = magVal.axisY
        }
        if (magVal.axisZ > magCalbrMax.axisZ) {
            magCalbrMax.axisZ = magVal.axisZ
        }
    }

    private fun updateMagCalbr() {
        magCalbrMid = magCalbrMax.add(magCalbrMin).divide(axes3_2)
        magCalbrScale = axes3_1.divide(magCalbrMax.subtract(magCalbrMin).divide(axes3_2))
    }

    private fun magScale(magVal: Axes3): Axes3 {
        var diff = magVal.subtract(magCalbrMid)
        var scaled = diff.multiply(magCalbrScale)
        return diff
    }


    suspend fun startGyroInterface() {

        GlobalScope.launch(Dispatchers.Main) {

            var testCount = 0
            var newData: ByteArray

            // Drain existing data
            usbDemo.drainExisting()

            while(true) {
                newData = usbDemo.getData(100)
                if (newData.isNotEmpty()) {

                    // Get and convert Gyro data
                    gyroAxes = Axes3.create(newData, 0, gyroScale)
                    gyroAxes.axisZ += -0.012  // Compensate for constant drift

                    // Get raw acc
                    var rawAccAxes = Axes3.create(newData, 6)

                    // Get raw mag
                    var rawMagAxes = Axes3.create(newData, 12)

                    // Average mag Axes
                    calcMagAvg(rawMagAxes)

                    // Update Mag min/max
                    updateMagScale(magAxesAvg)

                    // Update Mag Calibration
                    updateMagCalbr()

                    // Update text
                    updateText(gyroAxes, gyroX_Text, gyroY_Text, gyroZ_Text)
//                    updateTextInt(rawAccAxes, gyroX_Text, gyroY_Text, gyroZ_Text)
                    updateTextInt(magScale(magAxesAvg), magX_Text, magY_Text, magZ_Text)
                    updateTextInt(magCalbrMin, magCalMinX_Text, magCalMinY_Text, magCalMinZ_Text)
                    updateTextInt(magCalbrMax, magCalMaxX_Text, magCalMaxY_Text, magCalMaxZ_Text)

                    var magAvg = magScale(magAxesAvg)
                    var magHdgRad = atan2(magAvg.axisY, magAvg.axisX)
                    var magHdgDeg = (Math.toDegrees(-magHdgRad) - 90.0)

                    hdg_gauge.setRotation(-magHdgDeg.toFloat())


                      gaugeRotationDeg -= gyroAxes.axisZ
//                      hdg_gauge.setRotation(gaugeRotationDeg.toFloat())


//                    magHeading = (atan2(magAvg.axisY, magAvg.axisX) * -180.0) / Math.PI
//                    magHeading = -magHeading + 90.0



//                    // Rotate dial if over 0.05 deg/sec
//                    if ((gyroAxes.axisZ > 0.05) || (gyroAxes.axisZ  < -0.05)) {
//                        gaugeRotationDeg -= gyroAxes.axisZ
//                        hdg_gauge.setRotation(gaugeRotationDeg.toFloat())
//                    }
//                    else {
//                        var curCompass = magScale(magAxesAvg)
//                        var rotAngle = atan2(curCompass.axisY, curCompass.axisX)
//                        rotAngle = (rotAngle * 180.0) / Math.PI
//                        rotAngle = -rotAngle + 90.0
//                        var rotDiff = gaugeRotationDeg - rotAngle
//                        if ((rotDiff < -1.0) || (rotDiff > 1.0 )) {
//                            hdg_gauge.setRotation(rotAngle.toFloat())
//                            gaugeRotationDeg = rotAngle
//                        }
//                    }

                }
            }
        }
    }

    suspend fun startEthCommInterface() {

        GlobalScope.launch(Dispatchers.IO) {

        }
    }
}

