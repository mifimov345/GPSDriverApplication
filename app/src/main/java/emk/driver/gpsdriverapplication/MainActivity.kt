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
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit


data class LoginRequest(val login: String, val password: String)

data class LoginResponse(val type: String)

interface AuthService {
    @POST("/login")
    fun login(@Body request: LoginRequest): Call<LoginResponse>
}


class MainActivity : AppCompatActivity() {

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5000/login/") // Replace this with your server URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()


    private val authService = retrofit.create(AuthService::class.java)


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

/*        if (LastLoginManager.lastLogin == "admin")
            navigateToAdminActivity()
        if (LastLoginManager.lastLogin !== "user")
            navigateToUserActivity()*/
        //просьба врубить GPS
        checkLocationPermission()
    }

    private fun validateLogin() {
        val enteredUsername = loginEditText.text.toString()
        val enteredPassword = passwordEditText.text.toString()

        val request = LoginRequest(enteredUsername, enteredPassword)

        authService.login(request).enqueue(object : Callback<LoginResponse> {
             override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                if (response.isSuccessful) {
                    println("response: ${response}")
                    val type = response.body()?.type
                    if (type == "admin") {
                        validateGlobalLogin(enteredUsername)
                        navigateToAdminActivity()
                    } else if (type == "user") {
                        validateGlobalLogin(enteredUsername)
                        navigateToUserActivity()
                    } else {
                        Toast.makeText(this@MainActivity, "Invalid type", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MainActivity, "Login failed", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@MainActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                println("Error: ${t.message}")
                t.printStackTrace()
            }
        })
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
