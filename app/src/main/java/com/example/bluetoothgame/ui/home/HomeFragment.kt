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
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothgame.Device
import com.example.bluetoothgame.DeviceAdapter
import com.example.bluetoothgame.databinding.FragmentCurrentBinding


class HomeFragment : Fragment() {

    private var _binding: FragmentCurrentBinding? = null
    private var PERMISSIONS: Array<String> = emptyArray()
    private lateinit var _devices: ArrayList<Device>
    private lateinit var _bluetooth: BluetoothAdapter
    private lateinit var _bm: BluetoothManager
    private lateinit var _recyclerView: RecyclerView

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    fun getNearbyDevices(){

    }

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                Log.i("Permission: ,", "granted")
            } else {
                Log.i("Permission: ,", "denied")
            }
        }

    private fun requestPermission(context: Context, perm: String){
        when (PackageManager.PERMISSION_GRANTED) {
            ContextCompat.checkSelfPermission(
                context,
                perm
            ) -> {
                // You can use the API that requires the permission.
            }
            else -> {
                requestPermissionLauncher.launch(
                    Manifest.permission.BLUETOOTH_ADMIN)
            }
        }
    }

    private fun checkPermissions(context:Context, perm: Array<String>){
            if(ActivityCompat.checkSelfPermission(context, perm[0]) != PackageManager.PERMISSION_GRANTED){
                ActivityCompat.requestPermissions(this.requireActivity(), perm, 1)
            }
    }

    @RequiresApi(Build.VERSION_CODES.S)
    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        PERMISSIONS += Manifest.permission.BLUETOOTH
        PERMISSIONS += Manifest.permission.BLUETOOTH_ADMIN
        PERMISSIONS += Manifest.permission.BLUETOOTH_CONNECT
        PERMISSIONS += Manifest.permission.BLUETOOTH_SCAN
        checkPermissions(this.requireContext(), PERMISSIONS)

        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentCurrentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textTitle
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        _devices = arrayListOf()
        _devices.add(Device("this one", "1:1:1:1"))
        _recyclerView = binding.visibleDevicesList
        val devAdapter = DeviceAdapter(_devices)
        _recyclerView.adapter = devAdapter
        _recyclerView.layoutManager = LinearLayoutManager(this.context)

        _bm = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        _bluetooth = _bm.adapter
        // _bluetooth.startDiscovery()
        val filter = IntentFilter(BluetoothDevice.ACTION_FOUND)
        context?.registerReceiver(_receiver, filter)
        return root
    }

    private val _receiver: BroadcastReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            if (BluetoothDevice.ACTION_FOUND == action) {
                val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                _devices?.add(Device(device!!.name, device.address))
                Log.i(
                    "BT", """
                         ${device!!.name}
                         ${device.address}
                         """.trimIndent()
                )
            }
        }
    }

    override fun onDestroyView() {
        context?.unregisterReceiver(_receiver);
        super.onDestroyView()
        _binding = null
    }
}