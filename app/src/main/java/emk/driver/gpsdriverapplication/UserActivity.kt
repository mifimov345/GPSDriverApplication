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


class UserActivity : AppCompatActivity() {
    private lateinit var statusButton: Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = UserActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

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
