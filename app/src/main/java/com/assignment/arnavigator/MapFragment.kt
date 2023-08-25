package com.assignment.arnavigator


import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.RelativeLayout
import androidx.annotation.RequiresApi
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import com.assignment.arnavigator.data.ClosestPoi
import com.assignment.arnavigator.data.LatLon
import com.assignment.arnavigator.data.PoiViewModel
import com.assignment.arnavigator.data.ViewModels
import com.assignment.arnavigator.databinding.MapActivityBinding
import org.osmdroid.config.Configuration
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.ItemizedIconOverlay
import org.osmdroid.views.overlay.OverlayItem



class MapFragment: Fragment(R.layout.map_activity) {
    private lateinit var binding: MapActivityBinding
    private lateinit var moving: Moving
    private lateinit var map1: MapView
    private lateinit var items : ItemizedIconOverlay<OverlayItem>
    val viewModels: ViewModels by activityViewModels()
    val poiViewModel: PoiViewModel by activityViewModels()
    var currentLatLon = LatLon(0.0,0.0)
    var closestDistance =  ClosestPoi(0.0,0,"")

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = MapActivityBinding.inflate(inflater,container, false)
        binding.root
        return binding.root
    }
    @RequiresApi(Build.VERSION_CODES.N)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {

        Configuration.getInstance().load(activity, PreferenceManager.getDefaultSharedPreferences(activity))
        moving = Moving()
        map1 = binding.map1
        binding.progress.progress = 25
        items = ItemizedIconOverlay(activity, arrayListOf<OverlayItem>(),null)
        binding.getLocation.setOnClickListener {
            getLocation()
        }

    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onStart() {
        super.onStart()

        map1.controller.setZoom(14.0)
        binding.progress.progress = 50
        getLocation()

    }

    @RequiresApi(Build.VERSION_CODES.N)
    fun getLocation(){
        var prevLocation = LatLon(0.0,0.0)
        viewModels.getCurrentLocation().observe(viewLifecycleOwner) {
            Log.d("AdapterlatLon", "$it")
            currentLatLon = it

            if (currentLatLon.lat != 0.0) {
                binding.progress.progress = 100
                map1.controller.setCenter(GeoPoint(prevLocation.lat,prevLocation.lon))
                prevLocation = currentLatLon
                binding.progress.isVisible = false
            }

        }
        if(currentLatLon.lat == 0.0){

            map1.controller.setCenter(GeoPoint(prevLocation.lat,prevLocation.lon))
        }else {
            map1.controller.setCenter(GeoPoint(currentLatLon.lat, currentLatLon.lon))
        }
    }

    fun markAll(){
        Log.d("I am called","Mark is called")
        if(items.size() != 0){
            deleteAllMarkers()
        }
        poiViewModel.readAllPois.observe(viewLifecycleOwner){
            if(it.isNotEmpty()){
                var currentDistance = 0.0
                var prevDistance = arrayOf(0.0)
                var itTracker = arrayOf(0)
                var nameTracker = arrayOf("")

                it.forEach{
                    val poi = OverlayItem(it.name,it.locationType,GeoPoint(it.lat.toDouble(),
                        it.lon.toDouble()))

                    items.addItem(poi)


                    currentDistance=getClosestDistance(it.lat.toDouble(),it.lon.toDouble()
                        ,viewModels.currentLatLon.lat,viewModels.currentLatLon.lon)

                    prevDistance = prevDistance.plus(currentDistance)
                    itTracker = itTracker.plus(it.osm_id.toInt())
                    nameTracker = nameTracker.plus(it.name)
                }
                for(i in 0 until prevDistance.size){
                    if(currentDistance > prevDistance[i]){
                        Log.d("LOGIC CHECK","$currentDistance and ${prevDistance[i]}")
                        closestDistance = ClosestPoi(prevDistance[i],itTracker[i].toLong(),nameTracker[i])
                        viewModels.changeClosestLocation(closestDistance)

                    }
                }
                if(closestDistance.distance < 10){
                    (activity as UiActivity).getNotification(closestDistance)
                }
            }
        }
        map1.overlays.add(items)
    }

    fun markByType(type: String){
        Log.d("Items size","${items.size()}")
        if(items.size() != 0){
            deleteAllMarkers()
        }
        poiViewModel.getByType(type).observe(viewLifecycleOwner){
            if(it.isNotEmpty()){
                var currentDistance = 0.0
                var prevDistance = arrayOf(0.0)
                var itTracker = arrayOf(0)
                var nameTracker = arrayOf("")

                it.forEach{
                    if(it.locationType == type){
                        val poi = OverlayItem(it.name,it.locationType,GeoPoint(it.lat.toDouble(),it.lon.toDouble()))

                        items.addItem(poi)


                        currentDistance=getClosestDistance(currentLatLon.lat,currentLatLon.lon
                            ,it.lat.toDouble(),it.lon.toDouble())

                        prevDistance = prevDistance.plus(currentDistance)
                        itTracker = itTracker.plus(it.osm_id.toInt())
                        nameTracker = nameTracker.plus(it.name)
                    }
                }
                for(i in prevDistance){
                    Log.d("LOGIC CHECK"," and $i")
                }
                for(i in prevDistance.indices){
                    if(currentDistance > prevDistance[i]){
                        Log.d("LOGIC CHECK","$currentDistance and ${prevDistance[i]}")
                        currentDistance = prevDistance[i]
                        closestDistance = ClosestPoi(prevDistance[i],itTracker[i].toLong(),nameTracker[i])
                        viewModels.changeClosestLocation(closestDistance)
                    }
                }
                if(closestDistance.distance/1000 < 1000){
                    Log.d("CLOSEST", "${closestDistance.distance/1000} + ${closestDistance.name}")
                    (activity as UiActivity).getNotification(closestDistance)
                }


            }
        }
    }

    private fun getClosestDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        return moving.calulateDistance(lon1, lat1, lon2, lat2)
    }

    fun deleteAllMarkers(){
        Log.d("Removed","Removed has been called")
        items.removeAllItems()
    }


}