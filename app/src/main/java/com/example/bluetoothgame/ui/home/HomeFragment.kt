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
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothgame.Device
import com.example.bluetoothgame.DeviceAdapter
import com.example.bluetoothgame.databinding.FragmentCurrentBinding
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class HomeFragment : Fragment() {

    // variables
    private var _binding: FragmentCurrentBinding? = null
    private var PERMISSIONS: Array<String> = emptyArray()
    private var _refreshRate: Int = 30
    private var _discoveryFinished: Boolean = true

    // layout elements
    private lateinit var _devices: ArrayList<Device>
    private lateinit var _toSend: ArrayList<Pair<Device, Long>>
    private lateinit var _newDevices: ArrayList<Device>
    private lateinit var _bluetooth: BluetoothAdapter
    private lateinit var _bm: BluetoothManager
    private lateinit var _recyclerView: RecyclerView
    private lateinit var _refreshButton: ImageButton
    private lateinit var _cancelButton: ImageButton
    private lateinit var _uploadButton: ImageButton
    private lateinit var _syncImage: ImageView
    private lateinit var _uploadImage: ImageView
    private lateinit var _btOffImage: ImageView

    //Constants
    private val binding get() = _binding!!
    private val _receiver: BroadcastReceiver = object : BroadcastReceiver() {
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
                            _newDevices.add(Device("-", device.address))
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
            }
        }
    }
    fun prepareDeviceList(){
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

    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
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

        // bluetooth adapter settings
        _bm = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        _bluetooth = _bm.adapter
        if (ActivityCompat.checkSelfPermission(
                this.requireContext(),
                Manifest.permission.BLUETOOTH_SCAN
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(this.requireActivity(), PERMISSIONS, 1)
        }
        if (!_bluetooth.isEnabled) {
            _btOffImage.visibility = View.VISIBLE
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_STARTED)
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        requireContext().registerReceiver(_receiver, filter)
        //deviceList setup
        _devices = arrayListOf()
        _toSend = arrayListOf()
        _newDevices = arrayListOf()
        GlobalScope.launch { sync_coroutine() }

        return root
    }


    override fun onDestroyView() {
        requireContext().unregisterReceiver(_receiver);
        super.onDestroyView()
        _binding = null
    }


    @SuppressLint("MissingPermission")
    fun sync(v:View){
        Log.i("Button", "sync clicked")
        if(_bluetooth.isEnabled){
            if(_discoveryFinished){
                _discoveryFinished = false
                _syncImage.visibility = View.VISIBLE
                _bluetooth.startDiscovery()
            }
        }

    }

    @SuppressLint("MissingPermission")
    suspend fun sync_coroutine() {
        while(true){
            if(_bluetooth.isEnabled){
                while(!_discoveryFinished){
                    delay(1000L *_refreshRate/2)
                }
                _discoveryFinished = false
                Log.i("Sync", "Synchronizing")
                this@HomeFragment.requireActivity().runOnUiThread(Runnable {
                    _btOffImage.visibility = View.INVISIBLE
                    _syncImage.visibility = View.VISIBLE
                })
                this._bluetooth.startDiscovery()
                delay(1000L *_refreshRate)
            }
            else{
                this@HomeFragment.requireActivity().runOnUiThread(Runnable {
                    _btOffImage.visibility = View.VISIBLE
                })
            }
        }
    }
    fun upload(v:View){
        Log.i("Button", "upload clicked")
    }

    fun cancel(v:View){
        Log.i("Button", "cancel clicked")
    }

}