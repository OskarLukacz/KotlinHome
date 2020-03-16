package com.example.hellohome

import android.os.Handler
import android.view.View
import android.widget.CompoundButton
import android.widget.ImageView
import com.google.gson.JsonObject
import com.mikhaellopez.circularprogressbar.CircularProgressBar

//var handler = Handler()


class Outlet(var id: Int, var ui: UIObject) {
    var state = false
    var updatingTo = state
    var updating = false

    fun setsState(to: Boolean) {
        if (to != state) {
            update(false, to)
        }
        state = to
        ui.button.isChecked = to

    }


    fun update(inProgress: Boolean, to: Boolean) {
        if (inProgress) {
            ui.setUIInProgress(true)
            updating = true
            updatingTo = to
        }else {
            ui.setUIInProgress(false)
            updating = false
            updatingTo = state
        }
    }
}

class UIObject(var button: CompoundButton, private var image: ImageView, var progressBar: CircularProgressBar){

    fun setUIInProgress(inProgress: Boolean)
    {
        button.isClickable = !inProgress

        if (inProgress) {
            image.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
        } else {
            image.visibility = View.VISIBLE
            progressBar.visibility = View.INVISIBLE
        }
    }
}

