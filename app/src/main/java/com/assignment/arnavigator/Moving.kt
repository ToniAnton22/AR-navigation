package com.assignment.arnavigator



import com.assignment.arnavigator.proj.Algorithms
import com.assignment.arnavigator.proj.EastNorth
import com.assignment.arnavigator.proj.LonLat
import com.assignment.arnavigator.proj.SphericalMercatorProjection


class Moving {
    var coord = floatArrayOf(0f,0f)
    val sph= SphericalMercatorProjection()

    private lateinit var eastNorth: EastNorth


    fun convert(lat:Double, lon:Double): FloatArray{
        val lonLat = LonLat(lon, lat)
        eastNorth = sph.project(lonLat)
        coord[0] = eastNorth.northing.toFloat()
        coord[1] = eastNorth.easting.toFloat()

        return coord
    }

    fun calulateDistance(lat1:Double, lon1:Double, lat2:Double, lon2:Double):Double{
        val distance = Algorithms.haversineDist(lon1,lat1,lon2,lat2)
        return distance
    }



}