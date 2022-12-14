package xyz.jimbray.elevatorcontroller

import android.content.Context
import android.net.wifi.WifiManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.InetSocketAddress
import java.text.SimpleDateFormat
import java.util.*

class MainViewModel : ViewModel() {

    var receiveText: String by mutableStateOf("")
    var sendText: String by mutableStateOf("")

    var serverRunning: Boolean by mutableStateOf(false)

    private val CLIENT_PORT = 8888


    private var wifiLock: WifiManager.MulticastLock? = null

    fun initWifiLock() {
        if (wifiLock == null) {
            val manager = App.getAppContext().applicationContext
                .getSystemService(Context.WIFI_SERVICE) as WifiManager
            wifiLock = manager.createMulticastLock("wifi")
        }
    }

    fun startUdpServer() {
        viewModelScope.launch(Dispatchers.Default) {
            flow {
                val host = InetAddress.getLocalHost().hostAddress
                emit(host)
            }
                .flowOn(Dispatchers.Default)
                .catch {
                    if (ServerSocket.getInstance().isSocketClosed) {
                        "Server is closed".log_d()
                        serverRunning = false
                    }
                }
                .collect {
                    it.log_d()
                    val ds = ServerSocket.getInstance().getSocket()

                    var buf = ByteArray(12)

                    var dp = DatagramPacket(buf, buf.size)

                    serverRunning = true
                    while (true) {

//                        wifiLock?.acquire()

                        ds.receive(dp)

                        val hexDataString = Common.BinaryToHexString(dp.data)

                        receiveText = "${receiveText}\n${hexDataString}"
                        parseCommand(hexDataString)

//                        wifiLock?.release()

//                        "???????????????????????? clientip : ${dp.address.hostAddress} | port: ${dp.port}".log_d()
//                        val sendStr = "Server send: hello! from ${dp.address.hostName}"

//                        sendCommand2Client("192.168.50.16", "FF 00")

//                        dp = DatagramPacket(buf, buf.size, dp.address, dp.port)
//                        ds.send(dp)
                    }
                }
        }
    }

    fun stopServer() {
        ServerSocket.getInstance().closeSocket()
    }

    fun openDoor(liftFloorId: String, isOpen: Boolean) {
        val command = generateCommand(liftFloorId, isOpen)
        command?.let {

            viewModelScope.launch(Dispatchers.Default) {
                flow {
                    emit("192.168.50.6")
                }.collect {
                    sendCommand2Client(it, command)
                }
            }
        }
    }

    private fun sendCommand2Client(clientIp: String, command: String) {
        "????????????????????????: $clientIp:$CLIENT_PORT".log_d()
        // ?????? DatagramSocket
        try {
            val ds = ServerSocket.getInstance().getSocket()
//            val ds = DatagramSocket(null)
//            ds.reuseAddress = true
//            ds.bind(InetSocketAddress(InetAddress.getLocalHost(), 9999))
            // ?????????????????????
            val buf = Common.hexStringToByteArray(command)

//            val buf = byteArrayOf(
//                0xFF.toByte(),
//                0x27,
//                0x01,
//                0x00,
//                0x01,
//                0x00,
//                0x00,
//                0x00,
//                0x00,
//                0x00,
//                0xD8.toByte(),
//                0xFE.toByte()
//            )

            "????????????: [${Common.BinaryToHexString(buf)}]".log_d()

            // ??????????????????
            val dp = DatagramPacket(buf, buf.size, InetAddress.getByName(clientIp), CLIENT_PORT)
            // ????????????
            ds.send(dp)
        } catch (e: Exception) {
            e.printStackTrace()
            e.message?.log_d()
        }
    }

    private fun generateCommand(liftFloorId: String, isOpen: Boolean): String? {
        // ??????
        val head = "FF"
        // ?????????
        val function_code = "27"
        // ?????????????????????
        // ????????????
        val additional_function = "00"
        // ??????/????????????
        val open_close_door = if (isOpen) "01" else "02"
        // ??????5??????????????????
        val additional_function_code = "00 00 00 00 00"
        // ???????????? ??????10?????????????????????????????????

        // ??????????????? string ????????????
        val check_sum_source = head + " " + function_code + " " +
                liftFloorId + " " + additional_function + " " +
                open_close_door + " " + additional_function_code
        "?????????????????????10?????????: $check_sum_source".log_d()

        // ???????????????
        var check_sum: String = HexBCCCheck(check_sum_source)
        if (check_sum.equals("FF", ignoreCase = true) || check_sum.equals(
                "FE",
                ignoreCase = true
            )
        ) {
            check_sum = "00"
        }
        // ??????
        val tail = "FE"

        // ??????????????? string ???????????????
        val command = head + " " + function_code + " " +
                liftFloorId + " " + additional_function + " " +
                open_close_door + " " + additional_function_code + " " +
                check_sum + " " + tail
        "?????????: $check_sum".log_d()
        "command: $command".log_d()
        return command
    }

    // 16??????????????????
    private fun HexBCCCheck(source: String): String {
        val source_array = source.split(" ").toTypedArray()
        var sum = 0
        for (s in source_array) {
            sum = sum xor s.toInt(16)
        }
        return Integer.toHexString(sum).uppercase(Locale.getDefault())
    }

    /**
     * ?????????????????????
     * 0x29 ??????????????????????????????????????????????????????????????????????????????????????? 0x29 ?????????????????????????????????
     * @param hexData
     */
    private fun parseCommand(hexData: String) {
        // ????????????
        "Server receive: [$hexData]".log_d()
        // ?????? " " ???????????????
        val strs = hexData.split(" ").toTypedArray()
        // ???????????????
        val function_code = strs[1]
        when (function_code) {
            "28" -> {}
            "29" -> {}
        }

        // ????????????ID
        val device_id = strs[2]

        // ???????????????
        val control_code = strs[4]
        if (function_code.equals("28", ignoreCase = true)) {
            // ??????????????????
            if (control_code.equals("01", ignoreCase = true)) {
                // ????????????
                "??????[$device_id]????????????????????????-${getCurrentTime()}".log_d()
            } else if (control_code.equals("02", ignoreCase = true)) {
                // ??????????????????
                "??????[$device_id]??????????????????????????????-${getCurrentTime()}".log_d()
            }
        } else if (function_code.equals("29", ignoreCase = true)) {
            // ?????????????????????
            if (control_code.equals("01", ignoreCase = true)) {
                // ??????????????????
                "??????[$device_id]????????????????????????-${getCurrentTime()}".log_d()
            }
        }
    }


    // ?????????????????? ??????????????? yyyy-MM-dd HH:mm:ss
    private fun getCurrentTime(): String? {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        return sdf.format(Date())
    }

    fun send2Server(host: String, port: Int, message: String) {
        viewModelScope.launch(Dispatchers.Default) {
            flow<Unit> {
                emit(Unit)
            }.collect {
//                try {
//                    // data to send
////            val data = message.toByteArray()
//                    val data = byteArrayOf(
//                        0xFF.toByte(),
//                        0x27,
//                        0x01,
//                        0x00,
//                        0x01,
//                        0x00,
//                        0x00,
//                        0x00,
//                        0x00,
//                        0x00,
//                        0xD8.toByte(),
//                        0xFE.toByte()
//                    )
//                    // DatagramSocket to send the message
//                    val socket = DatagramSocket(port)
//                    // InetAddress to send the message
//                    val address = InetAddress.getByName(host)
//                    // DatagramPacket to send the message
//                    val packet = DatagramPacket(data, data.size, address, port)
//                    // send the message
//                    socket.send(packet)
//
//                    "UDP message sent to $host:$port".log_d()
//
//                    // receive the message from the server
////                    val receiveData = ByteArray(1024)
////                    val receivePacket = DatagramPacket(receiveData, receiveData.size)
////                    socket.receive(receivePacket)
//                    // print the message
////                    "Received from server: " + String(receivePacket.data).log_d()
//
//                    // ??????socket
////                    socket.close()
//                } catch (e: UnknownHostException) {
//                    e.printStackTrace()
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                }
            }
        }

    }


}