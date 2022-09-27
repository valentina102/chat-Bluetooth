package dev.valentina.bluetooth.screens

import android.bluetooth.BluetoothSocket
import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.valentina.bluetooth.model.Message
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

@Composable
fun ChatPage(bluetoothSocket: BluetoothSocket) {
    val mmInStream: InputStream = bluetoothSocket.inputStream
    val mmOutStream: OutputStream = bluetoothSocket.outputStream
    val messages = remember {
        mutableStateOf(listOf<Message>())
    }
    val coroutineScope = rememberCoroutineScope()
    var error by remember {
        mutableStateOf(false)
    }

    var textValue by remember {
        mutableStateOf("")
    }

    var state = rememberLazyListState()

    LaunchedEffect(null) {
        var numBytes: Int // bytes returned from read()

        while (true) {
            val mmBuffer: ByteArray by lazy {
                ByteArray(1024)
            }
            numBytes = try {
                withContext(Dispatchers.IO) {
                    mmInStream.read(mmBuffer)
                }
                Log.d("Bluetuch", "Mensaje recibido! ${String(mmBuffer)}")
            } catch (e: IOException) {
                Log.d("Bluetuch", "Input stream was disconnected", e)
                break
            }

            Log.d("Bluetuch", "Mensaje enviado!")
            messages.value =
                messages.value + Message(messages.value.size + 1, String(mmBuffer), "Me")

    }
}



Column {
    MessageList(messages.value, modifier = Modifier.weight(1f))
    Surface(modifier = Modifier.padding(bottom = 10.dp)) {
        Row {
            TextField(value = textValue, onValueChange = {
                textValue = it
            })
            IconButton(onClick = {
                if (textValue.isNotEmpty()) {
                    coroutineScope.launch {
                        try {
                            withContext(Dispatchers.IO) {
                                mmOutStream.write(textValue.toByteArray())
                            }
                            Log.d("Bluetuch", "Mensaje enviado! $textValue")
                            messages.value = messages.value + Message(
                                messages.value.size + 1,
                                textValue,
                                "Pal"
                            )
                            textValue = ""
                        } catch (e: java.lang.Exception) {
                            Log.e("Bluetuch", "Error occurred when sending data", e)
                            e.printStackTrace()
                            error = true
                        }

                    }
                }
            }) {
                Icon(Icons.Default.Send, contentDescription = "Send")
            }
        }
    }
}
}

@Composable
fun MessageList(messages: List<Message>, modifier: Modifier) {
    LazyColumn(
        state = rememberLazyListState(),
        modifier = modifier,
        verticalArrangement = Arrangement.Bottom
    ) {
        items(messages.size, key = { i ->
            messages[i].id
        }) { index ->
            Box(modifier = Modifier.fillMaxWidth()) {
                Card(
                    backgroundColor = if (messages[index].receiver == "Me") MaterialTheme.colors.background else MaterialTheme.colors.primary,
                    modifier = Modifier
                        .padding(10.dp)
                        .align(
                            if (messages[index].receiver == "Me")
                                Alignment.BottomStart else Alignment.BottomEnd
                        )
                ) {
                    Text(
                        messages[index].message,
                        modifier = Modifier.padding(10.dp),
                        style = TextStyle(
                            color = if (messages[index].receiver == "Me") Color.Black else Color.White,
                            fontSize = 18.sp
                        )
                    )
                }
            }
        }
    }
}
