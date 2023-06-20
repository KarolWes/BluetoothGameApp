package com.example.bluetoothgame.ui.home

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.Color
import android.location.LocationManager
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
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast.LENGTH_SHORT
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothgame.DBInternal
import com.example.bluetoothgame.Device
import com.example.bluetoothgame.DeviceAdapter
import com.example.bluetoothgame.MainActivity
import com.example.bluetoothgame.R
import com.example.bluetoothgame.databinding.FragmentCurrentBinding
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import java.lang.reflect.Field
import java.lang.reflect.InvocationTargetException


data class PostBody(
    var recorder_mac: String,
    var recorder_name: String,
    var records: Array<Map<String,String>>
)

@OptIn(DelicateCoroutinesApi::class)
class HomeFragment : Fragment() {
    companion object{
        var connected: Boolean = false
        var token:String = ""
        var userId:String = ""
        var myMac = "00:00:00:00:00:00"
    }
    interface RestApiFoundDevices {
        @Headers("Content-Type: application/json")
        @POST("records/record/")
        fun saveRecord(@Header("Authorization") token:String,
                      @Body records: PostBody): Call<ResponseBody>
    }

    // variables
    private var PERMISSIONS: Array<String> = emptyArray()
    private var _binding: FragmentCurrentBinding? = null
    private var _refreshRate: Int = 30
    private var _scanNoName: Boolean = true
    private var _scanPaired: Boolean = false
    private var _discoveryFinished: Boolean = true
    private var _devices: ArrayList<Device> = arrayListOf()
    private var _toSend: ArrayList<Pair<Device, Long>> = arrayListOf()
    private var _newDevices: ArrayList<Device> = arrayListOf()
    private var _username: String = ""

    // layout elements
    private lateinit var _recyclerView: RecyclerView
    private lateinit var _refreshButton: ImageButton
    private lateinit var _cancelButton: ImageButton
    private lateinit var _uploadButton: ImageButton
    private lateinit var _syncImage: ImageView
    private lateinit var _uploadImage: ImageView
    private lateinit var _btOffImage: ImageView
    private lateinit var _noWifiImage: ImageView

    // various
    private lateinit var _bluetooth: BluetoothAdapter
    private lateinit var _bm: BluetoothManager
    private lateinit var _lm: LocationManager
    private lateinit var _cm: ConnectivityManager
    private lateinit var _internalDB: DBInternal

    //Constants
    private val binding get() = _binding!!
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.most-seen-person.rmst.eu/api/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    private val _receiver: BroadcastReceiver = object : BroadcastReceiver() {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device =
                        intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            Manifest.permission.BLUETOOTH_CONNECT
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        ActivityCompat.requestPermissions(requireActivity(), PERMISSIONS, 1)
                    }
                    if(device != null){
                        if(device.name == null){
                            if (_scanNoName){
                                _newDevices.add(Device("-", device.address))
                            }
                        }
                        else{
                            _newDevices.add(Device(device.name, device.address))
                        }
                        Log.i(
                            "bluetoothLog", """
                         ${device.name}
                         ${device.address}
                         """.trimIndent()
                        )
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_STARTED ->{
                    Log.i("bluetoothLog", "Started Discovery")
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED->{
                    Log.i("bluetoothLog", "Finished Discovery")
                    prepareDeviceList()
                    val devAdapter = DeviceAdapter(_devices)
                    _recyclerView.adapter = devAdapter
                    _recyclerView.layoutManager = LinearLayoutManager(context)
                    _discoveryFinished = true
                }
                BluetoothAdapter.ACTION_STATE_CHANGED->{
                    if (_bluetooth.isEnabled and _lm.isLocationEnabled){
                        _btOffImage.visibility = View.INVISIBLE
                    }
                    else{
                        _btOffImage.visibility = View.VISIBLE
                        _syncImage.visibility = View.INVISIBLE
                        _discoveryFinished = true
                        _bluetooth.cancelDiscovery()
                    }
                }
                LocationManager.PROVIDERS_CHANGED_ACTION->{
                    if (_bluetooth.isEnabled and _lm.isLocationEnabled){
                        _btOffImage.visibility = View.INVISIBLE
                    }
                    else{
                        _btOffImage.visibility = View.VISIBLE
                        _syncImage.visibility = View.INVISIBLE
                        _discoveryFinished = true
                        _bluetooth.cancelDiscovery()
                    }
                }
            }
        }
    }
    private val networkRequest = NetworkRequest.Builder()
        .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        .addTransportType(NetworkCapabilities.TRANSPORT_WIFI)
        .addTransportType(NetworkCapabilities.TRANSPORT_CELLULAR)
        .build()

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        // network is available for use
        override fun onAvailable(network: Network) {
            super.onAvailable(network)
            connected = true
            this@HomeFragment.requireActivity().runOnUiThread {
                _noWifiImage.visibility = View.INVISIBLE
            }
        }
        // lost network connection
        override fun onLost(network: Network) {
            super.onLost(network)
            connected = false
            this@HomeFragment.requireActivity().runOnUiThread {
                _noWifiImage.visibility = View.VISIBLE
            }
        }
    }

    // functions
    // overrides

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _devices = arrayListOf()
        _toSend = arrayListOf()
        _newDevices = arrayListOf()
        // setup
        PERMISSIONS += Manifest.permission.BLUETOOTH
        PERMISSIONS += Manifest.permission.BLUETOOTH_ADMIN
        PERMISSIONS += Manifest.permission.BLUETOOTH_CONNECT
        PERMISSIONS += Manifest.permission.BLUETOOTH_SCAN
        PERMISSIONS += Manifest.permission.ACCESS_COARSE_LOCATION
        PERMISSIONS += Manifest.permission.ACCESS_FINE_LOCATION
        checkPermissions(this.requireContext(), PERMISSIONS)
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]
        _binding = FragmentCurrentBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val textView: TextView = binding.textTitle
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        generateView(inflater, container)

        _internalDB = DBInternal(this.requireContext(), null, null, 1)
        val ans = _internalDB.getVals()
        if (ans == null){
            _internalDB.setDefault(30, 0, 1)
        }
        else{
            _username = ans[1]
            _refreshRate = Integer.parseInt(ans[5])
            _scanPaired = Integer.parseInt(ans[6]) == 1
            _scanNoName = Integer.parseInt(ans[7]) == 1
            myMac = ans[8]
        }
        token = _internalDB.getToken()
        userId = _internalDB.getUser()
        Log.i("Mac", myMac)
        if(token == ""){
            // Log in
            val nav = findNavController()
            nav.navigate(R.id.naviagtion_login)
        }


        // bluetooth adapter settings
        _bm = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        _lm = context?.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        _cm = context?.getSystemService(ConnectivityManager::class.java) as ConnectivityManager
        _bluetooth = _bm.adapter
        Log.i("BT", _bluetooth.name)
        if (ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this.requireActivity(), PERMISSIONS, 1)
        }
        if (!_bluetooth.isEnabled and !_lm.isLocationEnabled) {
            _btOffImage.visibility = View.VISIBLE
        }
        if(!isConnected()){
            _noWifiImage.visibility = View.VISIBLE
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        filter.addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        filter.addAction(LocationManager.PROVIDERS_CHANGED_ACTION)
        requireContext().registerReceiver(_receiver, filter)
        _cm.requestNetwork(networkRequest, networkCallback)
        GlobalScope.launch { syncCoroutine() }
        return root
    }
    @SuppressLint("MissingPermission")
    override fun onDestroyView() {
        _discoveryFinished = true
        _bluetooth.cancelDiscovery()
        requireContext().unregisterReceiver(_receiver)
        super.onDestroyView()
        _binding = null
    }

    // technical
    @SuppressLint("MissingPermission")
    fun prepareDeviceList(){
        if(!_scanPaired){
            val paired = _bluetooth.bondedDevices
            paired.forEach { dev ->
                val d = Device(dev.name, dev.address)
                _newDevices.remove(d)
            }
        }
        if(_devices.isEmpty()){
            _devices = _newDevices
            _newDevices = arrayListOf()
        }
        else{
            val tmp = ArrayList(_devices.toSet().minus(_newDevices.toSet()))
            for (entry in tmp){
                Log.i("bluetoothLog", "To send -> $entry")
                _toSend += Pair(entry, System.currentTimeMillis())
            }
            _devices = _newDevices
            _newDevices = arrayListOf()
            if(connected){
                GlobalScope.launch { saveDevicesOnApi() }
            }
        }
        _syncImage.visibility = View.INVISIBLE
    }
    private fun checkPermissions(context:Context, perm: Array<String>){
        for (p in perm){
            if(ActivityCompat.checkSelfPermission(context, p) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this.requireActivity(), perm, 1)
            }
        }
    }

    private fun isConnected():Boolean{
        val capabilities =_cm.getNetworkCapabilities(_cm.activeNetwork)
        if (capabilities != null) {
            if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                return true
            } else if (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) {
                return true
            }
        }
        return false
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    suspend fun syncCoroutine() {
        while(true){
            if(_bluetooth.isEnabled and _lm.isLocationEnabled){
                while(!_discoveryFinished){
                    delay(1000L *(_refreshRate+10))
                }
                _discoveryFinished = false
                Log.i("Sync", "Synchronizing")
                this@HomeFragment.requireActivity().runOnUiThread {
                    _syncImage.visibility = View.VISIBLE
                }
                this._bluetooth.startDiscovery()
                delay(1000L *(_refreshRate)+10)
            }
        }
    }

    @SuppressLint("DiscouragedPrivateApi") // DOESN'T WORK
    private fun getBluetoothMacAddress(): String? {
        val bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        var bluetoothMacAddress: String? = ""
        try {
            val mServiceField: Field = bluetoothAdapter.javaClass.getDeclaredField("mService")
            mServiceField.isAccessible = true
            val btManagerService: Any = mServiceField.get(bluetoothAdapter)
            if (btManagerService != null) {
                bluetoothMacAddress = btManagerService.javaClass.getMethod("getAddress")
                    .invoke(btManagerService) as String
            }
        } catch (e: NoSuchFieldException) {
        } catch (e: NoSuchMethodException) {
        } catch (e: IllegalAccessException) {
        } catch (e: InvocationTargetException) {
        }
        return bluetoothMacAddress
    }

    suspend fun saveDevicesOnApi(){
        val local = _toSend
        _toSend = arrayListOf() // clear the list
        val apiService = retrofit.create(RestApiFoundDevices::class.java)
        var rec: Array<Map<String, String>> = arrayOf()
        for (el in local){
            if(!el.first.my){
                val record = mapOf(
                    "mac" to el.first.address,
                    "name" to el.first.name
                )
                rec += record
            }
        }
        if(rec.isNotEmpty()){
            val body = PostBody(
                recorder_mac = myMac,
                recorder_name = _username,
                records = rec
            )
            apiService.saveRecord("Token $token", body).enqueue(object :
                Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    if (response.code().toString()[0] != '2') {
                        Log.i("http", "Error: ${response.code()}")
                        Log.i("http", "${response.errorBody()?.string()}")
                        Snackbar.make(MainActivity.bindingMain.mainLayout, R.string.upload_error, Snackbar.LENGTH_SHORT).show()
                    }
                    else{
                        response.body()?.string()?.let { Log.i("http", it) }
                        Snackbar.make(MainActivity.bindingMain.mainLayout, R.string.upload_done, Snackbar.LENGTH_SHORT).show()
                    }
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    Snackbar.make(MainActivity.bindingMain.mainLayout, R.string.upload_error, Snackbar.LENGTH_SHORT).show()
                    Log.i("http", "Error")
                }
            })
        }
        else{
            Log.i("http","Nothing to send")
        }
        return
    }

    // graphical
    @RequiresApi(Build.VERSION_CODES.P)
    private fun generateView(
        inflater: LayoutInflater,
        container: ViewGroup?
    ){
        // bindings
        _recyclerView = binding.visibleDevicesList
        _refreshButton = binding.buttonRefresh
        _refreshButton.setOnClickListener { sync(it) }
        _cancelButton = binding.buttonCancel
        _cancelButton.setOnClickListener { cancel(it) }
        _uploadButton = binding.buttonUpload
        _uploadButton.setOnClickListener { upload(it) }
        _syncImage = binding.syncImage
        _syncImage.visibility = View.INVISIBLE
        _uploadImage = binding.uploadImage
        _uploadImage.visibility = View.INVISIBLE
        _btOffImage = binding.bluetoothOffImage
        _btOffImage.visibility = View.INVISIBLE
        _noWifiImage = binding.noWifiImage
        _noWifiImage.visibility = View.INVISIBLE
    }

    @RequiresApi(Build.VERSION_CODES.P)
    @SuppressLint("MissingPermission")
    fun sync(v:View){
        Log.i("Button", "sync clicked")
        if(_bluetooth.isEnabled and _lm.isLocationEnabled){
                _discoveryFinished = false
                _syncImage.visibility = View.VISIBLE
                _bluetooth.startDiscovery()
        }

    }

    fun upload(v:View){
        if(connected){
            Log.i("Button", "upload clicked")
            for(el in _devices){
                _toSend += Pair(el, System.currentTimeMillis() )
            }
            GlobalScope.launch { saveDevicesOnApi() }

        }
    }

    @SuppressLint("MissingPermission")
    @RequiresApi(Build.VERSION_CODES.P)
    fun cancel(v:View){
        if(_bluetooth.isEnabled and _lm.isLocationEnabled) {
            Log.i("Button", "cancel clicked")
            _bluetooth.cancelDiscovery()
            for(el in _devices){
                _toSend += Pair(el, System.currentTimeMillis() )
            }
            if(connected){
                GlobalScope.launch { saveDevicesOnApi() }
            }
        }
    }

}