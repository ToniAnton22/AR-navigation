package com.assignment.arnavigator.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase


@Database(entities = arrayOf(Poi::class), version = 1, exportSchema = false)


abstract  class PoiDataBase: RoomDatabase() {
    abstract fun poiDao() : PoiDao

    companion object{
        private var instance: PoiDataBase? = null

        fun getDatabase(ctx: Context) : PoiDataBase {

            var tmpInstance = instance
            if(tmpInstance == null){
                tmpInstance = Room.databaseBuilder(
                    ctx.applicationContext,
                    PoiDataBase::class.java,
                "poiDatabase"
                ).build()
                instance = tmpInstance
            }

            return tmpInstance
        }
    }
}