package com.assignment.arnavigator.proj;

public class Algorithms {
   public static double haversineDist(double lon1, double lat1, double lon2, double lat2)

   {
       double R = 6371000;
       double dlon=(lon2-lon1)*(Math.PI / 180);
       double dlat=(lat2-lat1)*(Math.PI / 180);
       double slat=Math.sin(dlat/2);
       double slon=Math.sin(dlon/2);
       double a = slat*slat + Math.cos(lat1*(Math.PI/180))*Math.cos(lat2*(Math.PI/180))*slon*slon;
       double c = 2 *Math.asin(Math.min(1,Math.sqrt(a)));
       return R*c;
   }
}

