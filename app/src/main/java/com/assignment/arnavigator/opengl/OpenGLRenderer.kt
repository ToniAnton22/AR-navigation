package com.assignment.arnavigator.opengl

import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import android.util.Log
import com.assignment.arnavigator.data.LatLon
import com.assignment.arnavigator.data.Poi
import com.assignment.arnavigator.proj.Algorithms
import com.assignment.arnavigator.proj.EastNorth
import com.assignment.arnavigator.proj.LonLat
import com.assignment.arnavigator.proj.SphericalMercatorProjection
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.cos
import kotlin.math.sin

data class EastNorthPoi(val eastNorth: EastNorth,val name: String,val type:String, val distance: Double)

class OpenGLRenderer(val textureAvailableCallback: (SurfaceTexture) -> Unit) : GLSurfaceView.Renderer {


    // Must negate y when calculating texcoords from vertex coords as bitmap image data assumes
    // y increases downwards
    val sph = SphericalMercatorProjection()
    val texVertexShaderSrc =
        "attribute vec4 aTexVertex;\n" +
                "varying vec2 vTextureValue;\n" +
                "void main (void)\n" +
                "{\n" +
                "gl_Position = aTexVertex;\n" +
                "vTextureValue = vec2(0.5*(1.0 + aTexVertex.x), 0.5*(1.0 -aTexVertex.y));\n" +
                "}\n"
    val texFragmentShaderSrc =
        "#extension GL_OES_EGL_image_external: require\n" +
                "precision mediump float;\n" +
                "varying vec2 vTextureValue;\n" +
                "uniform samplerExternalOES uTexture;\n" +
                "void main(void)\n" +
                "{\n" +
                "gl_FragColor = texture2D(uTexture,vTextureValue);\n" +
                "}\n"
    val vertexShaderSrc =
        "attribute vec4 aVertex, aColour;\n" +
                "varying vec4 vColour;\n" +
                "uniform mat4 uView, uProjection;" +
                "void main(void)\n" +
                "{\n"+
                "gl_Position = uProjection*uView*aVertex;\n" +
                "vColour = aColour;\n"+
                "}\n"

    val fragmentShaderSrc =
        "precision mediump float;\n" +
                "varying vec4 vColour;\n" +
                "void main(void)\n"+
                "{\n"+
                "gl_FragColor = vColour;\n" +
                "}\n"


    var texShaderProgram = -1
    var objectShader = -1


    var texBuffer: FloatBuffer? = null

    var viewMatrix = FloatArray(16)
    var projectionMatrix = FloatArray(16)
    private var lastOrientMtx = FloatArray(16)
    var rotation = 0f

    lateinit var texIndexBuffer: ShortBuffer

    var drawBuffer: FloatBuffer? = null
    lateinit var drawImdexBuffer: ShortBuffer

    var cameraFeedSurfaceTexture: SurfaceTexture? = null

    var cameraPov = floatArrayOf(0.0f,0.0f,0.0f)

    var poisObjs = emptyArray<Poi>()
    var easObj = emptyArray<EastNorthPoi>()



    // We initialise the OpenGL view here
    override fun onSurfaceCreated(unused: GL10, config: EGLConfig) {
        // Set the background colour (red=0, green=0, blue=0, alpha=1)
        GLES20.glClearColor(0.0f, 1.0f, 0.0f, 1.0f)

        // Enable depth testing - will cause nearer 3D objects to automatically
        // be drawn over further objects
        GLES20.glClearDepthf(1.0f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        // http://stackoverflow.com/questions/6414003/using-surfacetexture-in-android
        val GL_TEXTURE_EXTERNAL_OES = 0x8d65
        val textureId = IntArray(1)
        GLES20.glGenTextures(1, textureId, 0)
        if (textureId[0] != 0) {
            GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
            GLES20.glBindTexture(GL_TEXTURE_EXTERNAL_OES, textureId[0])

            // Mag filters not really needed here...

            cameraFeedSurfaceTexture = SurfaceTexture(textureId[0])

            val obVertexShader = compileShader(GLES20.GL_VERTEX_SHADER, vertexShaderSrc)
            val obFragmetShader = compileShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderSrc)
            objectShader = linkShader(obVertexShader, obFragmetShader)

            val texVertexShader = compileShader(GLES20.GL_VERTEX_SHADER, texVertexShaderSrc)
            val texFragmentShader = compileShader(GLES20.GL_FRAGMENT_SHADER, texFragmentShaderSrc)
            texShaderProgram = linkShader(texVertexShader, texFragmentShader)

            createCameraRect()
            createDrawingRect()

            textureAvailableCallback(cameraFeedSurfaceTexture!!)
            val refShaderVar = GLES20.glGetUniformLocation(texShaderProgram, "uTexture")
            GLES20.glUniform1i(refShaderVar, 0)
        }
    }

    // We draw our shapes here
    override fun onDrawFrame(unused: GL10) {

        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)
        // Camera
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)

        GLES20.glUseProgram(texShaderProgram)
        cameraFeedSurfaceTexture?.updateTexImage()



        if(texBuffer == null) {
            Log.d("OpenGL01Log", "null tex buffer")
            return
        }
        val attrVarRef = GLES20.glGetAttribLocation(texShaderProgram, "aTexVertex")
        texBuffer?.position(0)
        texIndexBuffer.position(0)

        GLES20.glEnableVertexAttribArray(attrVarRef)
        GLES20.glVertexAttribPointer(attrVarRef, 3, GLES20.GL_FLOAT, false, 0, texBuffer)
        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            texIndexBuffer.limit(),
            GLES20.GL_UNSIGNED_SHORT,
            texIndexBuffer
        )

        GLES20.glEnable(GLES20.GL_DEPTH_TEST)


        // Overlay
        GLES20.glUseProgram(objectShader)

        Matrix.setIdentityM(viewMatrix,0)
        viewMatrix = lastOrientMtx.copyOf()
        Log.d("Coord of camera","on draw${-cameraPov[0]}, ${-cameraPov[1]},${-cameraPov[2]}")
        // Try switching the cemrapovs
        Matrix.translateM(viewMatrix,0,-cameraPov[0], -cameraPov[1],-cameraPov[2])

        val ref_uProjMatrix = GLES20.glGetUniformLocation(objectShader,"uProjection")
        GLES20.glUniformMatrix4fv(ref_uProjMatrix, 1, false, projectionMatrix, 0)

        val ref_ViewMatrix = GLES20.glGetUniformLocation(objectShader,"uView")
        GLES20.glUniformMatrix4fv(ref_ViewMatrix,1,false,viewMatrix,0)

        val ref_aVertex = GLES20.glGetAttribLocation(objectShader,"aVertex")


        val ref_aColour = GLES20.glGetAttribLocation(objectShader,"aColour")

        //Remove /* to test the object in your proximity
        /*
        val testCube = PointsOfInterestObject(cameraPov[0]-2,cameraPov[1]-1,cameraPov[2],
            1f,1f,0f,"Mcdoanlds","Restaurant",5.0,100242)
        testCube.render(ref_aVertex,ref_aColour)

        PointsOfInterestObject(30449.592f, 474094.4f,70.6f,
            1f,1f,0f,"Mcdoanlds","Restaurant",5.0,100242).render(
            ref_aVertex,ref_aColour
        )
        */



        easObj.forEach {

            val poi = PointsOfInterestObject(it.eastNorth.easting.toFloat(),
            it.eastNorth.northing.toFloat(),cameraPov[2],0.5f,0.5f,0.4f,
            it.name,it.type,it.distance,2012)
            poi.render(ref_aVertex,ref_aColour)
        }

        /*if(poisObjs.isNotEmpty()){
            try {

                poisObjs.forEach {

                    it.render(ref_aVertex,ref_aColour)
                }
            }catch (e:Exception){
                Log.d("OpenglObject",e.stackTraceToString())
            }
        }*/
        GLES20.glDisableVertexAttribArray(attrVarRef)
        GLES20.glDisableVertexAttribArray(ref_aVertex)
        GLES20.glDisableVertexAttribArray(ref_aColour)
    }

    // Used if the screen is resized
    override fun onSurfaceChanged(unused: GL10, w: Int, h: Int) {
        GLES20.glViewport(0, 0, w, h)
        val hfov = 80.0f
        val aspect: Float = w.toFloat() / h.toFloat()
        Matrix.perspectiveM(projectionMatrix, 0, hfov/aspect, aspect, 1.4f, 500f)
    }

    fun compileShader(shaderType: Int, shaderCode: String): Int {
        val shader = GLES20.glCreateShader(shaderType)
        GLES20.glShaderSource(shader, shaderCode)
        GLES20.glCompileShader(shader)
        return shader
    }

    fun linkShader(vertexShader: Int, fragmentShader: Int): Int {
        val shaderProgram = GLES20.glCreateProgram()
        GLES20.glAttachShader(shaderProgram, vertexShader)
        GLES20.glAttachShader(shaderProgram, fragmentShader)
        GLES20.glLinkProgram(shaderProgram)
        val linkStatus = IntArray(1)
        GLES20.glGetProgramiv(shaderProgram, GLES20.GL_LINK_STATUS, linkStatus, 0)

        GLES20.glUseProgram(shaderProgram)
        Log.d("OpenGL01Log", "Shader program = $shaderProgram")
        return shaderProgram
    }

    fun makeBuffer(vertices: FloatArray): FloatBuffer {
        val bbuf: ByteBuffer = ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES)
        bbuf.order(ByteOrder.nativeOrder())
        val fbuf: FloatBuffer = bbuf.asFloatBuffer()
        fbuf.put(vertices)
        fbuf.position(0)
        return fbuf
    }

    fun makeIndexBuffer(indices: ShortArray): ShortBuffer {
        val bbuf: ByteBuffer = ByteBuffer.allocateDirect(indices.size * Short.SIZE_BYTES)
        bbuf.order(ByteOrder.nativeOrder())
        val sbuf: ShortBuffer = bbuf.asShortBuffer()
        sbuf.put(indices)
        sbuf.position(0)
        return sbuf
    }

    private fun createCameraRect() {

        val cameraRect = floatArrayOf(-1f,1f,0f ,-1f,-1f,0f,1f,-1f,0f, 1f,1f,0f)
        val indices = shortArrayOf(0, 1, 2, 2, 3, 0)

        texBuffer = makeBuffer(cameraRect)
        texIndexBuffer = makeIndexBuffer(indices)
    }

    private fun createDrawingRect(){
        val vertices = floatArrayOf( 0f,0f,-3f, 1f,0f,-3f, 0.5f,1f,-3f,
            -0.5f,0f,-6f)
        val verticesIndex= shortArrayOf(0,0,-2, 1,0,-2, 1,1,-2, 0,1,-2)

        drawBuffer = makeBuffer(vertices)
        drawImdexBuffer= makeIndexBuffer(verticesIndex)
    }


    fun getPoiToOpenGl(poi: Poi, currentLoc:LatLon){
        var repeating = false
        if(poisObjs.isEmpty()){
            val eastNorth= sph.project(LonLat(poi.lon.toDouble(), poi.lat.toDouble()))

            val distance = Algorithms.haversineDist(
                currentLoc.lon,currentLoc.lat, poi.lon.toDouble(), poi.lat.toDouble()
            )/1000
            val en = EastNorthPoi(eastNorth,poi.name,poi.locationType,distance)
            easObj = easObj.plus(en)
            poisObjs = poisObjs.plus(poi)
            Log.d("Item arrives","${poisObjs.size}")
        }else{
            for(i in 0 until poisObjs.size){
                if(poi.osm_id == poisObjs[i].osm_id){
                    repeating = true
                    break
                }
            }
            if(repeating != true){
                val eastNorth= sph.project(LonLat(poi.lon.toDouble(), poi.lat.toDouble()))
                val distance = Algorithms.haversineDist(
                    currentLoc.lon,currentLoc.lat, poi.lon.toDouble(), poi.lat.toDouble()
                )/1000
                val en = EastNorthPoi(eastNorth,poi.name,poi.locationType,distance)
                Log.d("Item arrives","${eastNorth}")

                easObj = easObj.plus(en)
                poisObjs = poisObjs.plus(poi)
                Log.d("Item arrives","${poisObjs.size}")
            }
        }



    }
    /*fun filter(){
        var repeating = false
        val osm_id = poi.osm_id
        if(poisObjs.isEmpty()){
            val eastNorth= sph.project(LonLat(poi.lon.toDouble(), poi.lat.toDouble()))

            val distance = Algorithms.haversineDist(
                currentLoc.lon,currentLoc.lat, poi.lon.toDouble(), poi.lat.toDouble()
            )/1000
            val poiObj = PointsOfInterestObject(eastNorth.easting.toFloat(),
                eastNorth.northing.toFloat(),cameraPov[2],0f,0f,1f,poi.name,
                poi.locationType,distance,osm_id
            )

            poisObjs = poisObjs.plus(poiObj)
            Log.d("Item arrives","${poisObjs.size}")
        }else{
            for(i in 0 until poisObjs.size){
                if(poi.osm_id == poisObjs[i].osm_id){
                    repeating = true
                    break
                }
            }
            if(repeating != true){
                val eastNorth= sph.project(LonLat(poi.lon.toDouble(), poi.lat.toDouble()))
                Log.d("Item arrives","${eastNorth}")
                val distance = Algorithms.haversineDist(
                    currentLoc.lon,currentLoc.lat, poi.lon.toDouble(), poi.lat.toDouble()
                )/1000
                val poiObj = PointsOfInterestObject(eastNorth.easting.toFloat(),
                    eastNorth.northing.toFloat(),0f,0f,0f,1f,poi.name,
                    poi.locationType,distance,osm_id
                )
                Log.d("Item arrives","poi object ${poiObj.name}")
                poisObjs = poisObjs.plus(poiObj)
                Log.d("Item arrives","${poisObjs.size}")
            }
        }

    }*/

    fun setOrientMtx(orientMtx: FloatArray) {
        lastOrientMtx = orientMtx.clone()

    }

}