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
import org.osmdroid. config. Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid. views.MapView
import org.osmdroid. tileprovider.tilesource. TileSourceFactory
import org.osmdroid. views.overlay.Marker
import android.content.BroadcastReceiver
import android.content.IntentFilter

class UserActivity : AppCompatActivity() {
    private lateinit var statusButton: Button
    private lateinit var isExceeded: TextView
    private lateinit var mapView: MapView
    private lateinit var userMarker: Marker

    private val locationPermissionCode = 1


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


        mapView = binding.mapView.apply {
            setTileSource(TileSourceFactory.MAPNIK)
            setBuiltInZoomControls(true)
            setMultiTouchControls(true)
        }
        val mapController = mapView.controller
        mapController.setZoom(15.0)
        userMarker = Marker(mapView)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), locationPermissionCode)
        } else {
            setupMap()
        }

        userMarker = Marker(mapView)
        mapView.overlays.add(userMarker)
        LastLoginManager.setOnLocationIsChangedListener { Location ->
            runOnUiThread{
                updateMarker(Location)
            }
        }

        statusButton = binding.stopStartButton
        statusButton.setOnClickListener(){
            changeStatusButton()
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
        userMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        mapView.overlays.add(userMarker)
        LastLoginManager.setOnLocationIsChangedListener { location ->
            runOnUiThread {
                updateMarker(location)
            }
        }
    }

    private fun updateMarker(location: Location) {
        val userLocation = GeoPoint(location.latitude, location.longitude)
        userMarker.position = userLocation
        mapView.controller.setCenter(userLocation)
        mapView.invalidate()
    }

    override fun onResume() {
        super.onResume()
        Configuration.getInstance().load(this, getSharedPreferences("prefs", Context.MODE_PRIVATE))
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            setupMap()
        }
    }



    private fun changeStatusButton(){
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
