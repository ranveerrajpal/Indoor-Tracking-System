package com.example.bluetoothbecon1

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.BluetoothLeAdvertiser
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeAdvertiser: BluetoothLeAdvertiser
    private lateinit var advertisingSettings: AdvertiseSettings
    private lateinit var advertisingData: AdvertiseData

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        checkPermissions() // Check and request permissions

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        if (bluetoothAdapter.isMultipleAdvertisementSupported) {
            startAdvertising()
        } else {
            Toast.makeText(this, "BLE advertising not supported", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, missingPermissions.toTypedArray(), 1)
        }
    }

    private fun startAdvertising() {
        bluetoothLeAdvertiser = bluetoothAdapter.bluetoothLeAdvertiser

        // Create the advertising settings
        advertisingSettings = AdvertiseSettings.Builder()
            .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
            .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
            .setConnectable(false)
            .setTimeout(0) // Set to 0 for unlimited advertising duration
            .build()

        // Create the advertising data
        val uuid = UUID.randomUUID() // Generate a random UUID
        val manufacturerId = 0xFFFF // Use your manufacturer ID
        val manufacturerData = ByteArray(2)
        manufacturerData[0] = (manufacturerId and 0xFF).toByte()
        manufacturerData[1] = ((manufacturerId shr 8) and 0xFF).toByte()

        advertisingData = AdvertiseData.Builder()
            .setIncludeDeviceName(true)
            .addManufacturerData(manufacturerId, manufacturerData)
            .build()

        // Start advertising
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeAdvertiser.startAdvertising(advertisingSettings, advertisingData, object : AdvertiseCallback() {
                override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
                    super.onStartSuccess(settingsInEffect)
                    Toast.makeText(this@MainActivity, "Advertising Started", Toast.LENGTH_SHORT).show()
                }

                override fun onStartFailure(errorCode: Int) {
                    super.onStartFailure(errorCode)
                    val errorMessage = when (errorCode) {
                        AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED -> "Advertising already started"
                        AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE -> "Advertising data too large"
                        AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS -> "Too many advertisers"
                        AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR -> "Internal error"
                        AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED -> "Feature unsupported"
                        else -> "Unknown error"
                    }
                    Toast.makeText(this@MainActivity, "Advertising Failed: $errorMessage", Toast.LENGTH_SHORT).show()
                }
            })
        } else {
            Toast.makeText(this, "Bluetooth advertising permission is not granted.", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop advertising when the activity is destroyed
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADVERTISE) == PackageManager.PERMISSION_GRANTED) {
            bluetoothLeAdvertiser.stopAdvertising(object : AdvertiseCallback() {})
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted
                startAdvertising() // Start advertising after permissions are granted
            } else {
                Toast.makeText(this, "Permissions denied. Advertising will not work.", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
