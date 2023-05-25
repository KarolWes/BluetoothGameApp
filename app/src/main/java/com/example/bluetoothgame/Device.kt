package com.example.bluetoothgame

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothgame.databinding.DeviceEntryLayoutBinding
import kotlin.reflect.typeOf

class Device(val name: String, val address:String, var my: Boolean =false) {

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

    override fun toString(): String {
        return "$name $address"
    }

    override fun equals(other: Any?): Boolean {
        return if(other is Device){
            other.address== address
        } else{
            super.equals(other)
        }
    }

    override fun hashCode(): Int {
        return address.hashCode()
    }
}
class DeviceAdapter(private val devices: List<Device>): RecyclerView.Adapter<DeviceAdapter.ViewHolder>(){
    private lateinit var _binding: DeviceEntryLayoutBinding
    inner class ViewHolder(entryView: View, context: Context) : RecyclerView.ViewHolder(entryView) {
        val nameTextView = _binding.deviceTagText
        val addressTextView = _binding.deviceMacText
        val messageButton = _binding.markOwnButton
        val row = _binding.deviceEntry
        val context = context
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        _binding = DeviceEntryLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val deviceView = _binding.root
        return ViewHolder(deviceView, parent.context)
    }

    fun markOwn(v: View?, position: Int, button: Button) {
        if (v != null) {
            val dev: Device = devices[position]
            dev.my = dev.my xor true
            button.text = if (!dev.my) "Mark as my" else "Mark as not my"
        }
    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(viewHolder: DeviceAdapter.ViewHolder, position: Int) {
        val dev: Device = devices[position]
        if(position % 2 == 1){
            viewHolder.row.setBackgroundColor(
                ContextCompat.getColor(viewHolder.context, R.color.beige_800))
        }else{
            viewHolder.row.setBackgroundColor(
                ContextCompat.getColor(viewHolder.context, R.color.beige_200))
        }
        val nameText = viewHolder.nameTextView
        nameText.text = dev.name
        val addressText = viewHolder.addressTextView
        addressText.text = dev.address
        val button = viewHolder.messageButton
        button.tag = position
        button.text = if (!dev.my) "Mark as my" else "Mark as not my"
        button.setOnClickListener{
            markOwn(it, position, button)
        }
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return devices.size
    }
}