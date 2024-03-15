package emk.driver.gpsdriverapplication.services

import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import emk.driver.gpsdriverapplication.LastLoginManager


class LocationService : Service() {

    private fun getAccelerometerData(callback: (String) -> Unit) {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val accelerometerValues = FloatArray(3)

        val accelerometerListener = object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    accelerometerValues[0] = event.values[0]
                    accelerometerValues[1] = event.values[1]
                    accelerometerValues[2] = event.values[2]
                    sensorManager.unregisterListener(this)
                    callback("X: ${accelerometerValues[0]}, Y: ${accelerometerValues[1]}, Z: ${accelerometerValues[2]}")
                }
            }
        }

        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }
    private var locationManager: LocationManager? = null

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            // Handle location updates
            val latitude = location.latitude
            val longitude = location.longitude
            val speed = location.speed

            // Get accelerometer parameters
            getAccelerometerData { accelerometerData ->
                // Use latitude, longitude, orientation, and accelerometer data as needed
                Log.d("LocationService", "Location Updated: $latitude, $longitude, $speed, Accelerometer: $accelerometerData")

                // Save location to file
                saveLocationToFile("$latitude, $longitude", "$speed", "$accelerometerData")
            }
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }

    private fun getAccelerometerData(): String {
        val sensorManager = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        val accelerometerValues = FloatArray(3)

        sensorManager.registerListener(object : SensorEventListener {
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

            override fun onSensorChanged(event: SensorEvent?) {
                if (event != null) {
                    accelerometerValues[0] = event.values[0]
                    accelerometerValues[1] = event.values[1]
                    accelerometerValues[2] = event.values[2]
                }
            }
        }, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)

        return "X: ${accelerometerValues[0]}, Y: ${accelerometerValues[1]}, Z: ${accelerometerValues[2]}"
    }





    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
    }

    private fun startLocationUpdates() {
        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager?

        if (locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true) {
            try {
                // время сбора инфы
                locationManager?.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    1000,
                    0.toFloat(),
                    locationListener
                )
            } catch (e: SecurityException) {
                Log.e("LocationService", "Error requesting location updates", e)
            }
        }
    }

    private fun stopLocationUpdates() {
        locationManager?.removeUpdates(locationListener)
    }

    private fun saveLocationToFile(location: String, speed: String, accelerometerData: String) {
        val fileName = LastLoginManager.lastLogin + ".csv"
        try {
            val file = File(filesDir, fileName)

            // проверка на наличие файла
            if (!file.exists()) {
                file.createNewFile()
                // хедер файла
                FileWriter(file, true).use { writer ->
                    writer.append(fileName)
                }
            }

            val writer = FileWriter(file, true)
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            writer.append("$timestamp, $location, $speed, $accelerometerData\n")
            writer.close()
        } catch (e: Exception) {
            Log.e("LocationService", "Error saving location to file", e)
        }
    }


}
