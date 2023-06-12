package com.example.bluetoothgame

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Build
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.annotation.RequiresApi
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.bluetoothgame.databinding.DeviceEntryLayoutBinding
import com.example.bluetoothgame.ui.home.HomeFragment
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST

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
    interface RestApiRegisterDevice{
        @Headers("Content-Type: application/json")
        @POST("devices/register/")
        fun registerDevice(
            @Header("Authorization") token:String,
            @Body dev: Map<String, String>): Call<ResponseBody>

    }


    private lateinit var _binding: DeviceEntryLayoutBinding
    private lateinit var _myDevicesDB: DBOwnedDevices
    private lateinit var _internalDB: DBInternal
    private var _myDevicesList: ArrayList<Device> = arrayListOf()
    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.most-seen-person.rmst.eu/api/v1/")
        .addConverterFactory(GsonConverterFactory.create())
        .build()
    inner class ViewHolder(entryView: View, context: Context) : RecyclerView.ViewHolder(entryView) {
        val nameTextView = _binding.deviceTagText
        val addressTextView = _binding.deviceMacText
        val messageButton = _binding.markOwnButton
        val row = _binding.deviceEntry
        val context = context
    }


    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        _binding = DeviceEntryLayoutBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        _myDevicesDB = DBOwnedDevices(parent.context, null, null, 1)
        _internalDB = DBInternal(parent.context, null, null, 1)
        _myDevicesList = _myDevicesDB.getAll(HomeFragment.userId)
        val deviceView = _binding.root
        return ViewHolder(deviceView, parent.context)
    }

    fun markOwn(v: View?, position: Int, button: Button) {
        if(HomeFragment.connected){
            button.backgroundTintList= ColorStateList.valueOf(Color.parseColor("#03AC13"))
            if (v != null) {
                val dev: Device = devices[position]
                if(dev.my){
                    _myDevicesDB.remove(dev.address)
                }
                else{
                    _myDevicesDB.put(HomeFragment.userId, dev)
                    GlobalScope.launch { registerDevice(dev, button) }
                }
                dev.my = dev.my xor true
                button.text = if (!dev.my) "Mark as my" else "Mark as not my"
            }
        }
        else{
            button.text = "No internet"
            button.backgroundTintList= ColorStateList.valueOf(Color.parseColor("#990000"))
        }

    }

    // Involves populating data into the item through holder
    override fun onBindViewHolder(viewHolder: DeviceAdapter.ViewHolder, position: Int) {
        val dev: Device = devices[position]
        dev.my = dev in _myDevicesList
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
        if(!dev.my){
            button.text = "Mark as my"
            button.setOnClickListener{
                markOwn(it, position, button)
            }
        }
        else{
            button.text = "My device"
            button.backgroundTintList= ColorStateList.valueOf(Color.parseColor("#A9A9A9"))
            button.isClickable = false
            button.isEnabled = false
        }
    }

    // Returns the total count of items in the list
    override fun getItemCount(): Int {
        return devices.size
    }

    suspend fun registerDevice(dev:Device, button: Button){
        val apiService = retrofit.create(RestApiRegisterDevice::class.java)
        val body = mapOf(
            "mac" to dev.address,
            "name" to dev.name
        )
        apiService.registerDevice("Token ${HomeFragment.token}", body).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                if (response.code().toString()[0] != '2') {
                    Log.i("http", "Error: ${response.code()}")
                    Log.i("http", "${response.errorBody()?.string()}")
                    button.text = "Someone else"
                    button.backgroundTintList= ColorStateList.valueOf(Color.parseColor("#A9A9A9"))
                    button.isClickable = false
                    button.isEnabled = false
                }
                else{
                    response.body()?.string()?.let { Log.i("http", it) }
                }

            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.i("http", "Error")
            }
        })
        return
    }
}