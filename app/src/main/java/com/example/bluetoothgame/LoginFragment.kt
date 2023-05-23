package com.example.bluetoothgame

import android.app.Activity
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.bluetoothgame.databinding.FragmentLoginBinding
import com.example.bluetoothgame.ui.home.HomeFragment
import com.example.bluetoothgame.ui.leaderboard.LeaderboardViewModel
import okhttp3.ResponseBody
import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST


class LoginFragment : Fragment() {
    interface RestApiRegister{
        @Headers("Content-Type: application/json")
        @POST("register/")
        fun registerUser(@Body userData: Map<String, String>): Call<ResponseBody>
    }
    interface RestApiLogin{
        @Headers("Content-Type: application/json")
        @POST("auth/")
        fun registerUser(@Body userData: Map<String, String>): Call<ResponseBody>
    }


    private var _binding: FragmentLoginBinding? = null
    private lateinit var _submitButton:Button
    private lateinit var _switchButton:Button
    private lateinit var _title:TextView
    private lateinit var _emailField: EditText
    private lateinit var _passwordField: EditText
    private lateinit var _usernameField: EditText
    private val _submitText: ArrayList<String> = arrayListOf("Log in", "Register")
    private var _submitState: Int = 0
    private val _switchText: ArrayList<String> = arrayListOf("No account?\nRegister", "Already registered? \n Log in")
    private lateinit var _db: DBInternal
    private lateinit var _trans: FragmentTransaction
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.most-seen-person.rmst.eu/api/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val leaderboardViewModel =
            ViewModelProvider(this).get(LeaderboardViewModel::class.java)

        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        val root: View = binding.root

        _title = binding.loginTitleText
        leaderboardViewModel.text.observe(viewLifecycleOwner) {
            _title.text = "Log In"
        }
        _trans = this.requireActivity().supportFragmentManager.beginTransaction()
        _db = DBInternal(this.requireContext(), null, null, 1)
        _switchButton = binding.switchButton
        _submitButton = binding.submitButton
        _emailField = binding.emailInput
        _passwordField = binding.passwordInput
        _usernameField = binding.usernameInput
        _switchButton.text = "No account?\nRegister"
        _submitButton.text = "Log in"
        _emailField.visibility = View.GONE
        _switchButton.setOnClickListener{
            switch(it)
        }
        _submitButton.setOnClickListener{
            submit(it)
        }

        return root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun submit(it: View?) {
        val user:String = _usernameField.text.toString()
        val password:String = _passwordField.text.toString()
        val apiService = RestApiService()
        if(_submitState == 0){
            logIn(user, password)
        }
        else{
            val email = _emailField.text.toString()
            registerUser(user, password, email)
        }
    }
    fun registerUser(user: String, password: String, email: String) {

        val apiService = retrofit.create(RestApiRegister::class.java)
        val body = mapOf(
            "username" to user,
            "password" to password,
            "email" to email
        )
        apiService.registerUser(body).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.code().toString()[0] != '2'){
                    Log.i("http", "Error: ${response.code()}")
                    response.body()?.string()?.let { Log.i("http", it) }
                }
                else{
                    response.body()?.string()?.let { Log.i("http", it) }
                    logIn(user, password)
                }

            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.i("http", "Error")
            }
        })
    }

    private fun logIn(user:String, password: String){
        val apiService = retrofit.create(RestApiLogin::class.java)
        val body = mapOf(
            "username" to user,
            "password" to password
        )
        apiService.registerUser(body).enqueue(object : Callback<ResponseBody> {
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.code().toString()[0] != '2'){
                    Log.i("http", "Error: ${response.code()}")
                }
                else{
                    val r = response.body()?.string()
                    if(r == null){
                        Log.i("http", "Error: Empty response")
                    }
                    else{
                        val json = JSONObject(r)
                        val token = json.getString("token")
                        val u = json.getJSONObject("user")
                        val id = u.getString("id")
                        val email = u.getString("email")
                        Log.i("http", "log in finished, moving to main")
                        _db.logIn(id, user, email, token)
                        val nav = findNavController()
                        nav.navigate(R.id.naviagtion_login)
                    }
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.i("http", "Error")
            }
        })
    }

    private fun switch(it: View?) {
        _submitState = _submitState xor 1
        _switchButton.text = _switchText[_submitState]
        _submitButton.text = _submitText[_submitState]
        _title.text = _submitText[_submitState]
        if(_emailField.visibility == View.GONE){
            _emailField.visibility = View.VISIBLE
        }
        else{
            _emailField.visibility = View.GONE
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}