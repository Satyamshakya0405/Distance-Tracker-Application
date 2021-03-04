package com.example.distancetracker.ui.maps

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Camera
import android.graphics.Color
import androidx.fragment.app.Fragment

import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.distancetracker.ui.maps.MapUtil.setCameraPosition
import com.example.distancetracker.R
import com.example.distancetracker.databinding.FragmentMapsBinding
import com.example.distancetracker.model.Result
import com.example.distancetracker.service.TrackingService
import com.example.distancetracker.ui.maps.MapUtil.calculateDistance
import com.example.distancetracker.ui.maps.MapUtil.calculateElapsedTime
import com.example.distancetracker.util.Constants.ACTION_START_FOREGROUND_SERVICE
import com.example.distancetracker.util.Constants.ACTION_STOP_FOREGROUND_SERVICE
import com.example.distancetracker.util.ExtensionFunctions.disable
import com.example.distancetracker.util.ExtensionFunctions.enable
import com.example.distancetracker.util.ExtensionFunctions.hide
import com.example.distancetracker.util.ExtensionFunctions.show
import com.example.distancetracker.ui.permission.Permissions.hasBackgroundLocationPermission
import com.example.distancetracker.ui.permission.Permissions.requestBackgroundLocationPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.vmadalin.easypermissions.EasyPermissions
import com.vmadalin.easypermissions.dialogs.SettingsDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class MapsFragment : Fragment() ,OnMapReadyCallback,GoogleMap.OnMyLocationButtonClickListener,
    EasyPermissions.PermissionCallbacks,GoogleMap.OnMarkerClickListener{


    private  var _binding:FragmentMapsBinding?=null
    private val binding get()=_binding!!

    lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private lateinit var map:GoogleMap

    val started = MutableLiveData(false)

    private var locationList= mutableListOf<LatLng>()
    private var polylineList= mutableListOf<Polyline>()
    private var markerList = mutableListOf<Marker>()

    private var startTime:Long=0L
    private var stopTime:Long=0L

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding= FragmentMapsBinding.inflate(inflater, container, false)
        binding.lifecycleOwner = this
        binding.tracking = this

        binding.startButton.setOnClickListener {
            onStartButtonClicked()
        }

        binding.stopButton.setOnClickListener {
            onStopButtonClicked()
        }
        binding.resetButton.setOnClickListener {
            onResetButtonClicked()
        }

        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(requireActivity())
        return binding.root
    }






    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val mapFragment = childFragmentManager.findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap?) {
        map=googleMap!!
        map.isMyLocationEnabled=true
        map.setOnMyLocationButtonClickListener(this)
        map.setOnMarkerClickListener(this)
        map.uiSettings.apply {
            isZoomControlsEnabled = false
            isZoomGesturesEnabled = false
            isRotateGesturesEnabled = false
            isTiltGesturesEnabled = false
            isCompassEnabled = false
            isScrollGesturesEnabled = false
        }
        observeTrackerService()

    }
    private fun observeTrackerService()
    {
        TrackingService.locationList.observe(viewLifecycleOwner,{
            if(it!=null)
            {

                locationList=it
                drawPolyline()
                followPolyline()
                if(locationList.size>1)
                {
                    binding.stopButton.enable()
                }
            }

        })

        TrackingService.started.observe(viewLifecycleOwner,{
              started.value=it

        })

        TrackingService.startTime.observe(viewLifecycleOwner,{
            startTime=it
        })
        TrackingService.stopTime.observe(viewLifecycleOwner,{
            stopTime=it
            if (stopTime != 0L) {
                showBiggerPicture()
                displayResults()
            }

        })
    }



    private fun drawPolyline()
        {
            val polyline=map.addPolyline(
                PolylineOptions().apply {
                    color(Color.BLUE)
                    width(10f)
                    jointType(JointType.ROUND)
                    startCap(ButtCap())
                    endCap(ButtCap())
                    addAll(locationList)
                }
            )
            polylineList.add(polyline)
        }
    private fun followPolyline()
    {
        if(locationList.isNotEmpty())
        {
            map.animateCamera(CameraUpdateFactory.newCameraPosition(
                setCameraPosition(locationList.last())
            ),1000,null)
        }
    }

    private fun onStartButtonClicked() {
        if(hasBackgroundLocationPermission(requireContext()))
        {
                startCountDown()
            binding.startButton.disable()
            binding.startButton.hide()
            binding.stopButton.show()
        }
        else
        {
            requestBackgroundLocationPermission(this)
        }
    }
    private fun onStopButtonClicked() {
        stopForegroundService()
    }

    private fun onResetButtonClicked() {
        mapReset()
    }

    private fun startCountDown() {
        binding.timerTextView.show()
        binding.stopButton.disable()
        val timer:CountDownTimer=object:CountDownTimer(4000,1000)
        {
            override fun onTick(millisUntilFinished: Long) {
                val currentSecond = millisUntilFinished / 1000
                if(currentSecond.toString()=="0")
                {
                    binding.timerTextView.text="GO"
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.black
                    ))
                }
                else
                {
                    binding.timerTextView.text = currentSecond.toString()
                    binding.timerTextView.setTextColor(ContextCompat.getColor(requireContext(),
                        R.color.red
                    ))
                }
            }

            override fun onFinish() {
                binding.timerTextView.hide()
                    sendActionCommandToService(ACTION_START_FOREGROUND_SERVICE)
            }
        }
        timer.start()
    }

    private fun stopForegroundService() {
        binding.startButton.disable()
        binding.stopButton.hide()
        binding.startButton.show()
        sendActionCommandToService(ACTION_STOP_FOREGROUND_SERVICE)
    }
    private fun sendActionCommandToService(action:String) {

        Intent(requireContext(),TrackingService::class.java).apply {
            this.action=action
            requireContext().startService(this)
        }
    }


    private fun showBiggerPicture() {
        val bounds = LatLngBounds.Builder()
        for (location in locationList) {
            bounds.include(location)
        }
        map.animateCamera(
            CameraUpdateFactory.newLatLngBounds(
                bounds.build(), 100
            ), 2000, null
        )
        addMarker(locationList.first())
        addMarker(locationList.last())
    }
    private fun addMarker(position: LatLng){
        val marker = map.addMarker(MarkerOptions().position(position))
        markerList.add(marker)
    }
    private fun displayResults() {
    val result=Result(
        calculateDistance(locationList),
        calculateElapsedTime(startTime,stopTime))

        lifecycleScope.launch {
            delay(2500)
            val directions=MapsFragmentDirections.actionMapsFragmentToResultsFragment(result)
            findNavController().navigate(directions)
           binding.startButton.apply {
               hide()
               enable()
           }
            binding.stopButton.hide()
            binding.resetButton.show()
        }

    }
    @SuppressLint("MissingPermission")
    private fun mapReset() {

        fusedLocationProviderClient.lastLocation.addOnCompleteListener {
            val lastKnownLocation=LatLng(it.result.latitude,it.result.longitude)
            map.animateCamera(
                CameraUpdateFactory.newCameraPosition(
                    setCameraPosition(lastKnownLocation)
                )
            )
            for(polyline in polylineList)
            {
                polyline.remove()
            }
            for (marker in markerList){
                marker.remove()
            }

            locationList.clear()
            markerList.clear()
            binding.resetButton.hide()
            binding.startButton.show()
        }

    }

//



    override fun onMyLocationButtonClick(): Boolean {
        binding.hintTextView.animate().alpha(0f).duration=1500
        lifecycleScope.launch {
            delay(2500)
            binding.hintTextView.hide()
            binding.startButton.show()
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        EasyPermissions.onRequestPermissionsResult(requestCode,permissions,grantResults,this)
    }
    override fun onPermissionsDenied(requestCode: Int, perms: List<String>) {
        if(EasyPermissions.somePermissionPermanentlyDenied(this,perms[0]))
        {
            SettingsDialog.Builder(requireActivity()).build().show()
        }
        else
        {
            requestBackgroundLocationPermission(this)
        }
    }

    override fun onPermissionsGranted(requestCode: Int, perms: List<String>) {
        onStartButtonClicked()
    }


    override fun onDestroyView() {
        super.onDestroyView()
        _binding=null
    }

    override fun onMarkerClick(p0: Marker?): Boolean {
        return true
    }
}