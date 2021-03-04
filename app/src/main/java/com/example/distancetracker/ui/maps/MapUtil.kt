package com.example.distancetracker.ui.maps

import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.SphericalUtil
import java.text.DecimalFormat

object MapUtil {

    fun setCameraPosition(location:LatLng): CameraPosition {
       return CameraPosition.Builder().target(location).zoom(18f)
            .build()
    }

     fun calculateElapsedTime(startTime:Long,stopTime:Long):String{
         val elapsedTime=stopTime-startTime
         val seconds=(elapsedTime/1000)%60
         val minutes=(elapsedTime/(1000*60))%60
         val hours=(elapsedTime/(1000*60*60))%24
         return "$hours:$minutes:$seconds"
     }

    fun calculateDistance(locationList:MutableList<LatLng>):String{

        if(locationList.size>1){
            val distance= SphericalUtil.computeDistanceBetween(locationList.first(),locationList.last())
            return DecimalFormat("#.##").format(distance/1000)
        }

        return "0.00"

    }




}