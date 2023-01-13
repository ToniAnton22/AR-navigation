package com.assignment.arnavigator


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.assignment.arnavigator.opengl.OpenGLView

class OpenGLFragment(glView: OpenGLView) : Fragment(R.layout.opengl_view) {
    var glv = glView


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return glv
    }


}