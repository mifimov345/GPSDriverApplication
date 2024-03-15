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

class MainActivity : AppCompatActivity() {

    private val locationPermissionCode = 123


    private lateinit var loginEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityMainBinding.inflate(layoutInflater)

        loginEditText = binding.editTextLogin
        passwordEditText = binding.editTextPassword
        loginButton = binding.loginButton

        setContentView(binding.root)

        loginButton.setOnClickListener {
            validateLogin()
        }

        if (LastLoginManager.lastLogin == "admin")
            navigateToAdminActivity()
        if (LastLoginManager.lastLogin !== "user")
            navigateToUserActivity()
        //просьба врубить GPS
        checkLocationPermission()
    }

    private fun validateLogin() {
        val enteredUsername = loginEditText.text.toString()
        val enteredPassword = passwordEditText.text.toString()

        when {
            enteredUsername == "admin" && enteredPassword == "admin" -> {
                validateGlobalLogin(enteredUsername)
                navigateToAdminActivity()
            }
            enteredUsername == "user" && enteredPassword == "user" -> {
                validateGlobalLogin(enteredUsername)
                navigateToUserActivity()
            }
            else -> {
                Toast.makeText(this, "Неправильный логин или пароль", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun navigateToAdminActivity() {
        val adminIntent = Intent(this, AdminActivity::class.java)
        startActivity(adminIntent)
        finish()
    }

    private fun navigateToUserActivity() {
        val userIntent = Intent(this, UserActivity::class.java)
        startActivity(userIntent)
        finish()
    }

    private fun checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Просьба доступа
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                locationPermissionCode
            )
        }
    }

    private fun validateGlobalLogin(login:String){
        LastLoginManager.lastLogin = login
    }
}
