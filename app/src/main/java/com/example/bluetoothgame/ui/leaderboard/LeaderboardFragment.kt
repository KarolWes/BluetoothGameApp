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
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.Query

data class UserLeaderboard(
    var next: Int,
    var previous: Int,
    var count: Int,
    var results: Array<UserLeaderboardEntry>
)

data class UserLeaderboardEntry(
    var id: Int,
    var rank: Int,
    var seen_counter: Int,
    var username: String,
)

data class DeviceLeaderboard(
    var next: Int,
    var previous: Int,
    var count: Int,
    var results: Array<DeviceLeaderboardEntry>
)

data class DeviceLeaderboardEntry(
    var id: Int,
    var rank: Int,
    var seen_counter: Int,
    var mac_address: String
)

class LeaderboardFragment : Fragment() {

    interface RestApiGetLeaderboard {
        @Headers("Content-Type: application/json")
        @GET("users")
        fun getUserLeaderboard(
            @Header("Authorization") token: String,
            @Query("PA") page: Int,
            @Query("page-size") ps: Int,
        ): Call<UserLeaderboard>

        @GET("devices")
        fun getDeviceLeaderboard(
            @Header("Authorization") token: String,
            @Query("PA") page: Int,
            @Query("page-size") ps: Int,
        ): Call<DeviceLeaderboard>
    }

    private var _binding: FragmentLeaderboardBinding? = null
    private lateinit var _db: DBInternal
    private lateinit var _refreshButton: ImageButton
    private lateinit var userLeaderboard: UserLeaderboard
    private lateinit var deviceLeaderboard: DeviceLeaderboard

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
        getUserRankingFromApi() // here?
        getDeviceRankingFromApi()
        return root
    }

    private fun getUserRankingFromApi() {
        val page = 1
        val entriesPerPage = 10
        val apiService = retrofit.create(RestApiGetLeaderboard::class.java)
        apiService.getUserLeaderboard("Token ${HomeFragment.token}", page, entriesPerPage)
            .enqueue(object :
                Callback<UserLeaderboard> {
                override fun onResponse(
                    call: Call<UserLeaderboard>,
                    response: Response<UserLeaderboard>
                ) {
                    if (response.code().toString()[0] != '2') {
                        Log.i("httpL", "Error: ${response.code()}")
                        Log.i("httpL", "${response.errorBody()?.string()}")
                    } else {

                        Log.i("httpL", "${response.body()}")
                        userLeaderboard = response.body()!!

                        // Sort the results by rank
                        userLeaderboard.results.sortWith { e1: UserLeaderboardEntry,
                                                           e2: UserLeaderboardEntry ->
                            e1.rank - e2.rank
                        }
                        var i = 0
                        while (i < userLeaderboard.count && i < entriesPerPage) {
                            Log.i("httpL", "Entry: ${userLeaderboard.results[i]}")
                            i++
                        }

                    }
                }

                override fun onFailure(call: Call<UserLeaderboard>, t: Throwable) {
                    Log.i("httpL", "Error")
                }
            })
        return
    }

    private fun getDeviceRankingFromApi() {
        val page = 1
        val entriesPerPage = 10
        val apiService = retrofit.create(RestApiGetLeaderboard::class.java)
        apiService.getDeviceLeaderboard("Token ${HomeFragment.token}", page, entriesPerPage)
            .enqueue(object :
                Callback<DeviceLeaderboard> {
                override fun onResponse(
                    call: Call<DeviceLeaderboard>,
                    response: Response<DeviceLeaderboard>
                ) {
                    if (response.code().toString()[0] != '2') {
                        Log.i("httpL", "Error: ${response.code()}")
                        Log.i("httpL", "${response.errorBody()?.string()}")
                    } else {

                        Log.i("httpL", "${response.body()}")
                        deviceLeaderboard = response.body()!!

                        // Sort the results by rank
                        // (not needed for devices since they are already sorted)
                        deviceLeaderboard.results.sortWith { e1: DeviceLeaderboardEntry,
                                                             e2: DeviceLeaderboardEntry ->
                            e1.rank - e2.rank
                        }
                        var i = 0
                        while (i < deviceLeaderboard.count && i < entriesPerPage) {
                            Log.i("httpL", "Entry: ${deviceLeaderboard.results[i]}")
                            i++
                        }

                    }
                }

                override fun onFailure(call: Call<DeviceLeaderboard>, t: Throwable) {
                    Log.i("httpL", "Error")
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
    ) {
        // bindings
        _refreshButton = binding.buttonRefresh
        _refreshButton.setOnClickListener { sync(it) }
    }

    fun sync(v: View) {
        Log.i("Button", "refresh clicked")
        getUserRankingFromApi()
    }
}