package com.example.bluetoothgame

import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.view.iterator
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.bluetoothgame.databinding.FragmentLoginBinding
import com.example.bluetoothgame.ui.leaderboard.LeaderboardViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
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
    interface RestApiLogin {
        @Headers("Content-Type: application/json")
        @POST("auth/")
        fun loginUser(@Body userData: Map<String, String>): Call<ResponseBody>
    }


    private var _binding: FragmentLoginBinding? = null
    private var _connected: Boolean = false
    private var _mainReady = false

    private lateinit var _submitButton:Button
    private lateinit var _switchButton:Button
    private lateinit var _title:TextView
    private lateinit var _emailField: EditText
    private lateinit var _passwordField: EditText
    private lateinit var _usernameField: EditText
    private lateinit var _passwordError: TextView
    private lateinit var _mailError: TextView
    private lateinit var _loginError: TextView
    private lateinit var _noWifi: ImageView
    private lateinit var _nav: BottomNavigationView

    private val _submitText: ArrayList<String> = arrayListOf("Log in", "Register")
    private var _submitState: Int = 0
    private val _switchText: ArrayList<String> = arrayListOf("No account?\nRegister", "Already registered? \n Log in")

    private lateinit var _db: DBInternal
    private lateinit var _trans: FragmentTransaction

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.most-seen-person.rmst.eu/api/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // network is available for use
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            _connected = true
            this@LoginFragment.requireActivity().runOnUiThread(Runnable {
                _noWifi.visibility = View.INVISIBLE
                _submitButton.isEnabled = true
                _submitButton.isClickable = true
            })

        }
        // lost network connection
        override fun onLost(network: Network) {
            super.onLost(network)
            _connected = false
            this@LoginFragment.requireActivity().runOnUiThread(Runnable {
                _noWifi.visibility = View.VISIBLE
                _submitButton.isEnabled = false
                _submitButton.isClickable = false
            })
        }
    }
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
        _noWifi = binding.noInternetImage
        _switchButton.text = "No account?\nRegister"
        _submitButton.text = "Log in"
        _emailField.visibility = View.GONE
        _passwordError = binding.errorPasswordText
        _passwordError.visibility = View.INVISIBLE
        _loginError = binding.errorLoginText
        _loginError.visibility = View.INVISIBLE
        _mailError = binding.errorMailText
        _mailError.visibility = View.GONE
        GlobalScope.launch { getBinding() }



        _switchButton.setOnClickListener{
            switch(it)
        }
        _submitButton.setOnClickListener{
            submit(it)
        }
        val cm = context?.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        cm.requestNetwork(networkRequest, networkCallback)
        return root
    }

    suspend fun getBinding(){
        while(!MainActivity.bindingIsInitialized()){}
        this@LoginFragment.requireActivity().runOnUiThread(Runnable {
            _nav = MainActivity.bindingMain.navView
            for (i in _nav.menu){
                i.isEnabled = false
                i.isCheckable = false
            }
        })
        _mainReady = true
        Log.i("ready", "binding obtained")
        return
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun submit(it: View?) {
        _loginError.visibility = View.INVISIBLE
        _passwordError.visibility = View.INVISIBLE
        val user:String = _usernameField.text.toString()
        val password:String = _passwordField.text.toString()
        if(_submitState == 0){
            _mailError.visibility = View.GONE
            logIn(user, password)
        }
        else{
            val email = _emailField.text.toString()
            _mailError.visibility = View.INVISIBLE
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
                    val r = response.errorBody()?.string()
                    if(r == null)
                    {
                        _passwordError.text = "Error"
                    }
                    else{
                        val json = JSONObject(r)
                        try {
                            _loginError.text = json.getJSONArray("username").getString(0)
                            _loginError.visibility = View.VISIBLE
                        }catch (e: Exception){}
                        try{
                            _mailError.text = json.getJSONArray("email").getString(0)
                            _mailError.visibility = View.VISIBLE
                        }catch (e: Exception){}
                        try{
                            _passwordError.text = json.getJSONArray("password").getString(0)
                            _passwordError.visibility = View.VISIBLE
                        }catch (e:Exception){}
                    }
                    _passwordError.visibility = View.VISIBLE
                    _passwordField.setText("")
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
        apiService.loginUser(body).enqueue(object : Callback<ResponseBody> {
            @RequiresApi(Build.VERSION_CODES.P)
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.code().toString()[0] != '2'){
                    Log.i("http", "Error: ${response.code()}")
                    val r = response.errorBody()?.string()
                    if (r == null){
                        _passwordError.text = "Error"
                    }
                    else{
                        val json = JSONObject(r)
                        try{
                            _passwordError.text = json.getJSONObject("appInfo").getString("text")
                        }catch (e:Exception){}
                        try {
                            _loginError.text = json.getJSONArray("username").getString(0)
                            _loginError.visibility = View.VISIBLE
                        }catch (e:Exception){}
                        try{
                            _passwordError.text = json.getJSONArray("password").getString(0)
                        }catch (e:Exception){}
                    }
                    _passwordError.visibility = View.VISIBLE
                    _passwordField.setText("")
                }
                else{
                    val r = response.body()?.string()
                    if(r == null){
                        Log.i("http", "Error: Empty response")
                    }
                    else{
                        Log.i("http", "${response.body()?.string()}")
                        val json = JSONObject(r)
                        val token = json.getString("token")
                        val u = json.getJSONObject("user")
                        val id = u.getString("id")
                        val email = u.getString("email")
                        Log.i("http", "log in finished, moving to main")
                        _db.logIn(id, user, email, token)
                        if (_mainReady) {
                            for (i in _nav.menu) {
                                i.isEnabled = true
                                i.isCheckable = true
                            }
                        }
                        val nav = findNavController()
                        nav.navigate(R.id.navigation_current_session)
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
        _emailField.setText("")
        _passwordField.setText("")
        _usernameField.setText("")
        if(_emailField.visibility == View.GONE){
            _emailField.visibility = View.VISIBLE
            _mailError.visibility = View.INVISIBLE
        }
        else{
            _emailField.visibility = View.GONE
            _mailError.visibility = View.GONE
        }
        _passwordError.visibility = View.INVISIBLE
        _loginError.visibility = View.INVISIBLE
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}