package com.assignment.arnavigator.opengl


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.opengl.GLSurfaceView

@SuppressLint("ViewConstructor")
class OpenGLView(ctx: Context, callback: (SurfaceTexture) -> Unit)  : GLSurfaceView(ctx) {
    // Make the renderer an attribute of the OpenGLView so we can access
    // it from outside the OpenGLView
    val renderer = OpenGLRenderer(callback)
    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
    }
}
