import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Button
import androidx.compose.material.Card
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

@SuppressLint("MissingPermission")
@Composable
fun BluetoothDevicesList(
    devicesList: List<BluetoothDevice>,
    showDialog:Boolean,
    onClickDevice:(BluetoothDevice)->Unit,
    onDismissRequest:()->Unit
) {
    if(showDialog){
        Dialog(onDismissRequest = onDismissRequest) {
            Surface(modifier = Modifier.fillMaxHeight(fraction = 0.7f)) {
                Column {
                    Button(modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp), onClick = {

                    }) {
                        Text("Buscar dispositivos ${devicesList.size}")
                    }
                    LazyColumn {
                        items(devicesList.size) {
                            Card(modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onClickDevice(devicesList[it])
                                }) {
                                Column(modifier = Modifier.padding(start = 20.dp)) {
                                    val item = devicesList[it]
                                    Text(
                                        item.name ?: "Device ID $it", style = TextStyle(
                                            fontSize = 20.sp
                                        )
                                    )
                                    Text(item.address)
                                }
                            }
                        }

                    }
                }

            }
        }
    }
}