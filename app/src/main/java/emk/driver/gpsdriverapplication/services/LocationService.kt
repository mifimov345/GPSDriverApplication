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
import android.Manifest
import android.content.pm.PackageManager
import kotlin.collections.HashMap
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.google.gson.reflect.TypeToken
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.CompletableFuture

data class OSMResponse(
    @SerializedName("elements") val elements: List<Element>
)

data class Element(
    @SerializedName("tags") val tags: Tags?
)

data class Tags(
    @SerializedName("maxspeed") val maxspeed: String?
)

class LocationService : Service() {


    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5000/score/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val scoreService = retrofit.create(ScoreService::class.java)

    data class ScoreRequest(val login: String)
    data class ScoreResponse(val score: Int)

    private val retrofit2 = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5000/editscore/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val scoreChangeService = retrofit.create(ChangeScoreService::class.java)

    data class ChangeScoreRequest(val login: String, val score: Int)
    data class ChangeScoreResponse(val score: String)

    interface ScoreService {
        @POST("/score")
        fun getScore(@Body request: ScoreRequest): Call<ScoreResponse>
    }

    interface ChangeScoreService {
        @POST("/editscore")
        fun getChangeScore(@Body request: ChangeScoreRequest): Call<ChangeScoreResponse>
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


    private fun parseSpeedLimitFromOSM(responseBody: String?): Int {
        if (responseBody == null) return 50

        return try {
            val gson = Gson()
            val responseType = object : TypeToken<OSMResponse>() {}.type
            val osmResponse = gson.fromJson<OSMResponse>(responseBody, responseType)

            osmResponse.elements
                .asSequence()
                .mapNotNull { it.tags?.maxspeed }
                .mapNotNull { it.toIntOrNull() }
                .firstOrNull() ?: 50 //
        } catch (e: Exception) {
            Log.e("LocationService", "Error parsing speed limit from OSM response", e)
            50
        }
    }

    private fun fetchSpeedLimitForLocation(location: Location, callback: (Int) -> Unit){
        val latitude = location.latitude
        val longitude = location.longitude
        val radius = 150

        val query = """
            [out:json];
            way(around:$radius,$latitude,$longitude)[maxspeed];
            (._;>;);
            out;
        """.trimIndent()

        val encodedQuery = URLEncoder.encode(query, "UTF-8")
        val overpassUrl = "http://overpass-api.de/api/interpreter?data=$encodedQuery"

        val client = OkHttpClient()
        val request = Request.Builder().url(overpassUrl).build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                callback(50)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body()?.string()
                    val speedLimit = parseSpeedLimitFromOSM(responseBody)
                    callback(speedLimit)
                } else {
                    callback(50)
                }
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
                    rotX = accelerometerValues[0].toInt()
                    rotY = accelerometerValues[1].toInt()
                    rotZ = accelerometerValues[2].toInt()

                    callback("X: ${accelerometerValues[0]}, Y: ${accelerometerValues[1]}, Z: ${accelerometerValues[2]}")
                }
            }
        }

        sensorManager.registerListener(accelerometerListener, accelerometer, SensorManager.SENSOR_DELAY_NORMAL)
    }
    private var locationManager: LocationManager? = null
    private var isExceeded: Boolean = false
    private var timeLimited: Int = 0
    private var timeOkay: Int = 0
    private var telephoneTime: Int = 0
    private var rotX = 0
    private var rotY = 0
    private var rotZ = 0
    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            var temporalpoints = LastLoginManager.pointsAmount
            val latitude = location.latitude
            val longitude = location.longitude
            val speed = location.speed


            fetchSpeedLimitForLocation(location) { speedLimit ->
                val isSpeeding = location.speed > (speedLimit + 20)

                if (isSpeeding) {
                    LastLoginManager.updateavaliability("Скорость превышена!")
                    Log.d("LocationService", "Exceeded speed limit: ${location.speed} km/h")
                    isExceeded = true
                    timeLimited += 1
                    timeOkay = 0
                } else {
                    LastLoginManager.updateavaliability("Скорость не превышена!")
                    Log.d("LocationService", "Within speed limit: ${location.speed} km/h")
                    isExceeded = false
                    timeLimited = 0
                    timeOkay += 1
                }
            }
            if (timeLimited > 59){
                if (temporalpoints - 65 > 0){
                    temporalpoints -= 65
                }
            }
            if (timeOkay > 43200){
                if (temporalpoints + 6 <= 1000){
                    temporalpoints += 6
                }
            }


            getAccelerometerData { accelerometerData ->
                Log.d("LocationService", "Location Updated: $latitude, $longitude, $speed, Accelerometer: $accelerometerData")

                saveLocationToFile("$latitude, $longitude", "$speed", "$accelerometerData")
            }
            if ((location.speed > 35 && 11 < rotZ && rotZ< 70 && -23 < rotY && rotY < 15)|| (location.speed > 40 && -69 < rotZ && rotZ< 24 && -47 < rotY && rotY < 43)){
                telephoneTime += 1
                if (telephoneTime > 180){
                    if (temporalpoints - 10 > 0){
                        temporalpoints -= 10
                    }
                }
            }
            else{
                telephoneTime = 0
            }

            changePointsAmount(temporalpoints)
        }

        override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

        override fun onProviderEnabled(provider: String) {}

        override fun onProviderDisabled(provider: String) {}
    }


    private fun changePointsAmount(points: Int){
        val login = LastLoginManager.lastLogin
        val changeScore = points

        val request = ChangeScoreRequest(login, changeScore)
        scoreChangeService.getChangeScore(request).enqueue(object: Callback<ChangeScoreResponse>{
            override fun onResponse(call: Call<ChangeScoreResponse>, response: Response<ChangeScoreResponse>){
                if (response.isSuccessful){
                    LastLoginManager.pointsAmount = changeScore
                    LastLoginManager.updatePointsAmount(changeScore)
                    Log.d("ChangeScoreService","Points amount updated $changeScore")
                }
                else{
                    Log.e("ChangeScoreService","Failed to update points $changeScore")
                }
            }

            override fun onFailure(call: Call<ChangeScoreResponse>, t: Throwable) {
                Log.e("ChangeScoreService", "Error changing points: ${t.message}", t)
            }
        })
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
                handler.postDelayed(this, 5 * 60 * 1000)
            }
        }, 0)
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
