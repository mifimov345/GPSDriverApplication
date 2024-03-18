package emk.driver.gpsdriverapplication

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.content.Intent
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.google.android.gms.location.LocationServices
import emk.driver.gpsdriverapplication.ui.theme.GPSDriverApplicationTheme
import emk.driver.oversimplification.databinding.ActivityMainBinding
import emk.driver.oversimplification.databinding.UserActivityMainBinding
import emk.driver.gpsdriverapplication.services.LocationService
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import android.os.Looper


class UserActivity : AppCompatActivity() {
    private lateinit var statusButton: Button
    private lateinit var googleMap: GoogleMap
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var mapFragment: SupportMapFragment
    private lateinit var isExceeded: TextView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = UserActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val isExceeded = binding.isExceeded
        LastLoginManager.setEnableListener { newExceedVariable ->
            runOnUiThread{
                isExceeded.text = newExceedVariable.toString()
            }
        }

        val mapFragment = binding.mapView
        mapFragment.getMapAsync{ map ->
            googleMap = map
            setupMap()
        }
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        statusButton = binding.stopStartButton
        statusButton.setOnClickListener(){
            changeStatus()
        }

        val pointsText: TextView = binding.pointsText
        pointsText.text = LastLoginManager.pointsAmount.toString()
        LastLoginManager.setOnPointsAmountChangedListener { newPointsAmount ->
            runOnUiThread {
                pointsText.text = newPointsAmount.toString()
            }
        }

        val logoutButton: Button = binding.exitButton
        logoutButton.setOnClickListener {
            logout()
        }

        val serviceIntent = Intent(this, LocationService::class.java)
        startService(serviceIntent)

    }

    private fun setupMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
        }
        googleMap.isMyLocationEnabled = true

        // Request location updates
        fusedLocationClient.requestLocationUpdates(
            LocationRequest.create().apply {
                interval = 10000
                fastestInterval = 5000
                priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            },
            locationCallback,
            Looper.getMainLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location = locationResult.lastLocation
            location?.let {
                val currentLatLng = LatLng(it.latitude, it.longitude)
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))

                // Add marker
                googleMap.clear()
                googleMap.addMarker(MarkerOptions().position(currentLatLng).title("Current Location"))
            } ?: run {
            }
        }
    }

    private fun changeStatus(){
        if (statusButton.text == "Остановить трекинг"){
            val serviceIntent = Intent(this, LocationService::class.java)
            stopService(serviceIntent)
            statusButton.text = "Начать трекинг"
        }else{
            val serviceIntent = Intent(this, LocationService::class.java)
            startService(serviceIntent)
            statusButton.text = "Остановить трекинг"
        }
    }

    private fun logout() {
        val userIntent = Intent(this, MainActivity::class.java)
        startActivity(userIntent)
        val serviceIntent = Intent(this, LocationService::class.java)
        stopService(serviceIntent)
        LastLoginManager.lastLogin = ""
        finish()
    }
}
