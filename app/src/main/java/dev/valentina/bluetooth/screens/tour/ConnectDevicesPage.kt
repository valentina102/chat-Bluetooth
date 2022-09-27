import android.annotation.SuppressLint
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Snackbar
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.lifecycleScope
import dev.valentina.bluetooth.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.util.*

@SuppressLint("MissingPermission")
@Composable
fun ConnectDevicesPage(
    activity: ComponentActivity,
    basicDevicesList:List<BluetoothDevice>,
    bluetoothAdapter: BluetoothAdapter,
    requestBeDiscoverable:ActivityResultLauncher<Intent>,
    onStart:()->Unit,
    onSuccessfulConnection: (BluetoothSocket)->Unit
) {

    val showDialog = remember {
        mutableStateOf(false)
    }

    var error by remember{
        mutableStateOf(false)
    }

    var progress by remember{
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier.fillMaxHeight()
    ) {
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            Image(
                painter = painterResource(id = R.drawable.phones),
                contentDescription = "Connect phones"
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                "Empezar", style = TextStyle(
                    fontWeight = FontWeight.Bold,
                    fontSize = 35.sp
                )
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                "Primero busca un dispositivo para conectarte", style = TextStyle(
                    fontSize = 20.sp, textAlign = TextAlign.Center
                )
            )
        }
        Button(onClick = {
            progress = true
            activity.lifecycleScope.launch{
                withContext(Dispatchers.Main) {
                    try {
                        ServerConnection.initServer(requestBeDiscoverable)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                        progress = false
                        error = true
                    } finally {
                        progress = false
                        onStart()
                        showDialog.value = true
                    }
                }

            }
        }, modifier = Modifier.align(alignment = Alignment.CenterHorizontally)) {
            Text(
                "Iniciar", style = TextStyle(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            )
        }

        if(progress){
            Dialog(onDismissRequest = {}) {
                CircularProgressIndicator()
            }
        }


        BluetoothDevicesList(
            devicesList = basicDevicesList,
            showDialog = showDialog.value,
            onClickDevice = {
                activity.lifecycleScope.launch(Dispatchers.IO) {
                    try{
                        val mmSocket: BluetoothSocket? by lazy(LazyThreadSafetyMode.NONE) {
                            it.createRfcommSocketToServiceRecord(
                                UUID.fromString(
                                    "00001101-0000-1000-8000-00805F9B34FB"
                                )
                            )
                        }
                        bluetoothAdapter.cancelDiscovery()
                        mmSocket?.connect()
                        mmSocket?.let {
                            onSuccessfulConnection(it)
                        }
                    }
                    catch(e:IOException){
                        e.printStackTrace()
                        error = true
                    }
                    finally {
                        showDialog.value = false;
                    }
                }
            }) {

        }

        if (error) {
            Snackbar(
                modifier = Modifier.padding(8.dp)
            ) { Text(text = "Ocurrio un error conectandose, intentalo de nuevo!") }
        }

    }

}