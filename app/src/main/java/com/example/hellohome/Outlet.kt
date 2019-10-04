package com.example.hellohome

import android.os.Handler
import android.text.Layout
import android.view.View
import android.view.ViewParent
import android.widget.CompoundButton
import android.widget.ImageView
import android.widget.ToggleButton
import com.google.gson.JsonObject
import com.mikhaellopez.circularprogressbar.CircularProgressBar

var handler = Handler()
var outlets = arrayListOf<Outlet>()

class Outlet(id: Int, ui: UIObject){
    var id = id
    var ui = ui
    var state = false
    var updating = false

    fun turnOn()
    {
        val payload = JsonObject()
        setInProgress(true)
        payload.addProperty("$id", "o")
        pubNub.publish(payload,1)
        timeoutProtector()
    }
    fun turnOff()
    {
        val payload = JsonObject()
        setInProgress(true)
        payload.addProperty("$id", "f")
        pubNub.publish(payload,1)
        timeoutProtector()
    }

    fun setNewState(newState: Boolean)
    {
        ui.toggle.isChecked = newState
        setInProgress(false)
        updating = false
        state = newState
        handler.removeCallbacksAndMessages("timeoutProtector$id")
    }

    fun setInProgress(inProgress: Boolean)
    {
        updating = ui.setUIInProgress(inProgress)
    }

    fun timeoutProtector()
    {
        handler.postDelayed({
            if (updating) {
                println("error 2")
            }
        },"timeoutProtector$id", 7000)
    }
}

class UIObject(toggle: CompoundButton, image: ImageView, progressBar: CircularProgressBar){

    var toggle = toggle
    var image = image
    var progressBar = progressBar

    fun setUIInProgress(inProgress: Boolean): Boolean
    {
        toggle.setClickable(!inProgress)

        if (inProgress) {
            image.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
        } else {
            image.visibility = View.VISIBLE
            progressBar.visibility = View.INVISIBLE
        }
        return inProgress
    }
}


fun getState(outlets: Array<Outlet>): BooleanArray {

    var state = BooleanArray(outlets.size-1)
    for (i in 0..outlets.size-1){
        state[i] = outlets[i].state
    }
    return state
}