package emk.driver.gpsdriverapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import emk.driver.gpsdriverapplication.services.LocationService
import emk.driver.oversimplification.databinding.ActivityMainBinding
import android.content.SharedPreferences


class MainActivity : AppCompatActivity() {

    private val locationPermissionCode = 123

    private lateinit var loginEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    private val PREF_NAME = "LoginPrefs"
    private val KEY_IS_LOGGED_IN = "isLoggedIn"
    private lateinit var sharedPreferences: SharedPreferences


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)

        // Access views using the binding object
        loginEditText = binding.editTextLogin
        passwordEditText = binding.editTextPassword
        loginButton = binding.loginButton

        // Set content view using the binding's root view
        setContentView(binding.root)

        // Set OnClickListener for the login button
        loginButton.setOnClickListener {
            validateLogin()
        }

        // Check and request location permissions
        checkLocationPermission()
    }

    private fun validateLogin() {
        val enteredUsername = loginEditText.text.toString()
        val enteredPassword = passwordEditText.text.toString()

        // Check credentials and navigate accordingly
        when {
            enteredUsername == "admin" && enteredPassword == "admin" -> {
                navigateToAdminActivity()
            }
            enteredUsername == "user" && enteredPassword == "user" -> {
                navigateToUserActivity()
            }
            else -> {
                // Show an error message for incorrect credentials
                Toast.makeText(this, "Invalid username or password", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToAdminActivity() {
        // Navigate to AdminActivity
        val adminIntent = Intent(this, AdminActivity::class.java)
        startActivity(adminIntent)
        finish() // Optional: Close the current activity
    }

    private fun navigateToUserActivity() {
        // Navigate to UserActivity
        val userIntent = Intent(this, UserActivity::class.java)
        startActivity(userIntent)
        finish() // Optional: Close the current activity
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Request location permissions
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        } else {
            // Permissions already granted, proceed with location updates
            setupLocationUpdates()
        }
    }

    private fun setupLocationUpdates() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            startLocationService()
        } else {
            // Handle the case where permission is not granted
        }
    }

    private fun startLocationService() {
        val serviceIntent = Intent(this, LocationService::class.java)
        startService(serviceIntent)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == locationPermissionCode && grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            // Permission granted, proceed with location updates
            setupLocationUpdates()
        } else {
            // Permission denied, handle accordingly
        }
    }

    // Other functions and overrides go here
}
