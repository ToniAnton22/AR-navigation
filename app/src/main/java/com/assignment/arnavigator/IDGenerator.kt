package com.assignment.arnavigator

import java.text.SimpleDateFormat
import java.util.*

class IDGenerator {

    fun getDate(){
        var now = Date();
        var id = SimpleDateFormat("ddhhmmssSS",Locale.UK).format(now).toInt()
    }

}