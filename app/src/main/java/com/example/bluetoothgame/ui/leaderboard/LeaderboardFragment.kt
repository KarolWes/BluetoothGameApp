package com.example.bluetoothgame.ui.leaderboard

import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.example.bluetoothgame.DBInternal
import com.example.bluetoothgame.R
import com.example.bluetoothgame.databinding.FragmentLeaderboardBinding

class LeaderboardFragment : Fragment() {

    private var _binding: FragmentLeaderboardBinding? = null
    private lateinit var _db: DBInternal

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
        if(token == ""){
            // Log in
            val nav = findNavController()
            nav.navigate(R.id.naviagtion_login)
        }
        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}