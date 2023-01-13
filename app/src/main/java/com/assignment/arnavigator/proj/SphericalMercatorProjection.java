package com.assignment.arnavigator.proj;


public class SphericalMercatorProjection {
    
    public static final double EARTH = 40075016.68, HALF_EARTH = 20037508.34;
    private static final double originE = -155997.56840143888, originN=6604997.254756545;

    public EastNorth project (LonLat lonLat)
    {
        return new EastNorth(lonToSphericalMercator(lonLat.lon)-originE, latToSphericalMercator(lonLat.lat)-originN);
    }
    
    public LonLat unproject (EastNorth projected)
    {
        return new LonLat(sphmercToLon(projected.easting+originE),sphmercToLat(projected.northing+originN));
    }
    
    private double lonToSphericalMercator(double lon)
    {
        return (lon/180) * HALF_EARTH;
    }
    
    private double latToSphericalMercator(double lat)
    {
        double y = Math.log(Math.tan((90+lat)*Math.PI/360)) / (Math.PI/180);
        return y*HALF_EARTH/180;
    }
    
    private double sphmercToLon(double x)
    {
            return (x/HALF_EARTH) * 180.0;
    }
    
    private double sphmercToLat(double y)
    {
        double lat = (y/HALF_EARTH) * 180.0;
        lat = 180/Math.PI * (2*Math.atan(Math.exp(lat*Math.PI/180)) - Math.PI/2);
        return lat;
    }
    
    public String getID()
    {
        return "epsg:3857";
    }
}
