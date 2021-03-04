package com.example.distancetracker.ui.permission

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.fragment.app.Fragment
import com.example.distancetracker.util.Constants.BACKGROUND_PERMISSION_LOCATION_REQUEST_CODE
import com.example.distancetracker.util.Constants.PERMISSION_LOCATION_REQUEST_CODE
import com.vmadalin.easypermissions.EasyPermissions

object Permissions {

    fun hasLocationPermission(context:Context)=EasyPermissions.hasPermissions(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION
    )
    fun requestLocationPermission(fragment: Fragment)
    {
        EasyPermissions.requestPermissions(fragment,
            "This Application cannot work without Location Permission",
            PERMISSION_LOCATION_REQUEST_CODE,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

    }

    fun hasBackgroundLocationPermission(context: Context):Boolean{
         if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
           return EasyPermissions.hasPermissions(context,Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        return true
    }

    fun requestBackgroundLocationPermission(fragment: Fragment)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            EasyPermissions.requestPermissions(fragment,
                "This Permission is necessary for this app without this permission you are not able to enjoy our services",
                BACKGROUND_PERMISSION_LOCATION_REQUEST_CODE,
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            )
        }

    }

}