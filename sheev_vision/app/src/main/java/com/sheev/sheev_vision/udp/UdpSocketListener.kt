package com.sheev.sheev_vision.udp

import android.util.Log
import java.io.IOException
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class UdpSocketListener(
    port: Int,
) {
    private var datagramSocket: DatagramSocket? = null
    private var buffer = ByteArray(1024)

    init {
        try {
            datagramSocket = DatagramSocket(port)
            datagramSocket?.broadcast = true
            datagramSocket?.reuseAddress = true
            datagramSocket?.soTimeout = 5000

            Log.d("UdpSocketListener", "UDP listener initialized on port $port")
        } catch (e: Exception) {
            Log.e("UdpSocketListener", "UDP initialization error", e)
        }
    }

    /**
     * Receives a UDP packet and processes its contents.
     *
     * @return A formatted string containing data from the UDP packet
     */
    fun receive(): String? {
        try {
            val packet = DatagramPacket(buffer, buffer.size)
            datagramSocket?.receive(packet)

            val message = String(packet.data, 0, packet.length)
            val senderIp = packet.address.hostAddress

            return handleMessage(message, senderIp ?: "unknown")
        } catch (e: IOException) {
            Log.e("UdpSocketListener", "Error receiving UDP packet", e)
        }

        return null
    }

    /**
     * Handles an incoming message and filters it based on specific content
     */
    private fun handleMessage(message: String, senderIp: String): String? {
        // 🔎 Filter by specific UDP message content
        if (UdpMessage.OPENED.name.equals(message, true) ||
            UdpMessage.CLOSED.name.equals(message, true)
        ) {
            return "$senderIp: $message - ${
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss"))
            }"
        }

        return null
    }

    fun stopListening() {
        datagramSocket?.close()
    }
}