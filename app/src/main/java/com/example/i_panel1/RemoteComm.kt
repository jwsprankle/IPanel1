package com.example.i_panel1

import android.net.wifi.p2p.WifiP2pManager
import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket

class RemoteComm {

    private var inPackets = Channel<ByteArray>(120)
    private val ethPort = 62510
    private var rcvBuff = ByteArray(1024)
    private var rcvPacket: DatagramPacket = DatagramPacket(rcvBuff, rcvBuff.size)
    private var rcvSocket: DatagramSocket = DatagramSocket(ethPort)
    private var ethScope: CoroutineScope

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
        while (getData(1)?.isNotEmpty()) {
            notEmptyCount++
        }
    }

    init {
        // Create unique coroutine scope
        ethScope = CoroutineScope(Job() + Dispatchers.IO)

        // Kick off receiver
        ethScope.launch {
            startEthReceiver()
        }
    }


    private suspend fun startEthReceiver() {
        while (true) {
            try {
                rcvSocket.receive(rcvPacket)
                inPackets.send(rcvPacket.getData())
            } catch (e: IOException) {
                Log.e("Me Broke", "IOException: " + e.message)
            }
        }
    }
}






//private lateinit var wifi_p2p_channel: WifiP2pManager.Channel
//private lateinit var wifi_p2p_manager: WifiP2pManager
//private lateinit var wifi_p2p: WifiP2p
