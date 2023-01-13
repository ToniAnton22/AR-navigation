package com.assignment.arnavigator.proj;

import androidx.annotation.NonNull;

public class EastNorth {
    public double easting,northing;
    
    public EastNorth(double easting, double northing)
    {
        this.easting=easting;
        this.northing=northing;
    }
    
    @NonNull
    public String toString()
    {
        return "easting= "+easting+ " northing="+northing;
    }
}    
