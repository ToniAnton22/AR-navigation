package com.assignment.arnavigator


import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.graphics.SurfaceTexture
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.view.Surface
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.commit
import com.assignment.arnavigator.data.*
import com.assignment.arnavigator.databinding.ActivityMainBinding
import com.assignment.arnavigator.opengl.OpenGLView
import com.github.kittinunf.fuel.httpGet
import com.github.kittinunf.fuel.json.responseJson
import com.github.kittinunf.result.Result
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.json.JSONArray


class MainActivity : AppCompatActivity(), SensorEventListener {



    private var permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.POST_NOTIFICATIONS)
    } else {
        arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.INTERNET,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION)
    }

    private var surfaceTexture: SurfaceTexture? = null

    val viewModel: ViewModels by viewModels()
    val poiViewModel: PoiViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    private lateinit var serviceConn: ServiceConnection
    private lateinit var receiver: BroadcastReceiver
    private lateinit var openGLView: OpenGLView
    private lateinit var glFragment: OpenGLFragment
    private lateinit var poisList: List<Poi>

    private lateinit var  moving :Moving
    var service: MappingService? = null


    lateinit var magField: Sensor
    lateinit var accel: Sensor
    lateinit var wm: DisplayManager
    var k = 0.075f

    var acc = FloatArray(3)
    var mag = FloatArray(3)
    var prevacc = floatArrayOf(0f,0f,0f)
    var prevmag = floatArrayOf(0f,0f,0f)
    var orientation= FloatArray(3)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)
        openGLView = OpenGLView(this) {
            surfaceTexture = it

            if (!startCamera()) {
                ActivityCompat.requestPermissions(this, permissions, 0)
            }

        }
        glFragment = OpenGLFragment(openGLView)
        startService()
        moving = Moving()
        poiViewModel.readAllPois.observe(this, {
            poisList = it
        })

        val sMgr = getSystemService(Context.SENSOR_SERVICE) as SensorManager
        accel = sMgr.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magField = sMgr.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        wm = getSystemService(DISPLAY_SERVICE) as DisplayManager

        sMgr.registerListener(this,magField, SensorManager.SENSOR_DELAY_UI)
        sMgr.registerListener(this, accel, SensorManager.SENSOR_DELAY_UI)

        setActivity()
        supportFragmentManager.commit {
            replace(binding.frameLayout1.id,glFragment)
        }


    }

    fun setActivity(){
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
                    try {
                        drawerLayout.closeDrawers()
                        val intent = Intent(this, UiActivity::class.java)
                        startActivity(intent)
                    }catch(e:Exception){
                        Log.d("Activitytranzaction",e.toString())
                    }
                    true
                }else ->{
                false
            }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val cordArr = floatArrayOf(0f,0f,0f)
        val currentLocation = LatLon()
        var prevLocation = LatLon( 0.0, 0.0)
        receiver = object: BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                Log.d("Intent sent","${intent?.action}")
                when(intent?.action){
                    "loc" -> {
                        currentLocation.lat = intent.getDoubleExtra("Lat", 0.0)
                        currentLocation.lon = intent.getDoubleExtra("Lng", 0.0)
                        val alt = intent.getFloatExtra("Alt", 0f)
                        if(currentLocation.lat != 0.0){
                            Log.d("currentLocation","${currentLocation.lat}")
                            val coord =moving.convert(currentLocation.lat,currentLocation.lon)

                            cordArr[0] = coord[1]
                            cordArr[1] = coord[0]
                            Log.d("Coord of camera","reciever ${cordArr[0]}, ${cordArr[1]},${alt}")
                            openGLView.renderer.cameraPov = floatArrayOf(cordArr[0],cordArr[1],alt)
                            viewModel.currentLatLon = LatLon(currentLocation.lat,currentLocation.lon)


                            if(currentLocation != prevLocation ){
                                Log.d("prevLocation updated","$prevLocation")
                                val distance = moving.calulateDistance(
                                    currentLocation.lat,
                                    currentLocation.lon,
                                    prevLocation.lat,
                                    prevLocation.lon
                                )
                                if(distance > 15){
                                    Log.d("Location updated","$distance")

                                    getPoi(currentLocation)
                                    prevLocation = currentLocation
                                }
                            }
                        }
                    }

                }
            }
        }
        val filter = IntentFilter().apply{
            addAction("loc")
        }
        registerReceiver(receiver,filter)

        poiViewModel.getAllPois().observe(this){
            Log.d("PoiIsGoinToOpen","${it.size}")
            setupPoiObjects(it)

        }
    }

    override fun onResume() {
        super.onResume()
        val currentLocation = receiveLocation()
        val curLoc = service?.getGps()
        Log.d("LocationRegistered","${currentLocation} or bind ${curLoc}")
    }

    private fun receiveLocation(){

    }



    private fun getPoi(cord: LatLon){
        Log.d("Http called", "is being called")
        val url = "https://hikar.org/webapp/map?bbox=${cord.lon -0.05},${cord.lat -0.05}," +
                "${cord.lon +0.05},${cord.lat + 0.05}&layers=poi&outProj=4326"
        //val url = "https://hikar.org/webapp/map?bbox=-0.73,51.04,-0.71,51.06&layers=poi&outProj=4326"
        url.httpGet().responseJson { _, response, result ->
            when(result) {
                is Result.Success ->{
                    val geojsonRootObj = result.get().obj()
                    val features = geojsonRootObj.getJSONArray("features")
                    convertAndAddJson(features)

                }
                is Result.Failure ->{
                    Log.d("Http error", "$result")
                }
                else -> {
                    Log.d("response","${response}")
                }
            }
        }
    }


    private fun convertAndAddJson(features: JSONArray) {
        var poiObj: Poi
        var skip: Boolean
        try {
            for (i in 0 until features.length()) {
                val propertiesObj =
                    features.getJSONObject(i).getJSONObject("properties")
                val geometryObj =
                    features.getJSONObject(i).getJSONObject("geometry")
                val coordinates = geometryObj.getJSONArray("coordinates")
                if (propertiesObj.has("name")) {
                    val name = propertiesObj.getString("name")
                    val osm_id = propertiesObj.getString("osm_id").toLong()
                    var locationType: String
                    if (propertiesObj.has("amenity")) {
                        locationType = propertiesObj.getString("amenity")
                        poiObj = Poi(
                            0, coordinates[1].toString().toFloat(),
                            coordinates[0].toString().toFloat(),
                            osm_id, name, locationType
                        )
                        if(poisList.isEmpty()){
                            poiViewModel.addPoi(poiObj)
                        }else {
                            skip = false
                            for (element in poisList) {
                                if (poiObj.osm_id == element.osm_id) {
                                    Log.d(
                                        "LiveObject",
                                        "Has been found, we will ignore it"
                                    )
                                    skip = true
                                }
                            }
                            if (!skip) {
                                poiViewModel.addPoi(poiObj)
                            }
                        }
                    } else if (propertiesObj.has("place")) {
                        locationType = propertiesObj.getString("place")
                        poiObj = Poi(
                            0, coordinates[1].toString().toFloat(),
                            coordinates[0].toString().toFloat(),
                            osm_id, name, locationType
                        )
                        if(poisList.isEmpty()){
                            poiViewModel.addPoi(poiObj)
                        }else {
                            skip = false
                            for (element in poisList) {
                                if (poiObj.osm_id == element.osm_id) {

                                    skip = true
                                }

                            }
                            if (!skip) {
                                poiViewModel.addPoi(poiObj)
                            }
                        }
                    } else {
                        Log.e(
                            "LocationType", "Location type is unknowns, object " +
                                    "is not fit for sending."
                        )
                    }
                    Log.e(
                        "NameKeyMissing", "Name key is unknowns, object " +
                                "is not fit for sending."
                    )
                }
            }
        }catch(e:Exception){
            Log.e("Something went wrong",e.stackTraceToString())
        }
    }

    private fun checkPermissions(): Boolean {
        return permissions.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun setupPoiObjects(pois: List<Poi>){

            Log.d("PoiIsGoinToOpen","${pois.size}")
            if(pois.isNotEmpty()) {
                Log.d("PoiIsGoinToOpen", "${pois.size}")
                pois.forEach {
                    openGLView.renderer.getPoiToOpenGl(it, viewModel.currentLatLon)

                }
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 0 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {

            startService()
            startCamera()
        } else {

            AlertDialog.Builder(this).setPositiveButton("OK", null)
                .setMessage("Will not work as camera permission not granted").show()
            checkPermissions()
        }
    }

    private fun startCamera(): Boolean {
        if(checkPermissions()){
            val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
            cameraProviderFuture.addListener({
                val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also{
                    val surfaceProvider: (SurfaceRequest) -> Unit = { request ->
                        val resolution = request.resolution
                        surfaceTexture?.apply{
                            setDefaultBufferSize(resolution.width, resolution.height)
                            val surface = Surface(this)
                            request.provideSurface(
                                surface,
                                ContextCompat.getMainExecutor(this@MainActivity.baseContext)
                            )
                            {}
                        }
                    }
                    it.setSurfaceProvider(surfaceProvider)
                }
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                try{
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(this, cameraSelector, preview)
                }catch (e:Exception){
                    Log.e("Opengl01log", e.stackTraceToString())
                }
            }, ContextCompat.getMainExecutor(this))
            return true
        }else{
            return false
        }
    }


    override fun onSensorChanged(p0: SensorEvent?) {
        val orientationMatrix = FloatArray(16)
        val remappedMatrix = FloatArray(16)

        if(p0?.sensor == accel ){

            val current = p0.values?.copyOf() as FloatArray
            acc = smoothing(current,prevacc)
            prevacc = acc.copyOf()

        }else if(p0?.sensor == magField){
            val current = p0.values?.copyOf() as FloatArray
            mag = smoothing(current,prevmag)
            prevmag = mag.copyOf()
        }

        SensorManager.getRotationMatrix(orientationMatrix, null, acc, mag)
        SensorManager.remapCoordinateSystem (orientationMatrix, SensorManager.AXIS_Y,
            SensorManager.AXIS_MINUS_X,
            remappedMatrix)
        SensorManager.getOrientation(remappedMatrix, orientation)
        try {

            openGLView.renderer.setOrientMtx(remappedMatrix)
        }catch(e:Exception){
            Log.e("OpenglException",e.toString())
        }



        if(orientation[0] >= 0 && orientation[0] <0.45) {
            Log.d("Sensor","Facing North")
        }else if(orientation[0] >=0.45 && orientation[0]<0.90){
            Log.d("Sensor","North east" +
                    "")
        }else if(orientation[0] >=0.90 && orientation[0]<1.35) {
            Log.d(
                "Sensor", "South east" +
                        ""
            )
        }else if(orientation[0] >=1.35 && orientation[0]<1.80) {
            Log.d(
                "Sensor", "South" +
                        ""
            )
        }else if(orientation[0] <0 && orientation[0]>-0.45) {
            Log.d(
                "Sensor", "North West" +
                        ""
            )
        }else if(orientation[0] <0.45 && orientation[0]>-0.9) {
            Log.d(
                "Sensor", "West" +
                        ""
            )
        }else if(orientation[0] <-0.9 && orientation[0]>-1.35) {
            Log.d(
                "Sensor", "South west" +
                        ""
            )
        }else if(orientation[0] <1.35 && orientation[0]>-1.80) {
            Log.d(
                "Sensor", "South" +
                        ""
            )
        }
    }

    private fun smoothing(current: FloatArray,prev: FloatArray): FloatArray {
        val result = floatArrayOf(0f,0f,0f)
        for(i in current.indices){
            result[i] = current[i]*k + prev[i]*(1-k)
        }

        return result
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {

    }
    private fun startService():Boolean{
        if(checkPermissions()) {
            try {
                val startIntent = Intent(this, MappingService::class.java)
                this.startService(startIntent)
                serviceConn = object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {

                        service = (binder as MappingService.MappingServiceBinder).mappingService
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {

                    }
                }
                val bindIntent = Intent(this, MappingService::class.java)
                this.bindService(bindIntent, serviceConn, Context.BIND_AUTO_CREATE)
            }catch (e: java.lang.Exception){
                Log.d("errorOpen","${e}")
            }


            return true
        }else{

            return false
        }
    }
    private fun stopServices(){
        var startIntent= Intent(this, MappingService::class.java)
        stopService(startIntent)
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun onDestroy() {
        super.onDestroy()
        stopServices()
        unregisterReceiver(receiver)
        unbindService(serviceConn)
        GlobalScope.launch(Dispatchers.IO){
            PoiDataBase.getDatabase(this@MainActivity).clearAllTables()
        }
    }

}
