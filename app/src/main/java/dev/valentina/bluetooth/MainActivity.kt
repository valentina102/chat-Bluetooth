package dev.valentina.bluetooth

import dev.valentina.bluetooth.screens.ChatPage
import ConnectDevicesPage
import android.annotation.SuppressLint
import android.bluetooth.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import dev.valentina.bluetooth.conection.PermissionsHelper
import dev.valentina.bluetooth.ui.theme.BluetoothTheme
import com.google.accompanist.pager.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.*


class MainActivity : ComponentActivity() {

    lateinit var bluetoothManager: BluetoothManager
    lateinit var bluetoothAdapter: BluetoothAdapter
    var mIsReceiverRegistered = false

    private val basicDevicesList: MutableState<List<BluetoothDevice>> = mutableStateOf(emptyList())
    private val bluetoothSocket: MutableState<BluetoothSocket?> = mutableStateOf(null)

    @SuppressLint("MissingPermission")
    val enableLocation: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val successfully = bluetoothAdapter.startDiscovery()
        if (successfully) Log.d("Bluetooth", "Bluetooth Discovery Started")
    }

    @SuppressLint("MissingPermission")
    val requestEnableBluetooth: ActivityResultLauncher<Intent> = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        val locationManager =
            getSystemService(ComponentActivity.LOCATION_SERVICE) as LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
        if (!isGpsEnabled) {
            enableLocation.launch(Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS))
        } else {
            val successfully = bluetoothAdapter.startDiscovery()
            if (successfully) Log.d("Bluetooth", "Bluetooth Discovery Started")
        }
    }

    private val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            var isGranted = true
            permissions.toList().forEach {
                if (!it.second) {
                    isGranted = false
                }
            }
            if (isGranted) {
                if (bluetoothAdapter.isEnabled) {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    requestEnableBluetooth.launch(enableBtIntent)
                }
            }
        }

    @SuppressLint("MissingPermission")
    val requestBeDiscoverable: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            lifecycleScope.launch(Dispatchers.IO){
                if (it.resultCode != ComponentActivity.RESULT_CANCELED) {
                    val mmServerSocket: BluetoothServerSocket? by lazy(LazyThreadSafetyMode.NONE) {
                        bluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(
                            "Bluetuch",
                            UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
                        )
                    }
                    try {
                        var shouldLoop = true
                        while (shouldLoop) {
                            bluetoothSocket.value = try {
                                mmServerSocket?.accept()
                            } catch (e: IOException) {
                                Log.e("Bluetuch", "Socket's accept() method failed", e)
                                shouldLoop = false
                                null
                            }
                            bluetoothSocket.value?.also {
                                shouldLoop = false
                            }
                        }
                    } catch (e: java.lang.Exception) {
                        mmServerSocket?.close()
                    } finally {
                        mmServerSocket?.close()
                    }
                }
            }
        }

    override fun onResume() {
        super.onResume()
        if (!mIsReceiverRegistered) {
            registerReceiver(receiver, IntentFilter(BluetoothDevice.ACTION_FOUND))
            mIsReceiverRegistered = true
        }
    }

    override fun onPause() {
        super.onPause()
        if (mIsReceiverRegistered) {
            unregisterReceiver(receiver)
            mIsReceiverRegistered = false
        }
    }


    private fun addBluetoothDevice(bluetoothDevice: BluetoothDevice) {
        if (basicDevicesList.value.none {
                it.address == bluetoothDevice.address
            }) {
            basicDevicesList.value += bluetoothDevice
        };
    }

    private val receiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {

            val action: String = intent.action ?: ""
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice =
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)!!
                    addBluetoothDevice(device);
                }
            }

        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        bluetoothManager = getSystemService(BluetoothManager::class.java)
        bluetoothAdapter = bluetoothManager.adapter

        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        registerReceiver(receiver, filter)

        setContent {
            BluetoothTheme {
                Scaffold(topBar = {
                    TopAppBar(title = {
                        Text("Bluetooth")
                    })
                }) {
                    Content(modifier = Modifier.padding(paddingValues = it));
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(receiver)
    }


    @OptIn(ExperimentalPagerApi::class)
    @SuppressLint("MissingPermission")
    @Composable
    fun Content(modifier: Modifier) {
        val pagerState = rememberPagerState()
        val coroutineScope = rememberCoroutineScope()
        val devicesList: List<BluetoothDevice> by basicDevicesList

        LaunchedEffect(bluetoothSocket.value){
            if(bluetoothSocket.value!=null){
                pagerState.animateScrollToPage(1)
            }
        }

        Column(modifier = modifier) {
            HorizontalPager(count = 2, state = pagerState, userScrollEnabled = false) { page ->
                when (page) {
                    0 -> ConnectDevicesPage(
                        this@MainActivity,
                        devicesList,
                        bluetoothAdapter,
                        onStart = {
                            PermissionsHelper.requestRequiredPermissions(
                                requestMultiplePermissions,
                                requestEnableBluetooth
                            )
                        },
                        requestBeDiscoverable = requestBeDiscoverable,
                        onSuccessfulConnection = {
                            bluetoothSocket.value = it
                        }
                    )
                    1 ->{
                        if(bluetoothSocket.value!=null){
                            ChatPage(bluetoothSocket.value!!)
                        }
                    }
                }
            }
        }
    }

}

