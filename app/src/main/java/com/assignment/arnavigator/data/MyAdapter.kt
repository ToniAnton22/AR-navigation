package com.assignment.arnavigator.data

import android.annotation.SuppressLint
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.assignment.arnavigator.databinding.PoiListViewBinding
import com.assignment.arnavigator.proj.Algorithms
import java.math.RoundingMode
import java.text.DecimalFormat


class MyAdapter :
    RecyclerView.Adapter<RecyclerView.ViewHolder>()
{
    var poiList = emptyList<Poi>()
    var currentLoc= LatLon(0.0,0.0)
    var callback: ((Long) ->Unit)? = null
    lateinit var binding: PoiListViewBinding
    inner class MyViewHolder(itemView: View): RecyclerView.ViewHolder(itemView){
        val tvType = binding.poiType
        val tvDistance = binding.poiMeters
        val tvFollow = binding.poiFollow
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        Log.d("AdapterCreated","the adapter works")
        val layoutInflater = LayoutInflater.from(parent.context)
        binding = PoiListViewBinding.inflate(layoutInflater,parent,false)
        return MyViewHolder(binding.root)
    }


    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val myViewHolder = holder as MyViewHolder
        val currentItem = poiList[position]

        val textReplace = "${currentItem.name}\n${currentItem.locationType}"
        myViewHolder.tvType.text = textReplace


        val calculatedDistance = Algorithms.haversineDist(
            currentLoc.lon,currentLoc.lat,
            currentItem.lon.toDouble(),currentItem.lat.toDouble())/1000

        Log.d("Adaptervalue", "$currentLoc")
        if(currentLoc.lat == 0.0 && currentLoc.lon == 0.0){
            myViewHolder.tvDistance.text = "0 km"

        }else {
            val df = DecimalFormat("#.##")
            df.roundingMode = RoundingMode.CEILING
            myViewHolder.tvDistance.text = "${df.format(calculatedDistance)} km"

        }
        myViewHolder.tvFollow.setOnClickListener {
            callback?.let { it1 -> it1(currentItem.osm_id) }
        }
    }

    override fun getItemCount(): Int {
        return poiList.size

    }

    override fun getItemId(position: Int): Long {
        return poiList[position].id
    }

}