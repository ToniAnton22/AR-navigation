package com.assignment.arnavigator.opengl

import android.opengl.GLES20
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class PointsOfInterestObject(x:Float, y:Float, z:Float,tr:Float,tg:Float,
                             tb:Float, name:String, type:String, distance: Double,osm_id:Long) {
    val vertexBuf: FloatBuffer
    val indexBuf: ShortBuffer
    val name: String
    val type: String
    val distance: Double
    val osm_id: Long
    var stride=24

    init {
        val vertices = floatArrayOf(
            x, y+0.5f, z,tr,tg,tb,
            x+0.5f, y+0.5f, z,tr,tg,tb,
            x+0.5f, y+0.5f, z+0.5f,tr,tg,tb,
            x, y+0.5f, z+0.5f,tr,tg,tb,
            x, y, z,0f,0f,1f,
            x+0.5f, y, z,tr,tg,tb,
            x+0.5f, y, z+0.5f,tr,tg,tb,
            x, y, z+0.5f,1.0f,tr,tg,tb
        )
        this.name= name
        this.type = type
        this.distance = distance
        this.osm_id = osm_id

        vertexBuf = makeBuffer(vertices)

        indexBuf= indexBuffer()
    }
    fun makeBuffer(vertices: FloatArray) : FloatBuffer {
        val bbuf : ByteBuffer = ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES)
        bbuf.order(ByteOrder.nativeOrder())
        val fbuf : FloatBuffer  = bbuf.asFloatBuffer()
        fbuf.put(vertices)
        fbuf.position(0)
        return fbuf
    }

    fun indexBuffer() :ShortBuffer{
        val indices = shortArrayOf(3,2,6,
            6,5,2,
            2,1,5,
            5,4,7,
            7,6,5,
            0,1,2,
            2,3,0,
            4,5,1,
            1,0,4,
            7,4,0,
            0,3,7,
            6,7,3,
        ) // 36 values
        val ibuf = ByteBuffer.allocateDirect(indices.size*Short.SIZE_BYTES)
        ibuf.order(ByteOrder.nativeOrder())
        val indexBuffer: ShortBuffer = ibuf.asShortBuffer()
        indexBuffer.put(indices)
        indexBuffer.position(0)
        return indexBuffer
    }

    fun render(ref_aVertex: Int,ref_aColor: Int){
        vertexBuf.position(0)
        GLES20.glGetError()
        GLES20.glVertexAttribPointer(ref_aVertex, 3, GLES20.GL_FLOAT,
            false, stride, vertexBuf)
        GLES20.glGetError()
        vertexBuf.position(3)
        GLES20.glGetError()
        GLES20.glVertexAttribPointer(ref_aColor, 3, GLES20.GL_FLOAT,
            false, stride, vertexBuf)

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,indexBuf.limit(),
            GLES20.GL_UNSIGNED_SHORT,indexBuf)

    }
}