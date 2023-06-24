package com.example.bluetoothgame.ui.leaderboard

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
import android.widget.ImageButton
import android.widget.Switch
import android.widget.ImageView
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
    var next: Int?,
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


    private var _connected: Boolean = false
    private var _binding: FragmentLeaderboardBinding? = null
    private lateinit var _db: DBInternal
    private lateinit var _refreshButton: ImageButton
    private lateinit var _textViewRank1: TextView
    private lateinit var _textViewRank2: TextView
    private lateinit var _textViewRank3: TextView
    private lateinit var _textViewRankUser: TextView
    private lateinit var _switch: Switch
    private lateinit var userLeaderboard: UserLeaderboard
    private lateinit var deviceLeaderboard: DeviceLeaderboard
    private lateinit var _noWifi: ImageView

    private var _user = ""
    private var _userRank = -1
    private var _userScore = -1

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
            this@LeaderboardFragment.requireActivity().runOnUiThread(Runnable {
                _noWifi.visibility = View.INVISIBLE
                _refreshButton.isEnabled = true
                _refreshButton.isClickable = true
            })

        }

        // lost network connection
        override fun onLost(network: Network) {
            super.onLost(network)
            _connected = false
            this@LeaderboardFragment.requireActivity().runOnUiThread(Runnable {
                _noWifi.visibility = View.VISIBLE
                _refreshButton.isEnabled = false
                _refreshButton.isClickable = false
                val textUser =
                    "You (${_user}) Rank: ?\nScore: ?"
                _textViewRankUser.text = (textUser)
            })
        }
    }

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
        _user = _db.getUser()
        generateView(inflater, container)
        var token = _db.getToken()
        if (token == "") {
            // Log in
            val nav = findNavController()
            nav.navigate(R.id.naviagtion_login)
        }
        val cm = context?.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        cm.requestNetwork(networkRequest, networkCallback)
        Log.i("Test", "Creating view!")
        return root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.i("Test", "View created!")
        getUserRankingFromApi()
        getDeviceRankingFromApi()
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
                        _userRank = -1
                        _userScore = -1
                        var i = 0
                        while (i < userLeaderboard.count && i < entriesPerPage) {
                            //Log.i("httpL", userLeaderboard.results[i].username)
                            if (userLeaderboard.results[i].username == _user) {
                                _userRank = userLeaderboard.results[i].rank
                                _userScore = userLeaderboard.results[i].seen_counter
                                Log.i("httpL", "Found user in first list")
                                break
                            }
                            Log.i("httpL", "Entry: ${userLeaderboard.results[i]}")
                            i++
                        }
                        if (_userRank == -1) {
                            if (userLeaderboard.next != null) {
                                getRankingOfUser(2)
                                Log.i("httpL", "Searching full ranking")
                            }
                        }
                        setScores()
                    }
                }

                override fun onFailure(call: Call<UserLeaderboard>, t: Throwable) {
                    Log.i("httpL", "Error, when fetching user leaderboard")
                }
            })
        return
    }

    private fun getRankingOfUser(page: Int) {
        Log.i("httpL", "Searching for user in ranking")
        val entriesPerPage = 10
        val apiService = retrofit.create(RestApiGetLeaderboard::class.java)
        apiService.getUserLeaderboard("Token ${HomeFragment.token}", page, entriesPerPage)
            .enqueue(object :
                Callback<UserLeaderboard> {
                override fun onResponse(
                    call: Call<UserLeaderboard>,
                    response: Response<UserLeaderboard>
                ) {
                    Log.i("httpL", "Got a response")
                    if (response.code().toString()[0] != '2') {
                        Log.i("httpL", "Error: ${response.code()}")
                        Log.i("httpL", "${response.errorBody()?.string()}")
                    } else {
                        val userLeaderboard = response.body()!!
                        var i = 0
                        Log.i("httpL", "Searching for user on page $page..")
                        while (i < userLeaderboard.count && i < entriesPerPage) {
                            if (userLeaderboard.results[i].username == _user) {
                                _userRank = userLeaderboard.results[i].rank
                                _userScore = userLeaderboard.results[i].seen_counter
                                return
                            }
                            Log.i("httpL", "Entry: ${userLeaderboard.results[i]}")
                            i++
                        }
                        if (_userRank == -1) {
                            if (userLeaderboard.next == null) {
                                Log.i("httpL", "Not found in ranking")
                                _userRank = -2
                                _userScore = -2
                                return
                            } else {
                                getRankingOfUser(page + 1)
                            }
                        }
                    }
                }

                override fun onFailure(call: Call<UserLeaderboard>, t: Throwable) {
                    Log.i("httpL", "Error")
                }
            })


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
                    Log.i("httpL", "Error while fetching the results")
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
        _textViewRank1 = binding.textViewGold
        _textViewRank2 = binding.textViewSilver
        _textViewRank3 = binding.textViewBronze
        _noWifi = binding.noInternetImage2
        if (_connected) {
            _noWifi.visibility = View.INVISIBLE
        } else {
            _noWifi.visibility = View.VISIBLE
            _refreshButton.isEnabled = false
            _refreshButton.isClickable = false
        }
        _textViewRankUser = binding.textView
        _switch = binding.switch1
        val textUser =
            "You (${_user}) Rank: ?\nScore: ?"
        _textViewRankUser.text = (textUser)
        _refreshButton.setOnClickListener { sync(it) }
        _switch.setOnClickListener { sync(it) }
    }

    private fun sync(v: View) {
        Log.i("Button", "refresh clicked")
        getUserRankingFromApi()
        getDeviceRankingFromApi()
    }

    private fun setScores() {
        if (!_switch.isChecked) {
            val text1 =
                "${userLeaderboard.results[0].username}\n${userLeaderboard.results[0].seen_counter}"
            _textViewRank1.text = (text1)
            val text2 =
                "${userLeaderboard.results[1].username}\n${userLeaderboard.results[1].seen_counter}"
            _textViewRank2.text = (text2)
            val text3 =
                "${userLeaderboard.results[2].username}\n${userLeaderboard.results[2].seen_counter}"
            _textViewRank3.text = (text3)
            val textUser =
                "You (${_user}) Rank: ${if (_userRank == -2) "Not in ranking" else "$_userRank\nScore: $_userScore"}"
            _textViewRankUser.text = (textUser)
        } else {
            val text1 =
                "${deviceLeaderboard.results[0].id}\n${deviceLeaderboard.results[0].seen_counter}"
            _textViewRank1.text = (text1)
            val text2 =
                "${deviceLeaderboard.results[1].id}\n${deviceLeaderboard.results[1].seen_counter}"
            _textViewRank2.text = (text2)
            val text3 =
                "${deviceLeaderboard.results[2].id}\n${deviceLeaderboard.results[2].seen_counter}"
            _textViewRank3.text = (text3)
            val textUser =
                "You (${_user}) Rank: ${if (_userRank == -2) "Not in ranking" else "$_userRank\nScore: $_userScore"}"
            _textViewRankUser.text = (textUser)
        }
    }
}