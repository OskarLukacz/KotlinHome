package com.example.hellohome

import android.app.AlertDialog
import android.content.Context
import android.content.Context.*
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.support.v4.content.ContextCompat
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import android.view.View
import android.widget.*
import com.google.gson.JsonObject
import com.mikhaellopez.circularprogressbar.CircularProgressBar
import com.pubnub.api.enums.PNStatusCategory
import kotlinx.android.synthetic.main.activity_main.*
import java.util.Arrays


class MainActivity : AppCompatActivity() {

    //onCreate function
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Activity specifc variables

        val toggle0: CompoundButton = findViewById(R.id.toggleButton)
        val toggle1: CompoundButton = findViewById(R.id.toggleButton2)
        val toggle2: CompoundButton = findViewById(R.id.toggleButton3)
        val toggle3: CompoundButton = findViewById(R.id.toggleButton4)

        val image0:  ImageView = findViewById(R.id.imageView)
        val image1:  ImageView = findViewById(R.id.imageView2)
        val image2:  ImageView = findViewById(R.id.imageView3)
        val image3:  ImageView = findViewById(R.id.imageView4)

        val bar0:    CircularProgressBar = findViewById(R.id.circularProgressBar)
        val bar1:    CircularProgressBar = findViewById(R.id.circularProgressBar2)
        val bar2:    CircularProgressBar = findViewById(R.id.circularProgressBar3)
        val bar3:    CircularProgressBar = findViewById(R.id.circularProgressBar4)

        //Initialize activity related variables

        outlets.add(Outlet(0, UIObject(toggle0,image0,bar0)))
        outlets.add(Outlet(1, UIObject(toggle1,image1,bar1)))
        outlets.add(Outlet(2, UIObject(toggle2,image2,bar2)))
        outlets.add(Outlet(3, UIObject(toggle3,image3,bar3)))

        for (i in 0..3)
        {
            outlets[i].ui.progressBar.apply {
                setProgressWithAnimation(5f, 1000) // =1s
                indeterminateMode = true
            }
        }

        //PubNub configuration
        val subscribeCallback: SubscribeCallback = object : SubscribeCallback() {
            override fun status(pubnub: PubNub, status: PNStatus) {
                if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {
                    pubNub.publish(pubNub.ping, 0)
                }
            }

            override fun message(pubnub: PubNub, message: PNMessageResult) {
                handleMessage(message.message.toString())
            }

            override fun presence(pubnub: PubNub, presence: PNPresenceEventResult) {}
        }

        pubNub.configure(subscribeCallback)


        //Start listeners
        pubNub.subscribe()

    }


    //onRestart function
    override fun onRestart()
    {
        super.onRestart()
        pubNub.reStartPubNubSubscribe()
    }


    //Handle new subscribe event
    fun handleMessage(message: String)
    {
        updateTextField(message)
        if (message.substring(2, 8) == "status") {
            println("Status received")
            if (message.contains("curState_")) {
                val i = message.indexOf("curState_")
                val newState = message.substring(i + 9, i + 13)
                newState(newState)
            }
        }
    }


    //Toggle Function on Toggle Button keypress
    fun toggleFunction(view: View)
    {
        val btn = view as ToggleButton
        val state = btn.isChecked
        var i = 0
        when (btn.id) {
            R.id.toggleButton ->  i = 0
            R.id.toggleButton2 -> i = 1
            R.id.toggleButton3 -> i = 2
            R.id.toggleButton4 -> i = 3
        }

        runOnUiThread {
            btn.toggle()
        }

        if (state) {outlets[i].turnOn()}
        else {outlets[i].turnOff()}
    }

    //Update the state in UI
    fun updateUIState (state: List<Boolean>)
    {
        runOnUiThread {
            for (i in 0..3) {
                outlets[i].ui.toggle.setChecked(state[i])
            }
        }
    }

    fun updateTextField(newMessage: String)
    {
        runOnUiThread {
            textView.run { text = ("Last message: $newMessage") }
        }
    }
    //Update current state and set UI to reflect change
    fun newState(input: String)
    {
        var updatedStateArray = mutableListOf<Boolean>()
        if (input.length == 4) {
            for (i in input) {
                when (i) {
                    '0' -> updatedStateArray.add(false)
                    '1' -> updatedStateArray.add(true)
                }
            }
            println(updatedStateArray.toString())

            for (x in 0..3) {
                if (updatedStateArray[x] != outlets[x].state) {
                    runOnUiThread { outlets[x].setNewState(updatedStateArray[x]) }
                }
            }
            updateUIState(updatedStateArray)
        }
    }


    //Handle press for manual message publishing
    fun handlePress(view: View)
    {

        val button =  view as Button
        val hint  = button.hint as String
        val msg= JsonObject()

        when (hint) {

            "Zero ON" -> msg.addProperty("0", "o")
            "Zero OFF" -> msg.addProperty("0", "f")
            "One ON" -> msg.addProperty("1", "o")
            "One OFF" -> msg.addProperty("1", "f")
            "Two ON" -> msg.addProperty("2", "o")
            "Two OFF" -> msg.addProperty("2", "f")
            "Three ON" -> msg.addProperty("3", "o")
            "Three OFF" -> msg.addProperty("3", "f")
            "Toggle All" -> msg.addProperty("A", "A")
        }

        pubNub.publish(msg,1)
    }

    /*
    //Connection check
    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE)
        return if (connectivityManager is ConnectivityManager) {
            val networkInfo: NetworkInfo? = connectivityManager.activeNetworkInfo
            networkInfo?.isConnected ?: false
        } else false
    }


    //Error Handling
    fun errorOccured(flag: Int)
    {
        var errorMessage = ""
        var positiveMessage = ""
        var negetiveMessage = ""

        when(flag)
        {
            0 -> {
                errorMessage = "Publish Error"
                positiveMessage = "TODO"
                negetiveMessage = "TODO"
            }
            1 -> {
                errorMessage = "Internet Connectivity Error"
                positiveMessage = "Close Application"
                negetiveMessage = "Dismiss"
            }
            2 -> {
                errorMessage = "Response Timeout"
                positiveMessage = "Reset Subscriber"
                negetiveMessage = "Dismiss"
            }
        }


        val dialogBuilder = AlertDialog.Builder(this)

        dialogBuilder.setMessage(errorMessage)
        .setCancelable(false)
        .setPositiveButton(positiveMessage, DialogInterface.OnClickListener {
                dialog, id -> dialog.cancel()
                when(positiveMessage)
                {
                    "TODO" -> println("Error needs fixing")
                    "Close Application" -> finish()
                    "Reset Subscriber" -> pubNub.reStartPubNubSubscribe()
                }
        })
        .setNegativeButton(negetiveMessage, DialogInterface.OnClickListener {
                dialog, id -> dialog.cancel()
                when(positiveMessage)
                {
                    "TODO" -> println("Error needs fixing")
                    "Dismiss" -> println("Error Dismissed")
                }
        })

        val alert = dialogBuilder.create()
        alert.setTitle("Error")

        alert.show()
    }*/
}