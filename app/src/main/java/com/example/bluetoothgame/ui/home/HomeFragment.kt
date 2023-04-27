package com.example.bluetoothgame.ui.home

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothgame.databinding.DeviceEntryLayoutBinding
import com.example.bluetoothgame.databinding.FragmentCurrentBinding
import org.w3c.dom.Text

class Device(val name: String, val address:String, val my: Boolean =false) {

    companion object {
        private var lastContactId = 0
        fun createContactsList(numContacts: Int): ArrayList<Device> {
            val devicesList = ArrayList<Device>()
            for (i in 1..numContacts) {
                devicesList.add(Device("Person " + ++lastContactId, "0:0:0:0", i <= numContacts / 2))
            }
            return devicesList
        }
    }
}
class DeviceAdapter(private val devices: List<Device>): RecyclerView.Adapter<DeviceAdapter.ViewHolder>(){
    private lateinit var _binding: DeviceEntryLayoutBinding
    // Provide a direct reference to each of the views within a data item
    // Used to cache the views within the item layout for fast access
    inner class ViewHolder(entryView: View) : RecyclerView.ViewHolder(entryView) {
        // Your holder should contain and initialize a member variable
        // for any view that will be set as you render a row
        val nameTextView = _binding.deviceTagText
        val messageButton = _binding.markOwnButton
    }

    // ... constructor and member variables
    // Usually involves inflating a layout from XML and returning the holder
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        _binding = DeviceEntryLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val deviceView = _binding.root
        return ViewHolder(deviceView)
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(viewHolder: DeviceAdapter.ViewHolder, position: Int) {
        // Get the data model based on position
        val contact: Device = devices[position]
        // Set item views based on your views and data model
        val textView = viewHolder.nameTextView
        textView.text = contact.name
        val button = viewHolder.messageButton
        button.text = if (contact.my) "Mark as my" else "Mark as not my"
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return devices.size
    }
}

class HomeFragment : Fragment() {

    private var _binding: FragmentCurrentBinding? = null
    private var _devices: ArrayList<Device>? = null
    private lateinit var _bluetooth: BluetoothAdapter
    private lateinit var _bm: BluetoothManager
    private lateinit var _recyclerView: RecyclerView

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    fun getNearbyDevices(){

    }

    @SuppressLint("MissingPermission")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val homeViewModel =
            ViewModelProvider(this)[HomeViewModel::class.java]

        _binding = FragmentCurrentBinding.inflate(inflater, container, false)
        val root: View = binding.root

        val textView: TextView = binding.textTitle
        homeViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        _recyclerView = binding.visibleDevicesList
        var devAdapter = _devices?.let { DeviceAdapter(it) }
        _recyclerView.adapter = devAdapter
        _recyclerView.layoutManager = LinearLayoutManager(this.context)

        _bm = context?.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        _bluetooth = _bm.adapter
        _bluetooth.startDiscovery()
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