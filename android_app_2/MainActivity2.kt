package com.example.beconlocator

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.DialogInterface
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class MainActivity : AppCompatActivity() {

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationTextView: TextView
    private lateinit var deviceListLayout: LinearLayout
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private val handler = Handler(Looper.getMainLooper())
    private var devicesMap = mutableMapOf<String, BluetoothDevice>()

    private lateinit var scanButton: Button
    private lateinit var userInfoTextView: TextView
    private var uniqueID: String = ""

    companion object {
        private const val REQUEST_LOCATION_PERMISSION = 101
        private const val REQUEST_BLUETOOTH_PERMISSION = 102
        private const val SERVER_URL = "https://realtime2.onrender.com/submit-data"
    }

    private val httpClient = OkHttpClient()

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            for (location in locationResult.locations) {
                Log.d("MainActivity", "Location changed: Latitude: ${location.latitude}, Longitude: ${location.longitude}")
                locationTextView.text = "Location: (${location.latitude}, ${location.longitude})"
                sendLocationToServer(location.latitude, location.longitude)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationTextView = findViewById(R.id.locationTextView)
        deviceListLayout = findViewById(R.id.deviceListLayout)
        scanButton = findViewById(R.id.scanButton)
        userInfoTextView = findViewById(R.id.userInfoTextView)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner

        val sharedPreferences = getSharedPreferences("BeaconLocatorPrefs", Context.MODE_PRIVATE)
        val userName = sharedPreferences.getString("userName", null)
        uniqueID = sharedPreferences.getString("uniqueID", UUID.randomUUID().toString()) ?: UUID.randomUUID().toString()

        if (userName == null) {
            showNameInputDialog()
        } else {
            userInfoTextView.text = "Name: $userName, ID: $uniqueID"
        }

        checkPermissionsAndStartLocationUpdates()
        startBluetoothScan()
    }

    private fun showNameInputDialog() {
        val input = EditText(this).apply {
            hint = "Enter your name"
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle("Enter Your Name")
            .setView(input)
            .setPositiveButton("OK") { _: DialogInterface, _: Int ->
                val userName = input.text.toString()
                val sharedPreferences = getSharedPreferences("BeaconLocatorPrefs", Context.MODE_PRIVATE)
                with(sharedPreferences.edit()) {
                    putString("userName", userName)
                    putString("uniqueID", uniqueID)
                    apply()
                }
                userInfoTextView.text = "Name: $userName, ID: $uniqueID"
            }
            .setNegativeButton("Cancel") { dialog: DialogInterface, _: Int -> dialog.cancel() }
            .create()
        dialog.show()
    }

    private fun checkPermissionsAndStartLocationUpdates() {
        val permissions = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT
        )
 
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), REQUEST_LOCATION_PERMISSION)
        } else {
            startLocationUpdates()
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 1000L) // Update every 1 second
                .setMinUpdateIntervalMillis(1000L) // Minimum interval is also 1 second
                .build()

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper())
        } else {
            Log.e("MainActivity", "Location permission not granted.")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBluetoothScan() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED) {
            try {
                devicesMap.clear()
                deviceListLayout.removeAllViews()
                bluetoothLeScanner.startScan(scanCallback)

                handler.postDelayed({
                    bluetoothLeScanner.stopScan(scanCallback)
                    updateDeviceList()
                    startBluetoothScan() // Restart the scan
                }, 5000)
            } catch (e: SecurityException) {
                Log.e("MainActivity", "SecurityException when starting Bluetooth scan: ${e.message}")
            }
        } else {
            ActivityCompat.requestPermissions(this, arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            ), REQUEST_BLUETOOTH_PERMISSION)
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                val device = result.device
                if (device.name != null && !devicesMap.containsKey(device.address)) {
                    devicesMap[device.address] = device
                }
            } else {
                Log.w("ScanCallback", "Bluetooth connect permission not granted.")
            }
        }
    }

    private fun updateDeviceList() {
        deviceListLayout.removeAllViews()

        if (devicesMap.isEmpty()) {
            val noDevicesTextView = TextView(this).apply {
                text = "No Bluetooth devices detected."
                textSize = 16f
            }
            deviceListLayout.addView(noDevicesTextView)
        } else {
            for ((_, device) in devicesMap) {
                val deviceInfo = TextView(this).apply {
                    if (ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED) {
                        text = "${device.name ?: "Unknown Device"} (${device.address})"
                    } else {
                        text = "Permission required to access Bluetooth devices."
                    }
                    textSize = 16f
                    setPadding(10, 10, 10, 10)
                }
                deviceListLayout.addView(deviceInfo)
            }
        }
    }

    private var lastLatitude: Double? = null
    private var lastLongitude: Double? = null

    private fun sendLocationToServer(latitude: Double, longitude: Double) {
        if (lastLatitude == latitude && lastLongitude == longitude) {
            return
        }

        lastLatitude = latitude
        lastLongitude = longitude

        val floor = determineFloor()
        val userName = getSharedPreferences("BeaconLocatorPrefs", Context.MODE_PRIVATE).getString("userName", "") ?: ""
        val uniqueID = getSharedPreferences("BeaconLocatorPrefs", Context.MODE_PRIVATE).getString("uniqueID", "") ?: ""

        val json = """
            {
                "name": "$userName",
                "uniqueID": "$uniqueID",
                "floor": $floor,
                "latitude": $latitude,
                "longitude": $longitude
            }
        """.trimIndent()

        CoroutineScope(Dispatchers.IO).launch {
            val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())
            val request = Request.Builder()
                .url(SERVER_URL)
                .post(requestBody)
                .build()

            try {
                val response = httpClient.newCall(request).execute()
                Log.d("MainActivity", "Response: ${response.body?.string()}")
            } catch (e: Exception) {
                Log.e("MainActivity", "Failed to send location: ${e.message}")
            }
        }
    }

    private fun determineFloor(): Int {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("MainActivity", "Bluetooth connect permission not granted.")
            return -1
        }

        for ((_, device) in devicesMap) {
            if (device.name != null) {
                val floorNumber = parseFloorNumberFromDeviceName(device.name)
                if (floorNumber != null) {
                    return floorNumber
                }
            }
        }
        return -1
    }

    private fun parseFloorNumberFromDeviceName(deviceName: String): Int? {
        return if (deviceName.contains("Floor")) {
            deviceName.filter { it.isDigit() }.toIntOrNull()
        } else {
            null
        }
    }
}
