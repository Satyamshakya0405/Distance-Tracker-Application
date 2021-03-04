package com.example.distancetracker.ui.result

import android.content.Intent
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.navArgs
import com.example.distancetracker.R
import com.example.distancetracker.databinding.FragmentResultsBinding
import com.google.android.material.bottomsheet.BottomSheetDialogFragment


class ResultsFragment : BottomSheetDialogFragment() {

    val args:ResultsFragmentArgs by navArgs()
   private var _binding:FragmentResultsBinding?=null
    private val binding get()=_binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding= FragmentResultsBinding.inflate(inflater, container, false)

        binding.distanceValueTextView.text=getString(R.string.result,args.result.distance)
        binding.timeValueTextView.text=args.result.time

        binding.shareButton.setOnClickListener {
            shareResult()
        }

        return binding.root
    }

    private fun shareResult() {

        val shareIntent= Intent().apply {
            action=Intent.ACTION_SEND
            type="text/plain"
            putExtra(Intent.EXTRA_TEXT,"I went ${args.result.distance}km in ${args.result.time}!")
        }
        startActivity(shareIntent)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }

}