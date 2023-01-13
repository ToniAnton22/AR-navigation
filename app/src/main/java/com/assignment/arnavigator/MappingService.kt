package com.assignment.arnavigator

import android.annotation.SuppressLint
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.IBinder
import android.util.Log
import com.assignment.arnavigator.data.LatLon



class MappingService: Service(), LocationListener {
    inner class MappingServiceBinder(val mappingService: MappingService): android.os.Binder()

    lateinit var locationManager: LocationManager

    var currentLocation: LatLon? =null
    // start handler

    override fun onCreate() {
        locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("ServiceStart","Service has started")
        startGps()

        return START_STICKY // we will look at this return value below
    }


    // bind handler - not needed in many cases but defined as an abstract
    // method in Service, therefore must be overridden
    override fun onBind(intent: Intent?): IBinder {
        return MappingServiceBinder(this) // can just return null if binding is not needed
    }

    override fun onLocationChanged(location: Location) {
        currentLocation = LatLon(location.latitude,location.longitude)
        Log.d("locationvalue","${currentLocation}")

        val broadcast = Intent().apply {
            action = "loc"
            putExtra("Lat", currentLocation!!.lat)
            putExtra("Lng", currentLocation!!.lon)
            putExtra("Alt", location.altitude.toFloat())
        }

        sendBroadcast(broadcast)

    }

    fun getGps(): LatLon? {

        return currentLocation
    }


    @SuppressLint("MissingPermission")
    private fun startGps(){
        Log.d("GpsCalled","Service has started")
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0f, this)

    }
}