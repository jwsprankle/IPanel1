package com.example.i_panel1

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.Channel
import java.io.IOException
import java.net.DatagramSocket
import java.net.InetSocketAddress

class EthComm {

    val outPackets = Channel<ByteArray>(120)
    val inPackets = Channel<ByteArray>(120)
    private var udpSocket: DatagramSocket
    private var ethCommScope: CoroutineScope



    init {
        val sockAdd = InetSocketAddress("192.168.0.236", 62510)
        udpSocket = DatagramSocket()
        udpSocket.connect(sockAdd)
    }


    suspend fun drainExisting() {
        var notEmptyCount = 0
//        while(getData(1)?.isNotEmpty()) {
//            notEmptyCount++
//        }
    }

}