package xyz.jimbray.elevatorcontroller

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import xyz.jimbray.elevatorcontroller.ui.theme.ElevatorControllerTheme

class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            ElevatorControllerTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colors.background
                ) {
                    Home()
                }
            }
        }
    }

//    override fun onResume() {
//        super.onResume()
//
//        mainViewModel.initWifiLock()
//    }
}

@Composable
fun Home() {
    val mainViewModel: MainViewModel = viewModel()
    Column {
        TopBar(title = "${stringResource(id = R.string.app_name)}${if (mainViewModel.serverRunning) "（服务运行中）" else ""}")
        OperationArea()
    }
}

@Composable
fun TopBar(title: String) {

    val mainViewModel: MainViewModel = viewModel()
    Row(
        verticalAlignment = Alignment.CenterVertically, modifier = Modifier
            .height(48.dp)
            .fillMaxWidth()
            .padding(start = 8.dp)
    ) {
        Text(
            text = title, fontSize = 16.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier
            .fillMaxWidth()
            .weight(1f))
        Text(text = if (mainViewModel.serverRunning) "停止服务" else "启动服务",
        modifier = Modifier.clickable {
            if (mainViewModel.serverRunning) {
                // stop
                mainViewModel.stopServer()
            } else {
                // start server
                mainViewModel.startUdpServer()
            }
        })

    }
}

@Composable
fun OperationArea() {
    val mainViewModel: MainViewModel = viewModel()

    Column(verticalArrangement = Arrangement.SpaceEvenly,
        modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(modifier = Modifier
            .fillMaxHeight()
            .weight(1f)
            .padding(16.dp),
            value = mainViewModel.receiveText,
            onValueChange = {
                mainViewModel.receiveText = it
            },
            label = {
                Text(text = "UDP服务端")
            })

        Button(onClick = {
            mainViewModel.send2Server("192.168.50.16", 9999, "message is coming.")
        }) {
            Text(text = "↑发送↑")
        }


        Column(
            modifier = Modifier
                .fillMaxHeight()
                .weight(1f)) {
            Button(onClick = {
                mainViewModel.openDoor("01", true)
            }) {
                Text(text = "开门指令")
            }

            Button(onClick = {
                mainViewModel.openDoor("01", false)
            }) {
                Text(text = "关门指令")
            }
        }

    }

}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    ElevatorControllerTheme {
        Home()
    }
}