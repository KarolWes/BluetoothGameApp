package com.example.bluetoothgame.ui.leaderboard

import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.bluetoothgame.DBInternal
import com.example.bluetoothgame.R
import com.example.bluetoothgame.databinding.FragmentLeaderboardBinding
import com.example.bluetoothgame.ui.home.HomeFragment
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

class LeaderboardFragment : Fragment() {

    interface RestApiGetLeaderboard {
        @Headers("Content-Type: application/json")
        @GET("devices")
        fun getLeaderboard(
            @Header("Authorization") token: String,
            @Query("PA") page: Int,
            @Query("page-size") ps: Int,
        ): Call<ResponseBody>
    }

    private var _binding: FragmentLeaderboardBinding? = null
    private lateinit var _db: DBInternal
    private lateinit var _refreshButton: ImageButton

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.most-seen-person.rmst.eu/api/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val leaderboardViewModel =
            ViewModelProvider(this).get(LeaderboardViewModel::class.java)

        _binding = FragmentLeaderboardBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textTitle
        leaderboardViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        _db = DBInternal(this.requireContext(), null, null, 1)
        var token = _db.getToken()
        if (token == "") {
            // Log in
            val nav = findNavController()
            nav.navigate(R.id.naviagtion_login)
        }
        getScoreFromApi() // here?
        return root
    }

    private fun getScoreFromApi() {
        val apiService = retrofit.create(RestApiGetLeaderboard::class.java)
        apiService.getLeaderboard("Token ${HomeFragment.token}", 1, 10).enqueue(object :
            Callback<ResponseBody> {
            override fun onResponse(
                call: Call<ResponseBody>,
                response: Response<ResponseBody>
            ) {
                if (response.code().toString()[0] != '2') {
                    Log.i("http", "Error: ${response.code()}")
                    Log.i("http", "${response.errorBody()?.string()}")
                } else {
                    response.body()?.string()?.let {
                        Log.i("http1", it)
                    }
                }
            }

            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.i("http", "Error")
            }
        })
        return
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    @RequiresApi(Build.VERSION_CODES.P)
    private fun generateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ){
        // bindings
        _refreshButton = binding.buttonRefresh
        _refreshButton.setOnClickListener { sync(it) }
    }

    fun sync(v:View){
        Log.i("Button", "refresh clicked")
        getScoreFromApi()
    }
}