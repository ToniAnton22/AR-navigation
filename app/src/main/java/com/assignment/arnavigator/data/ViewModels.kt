package com.assignment.arnavigator.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

data class LatLon(var lat:Double =0.0, var lon: Double= 0.0)
data class ClosestPoi(val distance:Double, val osm_id: Long,val name:String)

class ViewModels: ViewModel() {

    var currentLatLon = LatLon()
        set(newPosition){
            field = newPosition
            latLonLive.value =  newPosition
        }


    private var latLonLive = MutableLiveData<LatLon>()
    private var closestLocation = MutableLiveData<ClosestPoi>()

    fun getCurrentLocation():LiveData<LatLon>{
        return latLonLive
    }
    fun changeClosestLocation(poi: ClosestPoi){
        closestLocation.value = poi
    }

    fun getClosestLocation():LiveData<ClosestPoi>{
        return closestLocation
    }

}