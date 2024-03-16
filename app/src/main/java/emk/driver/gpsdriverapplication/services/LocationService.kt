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
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.Query
import android.os.Handler
import java.net.URLEncoder




class LocationService : Service() {


    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5000/score/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val scoreService = retrofit.create(ScoreService::class.java)

    data class ScoreRequest(val login: String)
    data class ScoreResponse(val score: Int)

    interface ScoreService {
        @POST("/score")
        fun getScore(@Body request: ScoreRequest): Call<ScoreResponse>
    }


    val changeToPoints = 1000

    private fun fetchPointsAmount() {
        val login = LastLoginManager.lastLogin
        val request = ScoreRequest(login)
        scoreService.getScore(request).enqueue(object : Callback<ScoreResponse> {
            override fun onResponse(call: Call<ScoreResponse>, response: Response<ScoreResponse>) {
                if (response.isSuccessful) {
                    val score = response.body()?.score ?: 0
                    LastLoginManager.pointsAmount = score
                    LastLoginManager.updatePointsAmount(score)
                    Log.d("LocationService", "Points amount updated: $score")
                } else {
                    Log.e("LocationService", "Failed to fetch points amount: ${response.code()}")
                }
            }

            override fun onFailure(call: Call<ScoreResponse>, t: Throwable) {
                Log.e("LocationService", "Error fetching points amount: ${t.message}", t)
            }
        })
    }







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
        startScoreUpdates()
    }

    private fun startScoreUpdates(){
        fetchPointsAmount()

        var handler = Handler()
        handler.postDelayed(object : Runnable {
            override fun run() {
                fetchPointsAmount()
                handler.postDelayed(this, 5 * 60 * 1000) // Schedule next execution after 5 minutes
            }
        }, 0) // Execute immediately
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        stopScoreUpdates()
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

    private fun stopScoreUpdates(){

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
