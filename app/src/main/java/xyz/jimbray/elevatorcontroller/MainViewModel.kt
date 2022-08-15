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
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import java.net.DatagramPacket
import java.net.InetAddress
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

                    val buf = ByteArray(12)

                    val dp = DatagramPacket(buf, buf.size)

                    serverRunning = true
                    while (true) {

//                        wifiLock?.acquire()

                        ds.receive(dp)

                        val hexDataString = Common.BinaryToHexString(dp.data)

                        receiveText = "${receiveText}\n${hexDataString}"
                        parseCommand(hexDataString)

//                        wifiLock?.release()
                    }
                }
        }
    }

    fun stopServer() {
        ServerSocket.getInstance().closeSocket()
    }

    fun openDoor(liftFloorId: String, isOpen: Boolean) {
        val command = generateCommand(liftFloorId, isOpen)
        command?.let { sendCommand2Client("192.168.50.7", it) }
    }

    private fun sendCommand2Client(clientIp: String, command: String) {
        "发送指令到客户端: $clientIp:$CLIENT_PORT".log_d()
        // 创建 DatagramSocket
        try {
            val ds = ServerSocket.getInstance().getSocket()
            // 创建发送缓冲区
            val buf = Common.hexStringToByteArray(command)

//            val buf1 = byteArrayOf(
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

            "发送数据: [${Common.BinaryToHexString(buf)}]".log_d()

            // 创建发送对象
            val dp = DatagramPacket(buf, buf.size, InetAddress.getByName(clientIp), CLIENT_PORT)
            // 发送数据
            ds.send(dp)
        } catch (e: Exception) {
            e.printStackTrace()
            e.message?.log_d()
        }
    }

    private fun generateCommand(liftFloorId: String, isOpen: Boolean): String? {
        // 帧头
        val head = "FF"
        // 功能码
        val function_code = "27"
        // 电梯控制板编号
        // 附加功能
        val additional_function = "00"
        // 开门/解除开门
        val open_close_door = if (isOpen) "01" else "02"
        // 剩余5个附加功能码
        val additional_function_code = "00 00 00 00 00"
        // 校验码： 前面10个字节的异或校验的结果

        // 拼接前面的 string 以，隔开
        val check_sum_source = head + " " + function_code + " " +
                liftFloorId + " " + additional_function + " " +
                open_close_door + " " + additional_function_code
        "生成校验码的前10个字节: $check_sum_source".log_d()

        // 计算校验码
        var check_sum: String = HexBCCCheck(check_sum_source)
        if (check_sum.equals("FF", ignoreCase = true) || check_sum.equals(
                "FE",
                ignoreCase = true
            )
        ) {
            check_sum = "00"
        }
        // 帧尾
        val tail = "FE"

        // 拼接所有的 string 以空格隔开
        val command = head + " " + function_code + " " +
                liftFloorId + " " + additional_function + " " +
                open_close_door + " " + additional_function_code + " " +
                check_sum + " " + tail
        "校验码: $check_sum".log_d()
        "command: $command".log_d()
        return command
    }

    // 16进制异或校验
    private fun HexBCCCheck(source: String): String {
        val source_array = source.split(" ").toTypedArray()
        var sum = 0
        for (s in source_array) {
            sum = sum xor s.toInt(16)
        }
        return Integer.toHexString(sum).uppercase(Locale.getDefault())
    }

    /**
     * 解析收到的消息
     * 0x29 在电磁阀正在执行时才会进行发送操作，电磁阀不动时，不会发送 0x29 消息（光电传感器消息）
     * @param hexData
     */
    private fun parseCommand(hexData: String) {
        // 显示数据
        "Server receive: [$hexData]".log_d()
        // 通过 " " 分割字符串
        val strs = hexData.split(" ").toTypedArray()
        // 获取功能码
        val function_code = strs[1]
        when (function_code) {
            "28" -> {}
            "29" -> {}
        }

        // 获取设备ID
        val device_id = strs[2]

        // 获取控制码
        val control_code = strs[4]
        if (function_code.equals("28", ignoreCase = true)) {
            // 电梯控制指令
            if (control_code.equals("01", ignoreCase = true)) {
                // 开门指令
                "电梯[$device_id]开门指令执行成功-${getCurrentTime()}".log_d()
            } else if (control_code.equals("02", ignoreCase = true)) {
                // 解除开门指令
                "电梯[$device_id]解除开门指令执行成功-${getCurrentTime()}".log_d()
            }
        } else if (function_code.equals("29", ignoreCase = true)) {
            // 光电传感器指令
            if (control_code.equals("01", ignoreCase = true)) {
                // 开门到位指令
                "电梯[$device_id]收到开门到位指令-${getCurrentTime()}".log_d()
            }
        }
    }


    // 获取当前时间 并格式化为 yyyy-MM-dd HH:mm:ss
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
//                    // 关闭socket
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