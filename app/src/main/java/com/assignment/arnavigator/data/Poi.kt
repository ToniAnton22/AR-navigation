package com.assignment.arnavigator.data


import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName="pois")



data class Poi(

    @PrimaryKey(autoGenerate = true) val id: Long,
    @ColumnInfo(name="latitude") val lat: Float,
    @ColumnInfo(name="longitude") val lon: Float,
    @ColumnInfo(name="osm_id") val osm_id: Long,
    @ColumnInfo(name="name") val name: String,
    @ColumnInfo(name="locationType") val locationType:String,


    )
