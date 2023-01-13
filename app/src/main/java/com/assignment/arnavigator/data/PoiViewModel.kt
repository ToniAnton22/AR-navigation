package com.assignment.arnavigator.data

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PoiViewModel(app:Application): AndroidViewModel(app) {

    val readAllPois: LiveData<List<Poi>>
    private val repository:  PoiRepository

    init{
        val db = PoiDataBase.getDatabase(app).poiDao()
        repository  = PoiRepository(db)
        readAllPois = repository.readAllPois

    }

    fun getAllPois(): LiveData<List<Poi>>{
        return readAllPois
    }
    fun addPoi(poi: Poi){
        viewModelScope.launch(Dispatchers.IO){
            repository.addPoi(poi)
        }
    }

    fun deleteTables(){
        viewModelScope.launch(Dispatchers.IO){
            repository.deleteTables()
        }
    }
    fun getByType(type: String): LiveData<List<Poi>> {

        return repository.getByTypes(type)
    }
}