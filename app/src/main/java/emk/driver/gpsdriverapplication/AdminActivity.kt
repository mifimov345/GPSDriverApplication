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
import android.widget.ArrayAdapter
import android.widget.Spinner
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
import emk.driver.oversimplification.databinding.AdminActivityMainBinding
import emk.driver.gpsdriverapplication.services.LocationService
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.GET
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit

data class CreateRequest(val login: String, val password: String , val type: String)

data class CreateResponse(val answer: String)

interface CreateService {
    @POST("/create")
    fun sendRequest(@Body request: CreateRequest): Call<CreateResponse>
}


class AdminActivity : AppCompatActivity() {

    private val retrofit = Retrofit.Builder()
        .baseUrl("http://10.0.2.2:5000/create/") // Replace this with your server URL
        .addConverterFactory(GsonConverterFactory.create())
        .build()


    private val createService = retrofit.create(CreateService::class.java)

    private lateinit var editCreationLogin : EditText
    private lateinit var editCreationPassword : EditText
    private lateinit var spinner_of_type : Spinner
    private lateinit var CreateButton : Button
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = AdminActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        editCreationLogin = binding.editCreationLogin
        editCreationPassword = binding.editCreationPassword
        spinner_of_type = binding.spinnerOfType
        CreateButton = binding.CreateButton

        val items = arrayOf("User","Admin")

        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, items)

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        spinner_of_type.adapter = adapter

        spinner_of_type.setSelection(0)

        CreateButton.setOnClickListener{
            createUser()
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

    private fun createUser(){
        val enteredUsername = editCreationLogin.text.toString()
        val enteredPassword = editCreationPassword.text.toString()
        val enteredType = spinner_of_type.selectedItem.toString()

        val request = CreateRequest(enteredUsername,enteredPassword,enteredType)
        createService.sendRequest(request).enqueue(object: Callback<CreateResponse>{
            override fun onResponse(call: Call<CreateResponse>, response: Response<CreateResponse>){
                if (response.isSuccessful){
                    Toast.makeText(this@AdminActivity,"Успешно!", Toast.LENGTH_SHORT).show()
                }
                else{
                    Toast.makeText(this@AdminActivity,"Пользователь уже существует!",Toast.LENGTH_SHORT).show()
                }
            }

            override fun onFailure(call: Call<CreateResponse>, t: Throwable) {
                Toast.makeText(this@AdminActivity, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                println("Error: ${t.message}")
                t.printStackTrace()
            }
        })


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
