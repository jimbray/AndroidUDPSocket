package xyz.jimbray.elevatorcontroller

import java.lang.Exception
import java.net.DatagramSocket
import java.net.InetAddress

class ServerSocket private constructor() {

    var isSocketClosed = false

    fun getSocket(): DatagramSocket {
        return socket
    }

    fun closeSocket() {
        if (socket != null) {
            println("Closing socket")
            try {
                socket.close()
                isSocketClosed = true
            } catch (e: Exception) {
                println("Error closing socket with " + e.message)
            }
        }
    }

    companion object {
        lateinit var socket: DatagramSocket
        private const val PORT = 9999
        private var instance: ServerSocket? = null
        fun getInstance(): ServerSocket {
            if (instance == null) {
                instance = ServerSocket()
            }
            if (socket == null) {
                socket = DatagramSocket(PORT, InetAddress.getLocalHost())
            }
            return instance!!
        }
    }

    init {
        instance = this
        try {
            socket = DatagramSocket(PORT, InetAddress.getLocalHost())
            isSocketClosed = false
        } catch (e: Exception) {
            e.printStackTrace()
            println("Error creating socket with " + e.message)
        }
    }
}