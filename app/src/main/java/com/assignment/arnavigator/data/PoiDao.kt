package com.assignment.arnavigator.data

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface PoiDao {
    @Query("SELECT * FROM pois WHERE id=:id")
    fun getPoiById(id: Long): Poi?

    @Query("SELECT * FROM pois WHERE locationType=:locationType")
    fun getPoiByType(locationType: String): LiveData<List<Poi>>

    @Query("SELECT * FROM pois")
    fun getAllPois(): LiveData<List<Poi>>

    @Insert
    fun insert(poi: Poi) : Long

    @Update
    fun update(poi: Poi) : Int

    @Query("DELETE FROM pois WHERE osm_id=:osm_id")
    fun delete(osm_id: Long) : Int

    @Query("DELETE FROM pois")
    fun delete()

}