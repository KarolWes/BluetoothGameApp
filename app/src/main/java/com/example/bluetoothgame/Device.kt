package com.example.bluetoothgame

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothgame.databinding.DeviceEntryLayoutBinding

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