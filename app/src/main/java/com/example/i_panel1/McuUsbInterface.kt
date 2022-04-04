package com.example.usb1

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.XmlResourceParser
import android.hardware.usb.*
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.i_panel1.R
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserException
import java.io.IOException

//*****************************************************************************
//** TODO:
// 2. Look at better method to close()
// 3. Handle usb timeouts
// 4. Add send receive accessors
//*****************************************************************************

//*****************************************************************************
// Notes:
//
// USB Device
// Regards to thread safeness, read following
// https://developer.android.com/guide/topics/connectivity/usb/host
// *****************************************************************************

class McuUsbInterface(context: Context) {

    val outPackets = Channel<ByteArray>(120)
    val inPackets = Channel<ByteArray>(120)

    val vendorId: Int
        get() = usbVendorId

    val productId: Int
        get() = usbProductId

    val maxPacketSize: Int
        get() = usbMaxPacketSize


    private var mcuUsbScope: CoroutineScope

    lateinit var some_crap: TextView

    private var usbVendorId: Int = 0
    private var usbProductId: Int = 0

    private val usbInterfaceNdx = 1
    private val usbMaxPacketSize = 64

    private val rcvDataBuff = ByteArray(usbMaxPacketSize)

    private val TAG = "UsbEnumerator"
    private var usbManager: UsbManager? = null
    private var usbDevice: UsbDevice? = null
    private var usbEndpointBlkIn: UsbEndpoint? = null
    private var usbEndpointBlkOut: UsbEndpoint? = null
    private var usbDeviceConnection: UsbDeviceConnection? = null
    private var usbInterface: UsbInterface? = null

    // Loop back test data
    private var testCount = 0
    private var errorCount = 0
    private var goodCount = 0
    private var packetRate = 0

    /**
     * Broadcast receiver to handle USB connect events.
     */
    private var usbReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context, intent: Intent) {
            // Check for disconnect
            if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                shutDownUsb()
                Log.v(TAG, "USB Detached!")
            } else if (UsbManager.ACTION_USB_DEVICE_ATTACHED == intent.action) {
                val newUsbDevice: UsbDevice? =
                    intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)
                initUsbDevice(newUsbDevice)
                Log.v(TAG, "USB Attached!")
            }
        }
    }

    /**
     *  Get data, set waitTime != 0 to wait for data
     *  set waitTime to 0 to wait indefinitely
     */
    suspend fun getData(waitTime: Int = 0): ByteArray {
        // See if we need to wait for data
        if (waitTime != 0) {
            var waitTimer = waitTime
            while (inPackets.isEmpty && (waitTimer != 0)) {
                delay(1)
                waitTimer--
            }
            if (waitTimer == 0) {  // Exit with no data if timed out
                return ByteArray(0)
            }
        }
        // Get data and return
        return inPackets.receive()
    }

    suspend fun drainExisting() {
        var notEmptyCount = 0
        while(getData(1)?.isNotEmpty()) {
            notEmptyCount++
        }
    }


    /**
     * Initial setup
     */
    init {
        // Create unique coroutine scope
        mcuUsbScope = CoroutineScope(Job() + Dispatchers.IO)

        // Setup detach filter
        val detachFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_DETACHED)
        context.registerReceiver(usbReceiver, detachFilter)

        // Setup Attached filter
        val attachFilter = IntentFilter(UsbManager.ACTION_USB_DEVICE_ATTACHED)
        context.registerReceiver(usbReceiver, attachFilter)

        // Get USB service
        usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager

        // Get usb resource info, this should provide usb vendor and product id's
        getResourceInfo(context)

        // See if device already attached
        val deviceList = usbManager?.deviceList
        deviceList?.values?.forEach { device ->
            if ((device.vendorId == usbVendorId) && (device.productId == usbProductId)) {
                initUsbDevice(device)
            }
        }
    }

    /**
     * close out,
     * shutDownUsb will insure that usbJob is canceled when this class is closed
     */
    fun close() {
        shutDownUsb()
    }


    /**
     *  Initialize USB device
     *      Called once our usb device has been discovered
     *      Establish:
     *          Device connection
     *          Interface
     *          Endpoints
     */
    private fun initUsbDevice(newUsbDevice: UsbDevice?): Boolean {

        // Shut down existing
        shutDownUsb()

        // Bail if not the device we are looking for
        if (
            (newUsbDevice == null) ||
            (newUsbDevice.vendorId != usbVendorId) ||
            (newUsbDevice.productId != usbProductId) ||
            (newUsbDevice.interfaceCount < usbInterfaceNdx)
        ) {
            return false
        }

        // Get interface
        // **** TODO: Add additional test to insure we have correct UsbInterface
        var tmpUsbInterface = newUsbDevice.getInterface(usbInterfaceNdx)

        val tmpUsbEndpointBlkOut = tmpUsbInterface.getEndpoint(0) ?: return false
        val tmpUsbEndpointBlkIn = tmpUsbInterface.getEndpoint(1) ?: return false
        val tmpUsbDeviceConnection = usbManager?.openDevice(newUsbDevice)?.apply {
            claimInterface(tmpUsbInterface, true)
        } ?: return false

        // All good
        usbDevice = newUsbDevice
        usbEndpointBlkIn = tmpUsbEndpointBlkIn
        usbEndpointBlkOut = tmpUsbEndpointBlkOut
        usbDeviceConnection = tmpUsbDeviceConnection
        usbInterface = tmpUsbInterface

        // Kick off USB device

        mcuUsbScope.launch {
            startUsbTransmiter()
        }

        mcuUsbScope.launch {
            startUsbReceiver()
        }

        return true
    }

    /**
     * Shutdown USB device
     *  Normally called when USB device is disconnected
     *  or when McuUsbInterface is destroyed
     */
    private fun shutDownUsb() {
        usbDevice = null
        usbEndpointBlkIn = null
        usbEndpointBlkOut = null
        usbDeviceConnection = null
        usbInterface = null
    }


    /**
     *  Kick off USB receiver "USB IN"
     * */
    private suspend fun startUsbReceiver() {
        var counter = 0
        var lastSet: ByteArray
        var lastSetSize = 0
        while (true) {
            var xferLen = usbDeviceConnection?.bulkTransfer(
                usbEndpointBlkIn,
                rcvDataBuff,
                usbMaxPacketSize,
                250
            ) ?: 0

            // Forward packet to out receive channel
            if (xferLen > 0) {
                val subSet: ByteArray = rcvDataBuff.sliceArray(0 until xferLen)
                inPackets.send(subSet)
            }
        }
    }


    /**
     * Kick off USB transmitter "USB OUT"
     */
    private suspend fun startUsbTransmiter() {
        while (true) {
            val xmtPacket = outPackets.receive()
            usbDeviceConnection?.bulkTransfer(
                usbEndpointBlkOut,
                xmtPacket,
                xmtPacket.size,
                250
            )
        }
    }


    /**
     * Get usb device info from res/xml/device_filter.xml
     * Return false if error
     */
    fun getResourceInfo(context: Context): Boolean {
        var retStatus = false
        try {
            val parser: XmlResourceParser = context.resources.getXml(R.xml.device_filter)
            var eventType = parser.eventType

            // Search for usb id's
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG) {
                    if (parser.name == "usb-device") {
                        usbProductId = parser.getAttributeIntValue(0, 0)
                        usbVendorId = parser.getAttributeIntValue(1, 0)
                        retStatus = true
                        break
                    }
                }
                eventType = parser.next()
            }
        } catch (e: XmlPullParserException) {
            e.printStackTrace()
            retStatus = false
        } catch (e: IOException) {
            e.printStackTrace()
            retStatus = false
        }
        return retStatus
    }
}


