package com.assignment.arnavigator.data

import android.util.Log
import androidx.lifecycle.LiveData
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PoiRepository(private val poiDao: PoiDao) {

    val readAllPois: LiveData<List<Poi>> = poiDao.getAllPois()
    lateinit var readTypePoi: LiveData<List<Poi>>

    suspend fun addPoi(poi: Poi){
        try {
            withContext(Dispatchers.IO){
                poiDao.insert(poi)
            }

        }catch (e: Exception){
            Log.e("AddException",e.stackTraceToString())
        }
    }
    suspend fun removePoi(id:Long){
        try {
            withContext(Dispatchers.IO){
                poiDao.delete(id)
            }

        }catch (e: Exception){
            Log.e("Remove1Exception",e.stackTraceToString())
        }
    }
    suspend fun deleteTables(){
        try {
            withContext(Dispatchers.IO){
                poiDao.delete()
            }
        }catch (e: Exception){
            Log.e("RemoveAllException",e.stackTraceToString())
        }

    }
   fun getByTypes(type:String): LiveData<List<Poi>>{
        try {

            readTypePoi = poiDao.getPoiByType(type)


        }catch (e: Exception){
            Log.e("GetTypeException",e.stackTraceToString())
        }
        return readTypePoi
    }
}