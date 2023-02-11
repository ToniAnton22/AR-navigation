package com.assignment.arnavigator

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.Menu
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.assignment.arnavigator.data.ClosestPoi
import com.assignment.arnavigator.data.LatLon
import com.assignment.arnavigator.data.PoiDataBase
import com.assignment.arnavigator.data.ViewModels
import com.assignment.arnavigator.databinding.UiActivityBinding
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import android.view.MenuItem
import androidx.preference.PreferenceManager


class UiActivity: AppCompatActivity() {
    private lateinit var binding: UiActivityBinding
    private lateinit var mapFragment: MapFragment
    private lateinit var poiListFragment: PoiListFragment
    val viewModel: ViewModels by viewModels()
    private lateinit var receiver: BroadcastReceiver
    private lateinit var nMgr: NotificationManager


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = UiActivityBinding.inflate(layoutInflater)
        Log.d(" Activity Inflated","Activity inflated")

        setContentView(binding.root)
        mapFragment = MapFragment()
        poiListFragment = PoiListFragment()

        nMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager


        val nv = binding.nv
        val drawerLayout = binding.drawerLayout

        nv.setNavigationItemSelectedListener {
            when(it.itemId){
                R.id.augmentedWorld ->{
                    drawerLayout.closeDrawers()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.map ->{
                    drawerLayout.closeDrawers()
                    val intent = Intent(this, UiActivity::class.java)
                    startActivity(intent)
                    true
                }else ->{
                false
            }
            }
        }
        supportFragmentManager.beginTransaction()
            .replace(binding.mapFragment.id, mapFragment)
            .commit()
        supportFragmentManager.beginTransaction()
            .replace(binding.listfragment.id, poiListFragment)
            .commit()

    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.poi_type_menu,menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.show_all->{
                poiListFragment.showAll()
                mapFragment.markAll()
            }
            R.id.getLocation ->{
                poiListFragment.delete()
                mapFragment.deleteAllMarkers()
            }
            R.id.preferences ->{
                val intent = Intent(this,MyPrefsActivity::class.java)
                startActivity(intent)
            }
        }
        return false
    }

    override fun onStart() {
        super.onStart()
        val currentLocation = LatLon()
        receiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("Intent sent","${intent?.action}")
                when(intent?.action){
                    "loc" -> {
                        currentLocation.lat = intent.getDoubleExtra("Lat", 0.0)
                        currentLocation.lon = intent.getDoubleExtra("Lng", 0.0)

                        viewModel.currentLatLon = LatLon(currentLocation.lat, currentLocation.lon)
                        Log.d("currentUiLocation","${viewModel.currentLatLon.lat}")


                    }
                }
            }
        }
        val filter = IntentFilter().apply{
            addAction("loc")
        }
        registerReceiver(receiver,filter)
        ClosestPoi(0.0,0,"")

    }

    override fun onResume() {
        super.onResume()
        val prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext)
        val type = prefs.getString("types", "SHOW_ALL") ?: "SHOW_ALL"
        Log.d("Typeof",type)
        if(type == "SHOW_ALL"){
            poiListFragment.showAll()
            mapFragment.markAll()
        }else{
            poiListFragment.orderByType(type)
            mapFragment.markByType(type)
        }
    }
    fun getNotification(it:ClosestPoi){

        val channelID = "CLOSE_TO_POI"

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val uniqueId = 12
            val channel = NotificationChannel(channelID,"Distance",
                NotificationManager.IMPORTANCE_DEFAULT)
            nMgr.createNotificationChannel(channel)

            val notification = Notification.Builder(this,channelID)
                .setContentTitle("You are close !")
                .setContentText("You've almost reached ${it.name}")
                .setSmallIcon(androidx.constraintlayout.widget.R.drawable.notification_bg)
                .build()

            nMgr.notify(uniqueId,notification)
        }else {
            val uniqueId = 23
            val notification = Notification.Builder(this)
                .setContentTitle("You almost reached")
                .setContentText("You almost reached ${it.name}")
                .setSmallIcon(R.drawable.ic_launcher_background)
                .build()

            nMgr.notify(uniqueId, notification)

        }
    }

    override fun onStop() {
        super.onStop()
        var prevClosestPoi = ClosestPoi(0.0,0,"")

        viewModel.getClosestLocation().observe(this) {
            if (prevClosestPoi.equals(it)) {
                Log.d("The same place","Same place, no need to announce")
            } else {
                if(it.distance <= 200.0) {
                    prevClosestPoi = it
                    getNotification(it)
                }
            }
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDestroy() {
        super.onDestroy()
        GlobalScope.launch(Dispatchers.IO){
            PoiDataBase.getDatabase(this@UiActivity).clearAllTables()
        }
    }
}