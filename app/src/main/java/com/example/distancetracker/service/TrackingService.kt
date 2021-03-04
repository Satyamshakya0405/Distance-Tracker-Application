package com.example.distancetracker.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.NotificationManager.IMPORTANCE_HIGH
import android.app.NotificationManager.IMPORTANCE_LOW
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Build
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.MutableLiveData
import com.example.distancetracker.ui.maps.MapUtil
import com.example.distancetracker.ui.maps.MapUtil.calculateDistance
import com.example.distancetracker.util.Constants.ACTION_START_FOREGROUND_SERVICE
import com.example.distancetracker.util.Constants.ACTION_STOP_FOREGROUND_SERVICE
import com.example.distancetracker.util.Constants.LOCATION_FASTEST_UPDATE_INTERVAL
import com.example.distancetracker.util.Constants.LOCATION_UPDATE_INTERVAL
import com.example.distancetracker.util.Constants.NOTIFICATION_CHANNEL_ID
import com.example.distancetracker.util.Constants.NOTIFICATION_CHANNEL_NAME
import com.example.distancetracker.util.Constants.NOTIFICATION_ID
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.LatLng
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class TrackingService:LifecycleService() {

    @Inject
    lateinit var notificationManager:NotificationManager
    @Inject
    lateinit var notification:NotificationCompat.Builder

    lateinit var fusedLocationProviderClient:FusedLocationProviderClient

    companion object{
        val started=MutableLiveData<Boolean>()
        val locationList=MutableLiveData<MutableList<LatLng>>()
        val startTime=MutableLiveData<Long>()
        val stopTime=MutableLiveData<Long>()
    }


    private fun initValues()
    {
        started.postValue(false)
        locationList.postValue(mutableListOf())
        startTime.postValue(0L)
        stopTime.postValue(0L)
    }
private val locationCallback=object: LocationCallback(){
    override fun onLocationResult(result: LocationResult) {
        super.onLocationResult(result)
        val locations=result.locations
        for(location in locations)
        {
            updateLocationList(location)
            updateNotificationPeriodically()

        }
    }
}



    private fun updateLocationList(location: Location) {
        val latLng=LatLng(location.latitude,location.longitude)
        locationList.value?.apply {
            add(latLng)
            locationList.postValue(this)
        }

    }
    private fun updateNotificationPeriodically() {
        notification.setContentTitle("Distance Travelled").
                setContentText(locationList.value?.let { calculateDistance(it) }+"km")
        notificationManager.notify(NOTIFICATION_ID,notification.build())
    }

    override fun onCreate() {
        super.onCreate()
        initValues()
        fusedLocationProviderClient=LocationServices.getFusedLocationProviderClient(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        intent?.let {
            when(it.action){
                ACTION_START_FOREGROUND_SERVICE->{
                    started.postValue(true)
                    startForegroundService()
                    startLocationUpdates()
                }
                ACTION_STOP_FOREGROUND_SERVICE->{
                    started.postValue(false)
                    stopForegroundService()

                }
                else->{}
            }

        }
        return super.onStartCommand(intent, flags, startId)
    }


    private fun startForegroundService(){

            createNotificationChannel()
            startForeground(NOTIFICATION_ID,notification.build())

    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {

        val locationRequest=LocationRequest.create().apply {
            interval= LOCATION_UPDATE_INTERVAL
            fastestInterval= LOCATION_FASTEST_UPDATE_INTERVAL
            priority= LocationRequest.PRIORITY_HIGH_ACCURACY
        }
        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()

        )
        startTime.postValue(System.currentTimeMillis())
    }
    private fun stopForegroundService() {
        removeLocationUpdates()
        (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).cancel(
            NOTIFICATION_ID
        )
        stopForeground(true)
        stopSelf()
        stopTime.postValue(System.currentTimeMillis())
    }

    private fun removeLocationUpdates() {
        fusedLocationProviderClient.removeLocationUpdates(locationCallback)
    }

    private fun createNotificationChannel() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.O)
        {
            val channel=NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }
    }

}