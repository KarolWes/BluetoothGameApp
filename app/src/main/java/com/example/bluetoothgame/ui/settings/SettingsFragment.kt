package com.example.bluetoothgame.ui.settings

import android.annotation.SuppressLint
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.bluetoothgame.DBInternal
import com.example.bluetoothgame.R
import com.example.bluetoothgame.databinding.FragmentSettingsBinding


class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private lateinit var _db: DBInternal
    private var _defaultRef = 30
    private var _defaultPaired = 0
    private var _defaultNNa = 1
    private var _defaultUser = ""
    private var _defaultEmail = ""
    private var _id = ""

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    @SuppressLint("UseSwitchCompatOrMaterialCode", "SetTextI18n")
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        val settingsViewModel =
            ViewModelProvider(this)[SettingsViewModel::class.java]
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        val root: View = binding.root
        val textView: TextView = binding.settingTitleText
        settingsViewModel.text.observe(viewLifecycleOwner) {
            textView.text = it
        }
        _db = DBInternal(this.requireContext(), null, null, 1)
        val data = _db.getVals()
        if (data == null){
            _db.setDefault(_defaultRef, _defaultPaired, _defaultNNa)
        }
        else{
            _id = data[0]
            _defaultUser = data[1]
            _defaultEmail = data[2]
            _defaultRef = Integer.parseInt(data[5])
            _defaultPaired = Integer.parseInt(data[6])
            _defaultNNa = Integer.parseInt(data[7])
        }
        var token = _db.getToken()
        if(token == ""){
            // Log in
            val nav = findNavController()
            nav.navigate(R.id.naviagtion_login)
        }
        val pairedSw = binding.pairedSwitch
        pairedSw.isChecked = _defaultPaired == 1
        pairedSw.setOnClickListener { _defaultPaired = _defaultPaired xor 1 }
        val nnaSw = binding.unnamedSwitch
        nnaSw.isChecked = _defaultNNa == 1
        nnaSw.setOnClickListener { _defaultNNa = _defaultNNa xor 1 }
        val refText = binding.refRateText
        refText.text = "${getString(R.string.ref_rate)} $_defaultRef seconds"
        val refBar = binding.refRateScroll
        refBar.progress = (_defaultRef-20)/5
        refBar.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, progress: Int, b: Boolean) {
                _defaultRef = progress*5+20
                refText.text = "${getString(R.string.ref_rate)} $_defaultRef seconds"
            }
            override fun onStartTrackingTouch(seekBar: SeekBar) {}
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
        })
        val userText = binding.usernameText
        userText.text = _defaultUser
        val emailText = binding.emailText
        emailText.text = _defaultEmail
        val logOutButton = binding.logoutButton
        logOutButton.setOnClickListener{
            _db.logOut(_id)
            val nav = findNavController()
            nav.navigate(R.id.naviagtion_login)
        }

        return root
    }

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onDestroyView() {
        super.onDestroyView()
        _db.updateParameters(_id, arrayListOf(_defaultRef, _defaultPaired, _defaultNNa))
        _binding = null
    }
}