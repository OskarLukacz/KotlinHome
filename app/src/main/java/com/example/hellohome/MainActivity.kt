package com.example.hellohome

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
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

    //Activity Variables


    //Pubnub Variables
    val pnConfiguration = PNConfiguration()
    val subscribeCallback: SubscribeCallback = object : SubscribeCallback()  {
        override fun status(pubnub: PubNub, status: PNStatus) {if (status.getCategory() == PNStatusCategory.PNConnectedCategory) {publish(ping,0)}}
        override fun message(pubnub: PubNub, message: PNMessageResult) {handleMessage(message.message.toString())}
        override fun presence(pubnub: PubNub, presence: PNPresenceEventResult) {}
    }
    val pubNub = PubNub(pnConfiguration)

    //Global Variables
    var state = BooleanArray(4)
    val ping: JsonObject = JsonObject()
    var toggles = arrayListOf<CompoundButton>()
    var images = arrayListOf<ImageView>()
    var bars = arrayListOf<CircularProgressBar>()
    var waitingForResponse = false



    //onCreate function
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Activity specifc variables

        val toggle:  CompoundButton = findViewById(R.id.toggleButton)
        val toggle2: CompoundButton = findViewById(R.id.toggleButton2)
        val toggle3: CompoundButton = findViewById(R.id.toggleButton3)
        val toggle4: CompoundButton = findViewById(R.id.toggleButton4)

        val image:   ImageView = findViewById(R.id.imageView)
        val image2:  ImageView = findViewById(R.id.imageView2)
        val image3:  ImageView = findViewById(R.id.imageView3)
        val image4:  ImageView = findViewById(R.id.imageView4)

        val bar:     CircularProgressBar = findViewById(R.id.circularProgressBar)
        val bar2:    CircularProgressBar = findViewById(R.id.circularProgressBar2)
        val bar3:    CircularProgressBar = findViewById(R.id.circularProgressBar3)
        val bar4:    CircularProgressBar = findViewById(R.id.circularProgressBar4)

        toggles = arrayListOf(toggle,toggle2,toggle3,toggle4)
        images = arrayListOf(image,image2,image3,image4)
        bars = arrayListOf(bar,bar2,bar3,bar4)

        for (bar in bars){
            bar.apply {
                setProgressWithAnimation(5f, 1000) // =1s
                indeterminateMode = true
            }
        }


        //PubNub configuration

        pnConfiguration.subscribeKey = "sub-c-05dce56c-3c2e-11e7-847e-02ee2ddab7fe"
        pnConfiguration.publishKey = "pub-c-b6db3020-95a8-4c60-8d16-13345aaf8709"
        ping.addProperty("ping", "Kotlin")


        //Start listeners

        pubNub.run {
            addListener(subscribeCallback)
            subscribe()
                .channels(Arrays.asList("hello_world")) // subscribe to channels
                .execute()
        }

    }

    //onRestart function
    override fun onRestart()
    {
        super.onRestart()
        reStartPubNubSubscribe()
    }

    //Timeout Protector
    fun timeoutProtector()
    {
        Handler().postDelayed({
            if (waitingForResponse) {error(2)}
        }, 7000)
    }


    //Restart Pubnub listener
    fun reStartPubNubSubscribe()
    {

        pubNub.run{
            removeListener(subscribeCallback)
            unsubscribeAll()
        }
        pubNub.run {
            addListener(subscribeCallback)
            subscribe()
                .channels(Arrays.asList("hello_world")) // subscribe to channels
                .execute()
        }
    }


    //Toggle Function on Toggle Button keypress
    fun toggleFunction(view: View)
    {
        val btn = view as ToggleButton
        val state = btn.isChecked
        val payload = JsonObject()
        val token: String
        var i = 0
        when (btn.id) {
            R.id.toggleButton ->  i = 0
            R.id.toggleButton2 -> i = 1
            R.id.toggleButton3 -> i = 2
            R.id.toggleButton4 -> i = 3
        }

        this@MainActivity.runOnUiThread(java.lang.Runnable {
            btn.toggle()
        })

        token = "$i"

        if (state)  { payload.addProperty(token, "o") }
        else        { payload.addProperty(token, "f") }

        publish(payload,1)
    }

    //Update UI to in progress
    fun updateInProgress (i: Int)
    {
        this@MainActivity.runOnUiThread(java.lang.Runnable {
            images[i].visibility = View.INVISIBLE
            bars[i].visibility = View.VISIBLE
            for(i in toggles) {i.setClickable(false)}
        })
        timeoutProtector()
    }


    //Handle new PubNub message
    fun handleMessage(message: String)
    {
        runOnUiThread {
            textView.run { text = ("Last message: $message") }
        }

        if (message.substring(2,8) == "status") {
            println("Status received")
            waitingForResponse = false
            if (message.contains("curState_") ) {
                val i = message.indexOf("curState_")
                val localState = message.substring(i+9,i+13)
                updateState(localState)
            }
        }
    }


    //Update current state and set UI to reflect change
    fun updateState(input: String)
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

            this@MainActivity.runOnUiThread(java.lang.Runnable {
                for (i in 0..3) {
                    toggles[i].setChecked(updatedStateArray[i])
                    for(i in toggles) {i.setClickable(true)}
                    for(i in images)  {i.visibility = View.VISIBLE}
                    for(i in bars)    {i.visibility = View.INVISIBLE}
                    state[i] = updatedStateArray[i]
                }
            })

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

        publish(msg,1)
    }


    //Publish JSON object with context if network is connected
    fun publish(msg: JsonObject, flag: Int)
    {

        val cm = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork: NetworkInfo? = cm.activeNetworkInfo
        val isConnected: Boolean = activeNetwork?.isConnectedOrConnecting == true

        if (isConnected) {
            pubNub.run {
                publish()
                    .message(msg)
                    .channel("hello_world")
                    .async(object : PNCallback<PNPublishResult>() {

                        override fun onResponse(result: PNPublishResult, status: PNStatus) {
                            if (!status.isError) {
                                if (flag == 1){
                                    val i = msg.toString()[2].toString().toInt()
                                    updateInProgress(i)
                                    waitingForResponse = true
                                }

                            } else {
                                println("Could not publish")
                                error(0)
                            }
                        }
                    })
            }
        }
        else{error(1)}
    }


    //Error Handling
    fun error(flag: Int)
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

        var lastState = ""
        for (i in state){
            if (i) {lastState += "1"}
            else {lastState += "0"}
        }

        updateState(lastState)

        val dialogBuilder = AlertDialog.Builder(this)

        dialogBuilder.setMessage(errorMessage)
        .setCancelable(false)
        .setPositiveButton(positiveMessage, DialogInterface.OnClickListener {
                dialog, id -> dialog.cancel()
                when(positiveMessage)
                {
                    "TODO" -> println("Error needs fixing")
                    "Close Application" -> finish()
                    "Reset Subscriber" -> reStartPubNubSubscribe()
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
    }
}