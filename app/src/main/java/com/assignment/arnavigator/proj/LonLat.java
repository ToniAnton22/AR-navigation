package com.assignment.arnavigator.proj;

import androidx.annotation.NonNull;

public class LonLat {
    public double lon,lat;
    
    public LonLat(double lon, double lat)
    {
        this.lon=lon;
        this.lat=lat;
    }
    
    @NonNull
    public String toString()
    {
        return "lon= "+lon+ " lat="+lat;
    }
}    
