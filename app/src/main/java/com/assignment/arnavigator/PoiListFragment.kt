package com.assignment.arnavigator

import android.annotation.SuppressLint
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import com.assignment.arnavigator.data.MyAdapter
import com.assignment.arnavigator.data.Poi
import com.assignment.arnavigator.data.PoiViewModel
import com.assignment.arnavigator.data.ViewModels
import com.assignment.arnavigator.databinding.ListViewBinding


class PoiListFragment: Fragment(R.layout.list_view), LifecycleOwner {
    private lateinit var binding: ListViewBinding
    private lateinit var adapter: MyAdapter

    val poiViewModel: PoiViewModel by activityViewModels()
    val viewModels : ViewModels by activityViewModels()




    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {

        binding = ListViewBinding.inflate(inflater, container, false)
        Log.d("BindingAdapter", "${binding}")

        return binding.root
    }

    @SuppressLint("NotifyDataSetChanged")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        adapter = MyAdapter()
        val recyclerView = binding.poiListView
        recyclerView.layoutManager = LinearLayoutManager(activity)

        recyclerView.adapter = adapter
        Log.d("ListtoAdapter", "${recyclerView} adapter ${adapter}")
        viewModels.getCurrentLocation().observe(this.viewLifecycleOwner, {
            Log.d("AdapterlatLon","${it}")
            if(adapter.currentLoc != it){
                adapter.currentLoc = it
                adapter.notifyDataSetChanged()
            }
        })

    }


    @SuppressLint("NotifyDataSetChanged")
    fun showAll(){
        var previousPoi= emptyList<Poi>()

        poiViewModel.readAllPois.observe(viewLifecycleOwner) {
            if(previousPoi.isEmpty()){
                previousPoi = previousPoi.plus(it)
                Log.d("ListtoAdapter",
                    "${adapter.itemCount}")
                adapter.poiList = it
                Log.d("ListtoAdapter", "${adapter.itemCount}")
                adapter.notifyDataSetChanged()
            }else{
                var rep = false
                it.forEach{
                    Log.d("Coordinates","${it.lat},${it.lon}")
                    for(i in 0 until previousPoi.size) {
                        if (previousPoi[i].osm_id == it.osm_id){
                            rep = true
                            break
                        }
                    }
                }
                if(!rep){
                    Log.d("ListtoAdapter",
                        "${adapter.itemCount}")
                    adapter.poiList = it
                    Log.d("ListtoAdapter", "${adapter.itemCount}")
                    adapter.notifyDataSetChanged()
                }
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun orderByType(type:String){
        poiViewModel.getByType(type).observe(viewLifecycleOwner){
            adapter.poiList = it
            adapter.notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun delete(){
        poiViewModel.deleteTables()
        adapter.notifyDataSetChanged()
    }
}
