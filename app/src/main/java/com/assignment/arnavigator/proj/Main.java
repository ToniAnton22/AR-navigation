package com.assignment.arnavigator.proj;

public class Main {
    //Uncomment to test

    public static void main(String[] args) {
        // Create a Spherical Mercator projection.
        SphericalMercatorProjection proj = new SphericalMercatorProjection();

        // Project a LonLat into eastings and northings
        LonLat p = new LonLat(-1.40135, 50.90778);
        EastNorth en = proj.project(p);
        System.out.println("Easting: " + en.easting + " Northing: " + en.northing);

        // Unproject an EastNorth into longitude and latitude
        EastNorth en2 = new EastNorth(75000, 25000);
        LonLat p2 = proj.unproject(en2);
        System.out.println("Longitude: " + p2.lon + " Latitude: " + p2.lat);
    }

}
