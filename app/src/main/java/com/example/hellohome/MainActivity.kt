package com.example.hellohome

import android.app.AlertDialog
import android.content.DialogInterface
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.view.ContextThemeWrapper
import com.pubnub.api.PubNub
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
import android.support.constraint.ConstraintSet
import android.support.constraint.ConstraintLayout
import android.view.Gravity
import com.victor.loading.rotate.RotateLoading



//TODO: CHECK FOR TODOs IN THE CODE
//TODO: CHANGE CURSTATE TO JUST STATE
//TODO: AUTOMATIC DIAGNOSTIC AND FAILURE RESOLVE (POP UP DIALOG OR ETC)
//TODO: FIX ANOMALOUS BEHAVIOR OF THE SYNC, SOMETIMES IT FREEZES
//      -ADD HANDLER CALLBACK REMOVER SO THAT AFTER SYNC IS FINISHED THERE ARE NO EXTRANEOUS CALLS
//TODO: CATCHING EXCEPTIONS FOR OUTLETS, WOL AND MA
//TODO: FIGURE OUT WHAT TO DO WITH PROTOTYPE BOARD BUTTON - MAYBE JUST STATUS?
//TODO: LOTS OF CLEANUP


class MainActivity : AppCompatActivity() {

    inner class Publish
    {
        val value = { value: Boolean ->
            when (value) {
                true -> "o"
                else -> "f"
            }
        }

        fun ping() {
            pub("ping", "Android Home v0.8")
            hardware.mkrUptime = ""
            Run.after(3000) {
                if (hardware.mkrUptime == "") {
                    hardware.syncFailed(2)
                }
            }
        }

        fun requestState() {
            pub("req", "STATE_MKR1000")
        }

        fun MKR1000_requestStatus() {
            pub("req", "STATUS_MKR1000")
            hardware.setMkrStat(false)
            Run.after(3000) {
                if (!hardware.checkLatch()) {
                    hardware.syncFailed(1)
                }
            }
        }
        fun MKR1000_requestReset() {
            pub("req", "RST_MKR1000")
            lock(true)
        }

        fun ESP_requestStatus() {
            pub("req", "STATUS_ESP")
            hardware.setEspStat(false)
            Run.after(3000) {
                if (!hardware.espOk) {
                    hardware.syncFailed(0)
                }
            }
        }

        fun ESP_requestReset() {
            pub("req", "RST_ESP")
            lock(true)
        }

        fun wolPC() {
            pub("WOL", "PC")
        }

        fun masterCommand() {
            pub("A", "A")
        }

        fun command(id: Int, state: Boolean) {
            pub("$id", value(state))
            runOnUiThread{
                outlets[id].update(true, state)
            }
        }

        fun pub(property: String, value: String) {
            val msg = JsonObject()
            msg.addProperty(property, value)
            updateTextField("Publishing:     $msg")
            pubNub.publish(msg)
        }
    }

    inner class Hardware
    {
        var mkrUptime = ""
        var latency = ""
        private var sync = false
        private var latch = false
        var mkrOk = false
        var espOk = false
        private var state = mutableListOf(false, false, false, false)

        fun setMkrStat(status:Boolean) {
            mkrOk = status
            checkLatch()
        }

        fun setEspStat(status:Boolean) {
            espOk = status
            checkLatch()
        }

        fun checkLatch(): Boolean  {
            latch = mkrOk && espOk
            sync = latch
            return latch
        }

        fun waitingForResponse(): Boolean {
            var waiting = false
            for (outlet in outlets) {
                waiting = waiting || outlet.updating
            }
            for (button in buttons) {
                waiting = waiting || !button.isClickable
            }
            return waiting
        }

        fun startSync() {
            runOnUiThread {
                val loadingCircle: RotateLoading = findViewById(R.id.syncLoading)
                loadingCircle.start()
                syncButton.visibility = View.GONE
                syncButton.isClickable = false
                lock(true)
                sync = true
                send.ESP_requestStatus()
            }
        }

        fun syncFailed(error: Int) {
            runOnUiThread {
                Run.cancelAll()
                val loadingCircle: RotateLoading = findViewById(R.id.syncLoading)
                loadingCircle.stop()
                syncButton.setImageResource(R.drawable.syncproblem)
                syncButton.visibility = View.VISIBLE
                syncButton.isClickable = true
                //errorOccured(2)
                toast("Sync Failed: Error $error")
            }
        }

        fun syncOK() {
            runOnUiThread {
                Run.cancelAll()
                val loadingCircle: RotateLoading = findViewById(R.id.syncLoading)
                loadingCircle.stop()
                syncButton.setImageResource(R.drawable.sync)
                syncButton.visibility = View.VISIBLE
                lock(false)
            }
        }

        fun newState(message: String) {
            val latestState = mutableListOf<Boolean>()
            for (i in message) {
                when (i) {
                    '0' -> latestState.add(false)
                    '1' -> latestState.add(true)
                }
            }
            state = latestState

            for (outlet in outlets) {
                outlet.setsState(state[outlet.id])
            }

            runOnUiThread {
                val indicator = findViewById<View>(R.id.masterIndicator)
                val button: ImageButton = findViewById(R.id.masterButton)
                when (state[0] || state[1] || state[2] || state[3]) {
                    true  -> indicator.setBackgroundColor(getColor(R.color.on))
                    false -> indicator.setBackgroundColor(getColor(R.color.off))
                }
                button.isClickable = true
            }
        }

        fun newStatus(message: String) {
            when {
                message == "MKR1000 OK" -> {                           //MKR1000 CONNECTION OK
                    setMkrStat(true)
                    Run.after(300) {
                        send.ping()
                    }
                }
                message == "MKR1000 initialized." ->                   //MKR1000 INITIALIZED
                    runOnUiThread {
                        Handler().postDelayed({
                            send.MKR1000_requestStatus()
                        }, 300)
                    }
                message == "MKR1000_RST" ->                            //MKR1000 RESET CONFIRMED
                    toast("MKR1000 Reset Confirmed")
                message == "ESP8266_SUB initialized." ->               //ESP INITIALIZED
                    runOnUiThread {
                        Handler().postDelayed({
                            send.ESP_requestStatus()
                        }, 300)
                    }
                message == "ESP OK" -> {                               //ESP CONNECTION OK
                    setEspStat(true)
                    Run.after(300) {
                        send.MKR1000_requestStatus()
                    }
                }
                message.contains("ESP_RST", false) ->  //ESP RESET CONFIRMED
                    toast("ESP Reset Confirmed")
                message == "WOL_ACK" -> {                              //WOL REQ ACK
                    toast("WOL Success")
                    val button: ImageButton = findViewById(R.id.wolButton)
                    button.isClickable = true
                }
            }
        }

        fun newPing(message: String) {

        }
    }

    var send = Publish()
    var hardware = Hardware()
    private var outlets = arrayListOf<Outlet>()
    private var buttons = mutableListOf<ImageButton>()

    //onCreate function
    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        //Activity specifc variables
        val button0: CompoundButton = findViewById(R.id.toggleButton)
        val button1: CompoundButton = findViewById(R.id.toggleButton2)
        val button2: CompoundButton = findViewById(R.id.toggleButton3)
        val button3: CompoundButton = findViewById(R.id.toggleButton4)

        val image0:  ImageView = findViewById(R.id.imageView)
        val image1:  ImageView = findViewById(R.id.imageView2)
        val image2:  ImageView = findViewById(R.id.imageView3)
        val image3:  ImageView = findViewById(R.id.imageView4)

        val bar0:    CircularProgressBar = findViewById(R.id.circularProgressBar)
        val bar1:    CircularProgressBar = findViewById(R.id.circularProgressBar2)
        val bar2:    CircularProgressBar = findViewById(R.id.circularProgressBar3)
        val bar3:    CircularProgressBar = findViewById(R.id.circularProgressBar4)

        val arduinoButton: ImageButton = findViewById(R.id.arduinoButton)
        val masterButton:  ImageButton = findViewById(R.id.masterButton)
        val wolButton:     ImageButton = findViewById(R.id.wolButton)
        val syncButton:    ImageButton = findViewById(R.id.syncButton)

        //Initialize activity related variables

        outlets.add(Outlet(0, UIObject(button0,image0,bar0)))
        outlets.add(Outlet(1, UIObject(button1,image1,bar1)))
        outlets.add(Outlet(2, UIObject(button2,image2,bar2)))
        outlets.add(Outlet(3, UIObject(button3,image3,bar3)))

        buttons.add(arduinoButton)
        buttons.add(masterButton)
        buttons.add(wolButton)
        buttons.add(syncButton)

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
                if (status.category == PNStatusCategory.PNConnectedCategory) {
                    hardware.startSync()
                }
            }

            override fun message(pubnub: PubNub, message: PNMessageResult) {
                runOnUiThread{handleMessage(message.message.toString())}
            }

            override fun presence(pubnub: PubNub, presence: PNPresenceEventResult) {}
        }
        pubNub.configure(subscribeCallback)
        pubNub.subscribe()
    }


    //onRestart function
    override fun onRestart()
    {
        super.onRestart()
        pubNub.restartSubscriber()
    }


    //Handle new subscribe event
    fun handleMessage(message: String)
    {
        println("Received:       $message")

        when {
            message.contains("status", false) -> {
                //NEW STATUS
                hardware.newStatus(message.substring(11,message.lastIndex-1))
                updateTextField("Received:       $message")

            }
            message.contains("curState", true) -> {
                //NEW STATE
                hardware.newState(message.substring(13, 17))
                updateTextField("Received:       $message")
                hardware.syncOK()

            }
            message.contains("ack", true) -> {
                //PING ACK
                hardware.newPing(message.substring(message.indexOf('T'),message.lastIndex-1))
                updateTextField("Received:       $message")
                Run.after(300) {
                    send.requestState()
                }
            }
        }
    }

    fun debugMode(view: View)
    {
        val debugLayout: LinearLayout = findViewById(R.id.debugLayout)
        val bias: Float
        if (debugLayout.visibility == View.VISIBLE) {
            debugLayout.visibility = View.GONE
            bias = .50f
        } else {
            bias = .38f
            debugLayout.visibility = View.VISIBLE
        }
        val cl = findViewById<ConstraintLayout>(R.id.constraintLayout)
        val cs = ConstraintSet()
        cs.clone(cl)
        cs.setVerticalBias(R.id.toggleLayout, bias)
        cs.applyTo(cl)
    }


    //Toggle Function on Toggle Button keypress
    fun toggleFunction(view: View)
    {
        val button: ToggleButton = view as ToggleButton
        var i = 0
        when (button.id) {
            R.id.toggleButton ->  i = 0
            R.id.toggleButton2 -> i = 1
            R.id.toggleButton3 -> i = 2
            R.id.toggleButton4 -> i = 3
        }
        send.command(i, button.isChecked)
        runOnUiThread { button.toggle() }
    }


    private fun lock(lock: Boolean)
    {
        runOnUiThread {
            for (i in 0..3) {
                outlets[i].ui.button.isClickable = !lock
            }
            for (button in buttons) {
                button.isClickable = !lock
            }
        }
    }


    private fun updateTextField(newMessage: String)
    {
        runOnUiThread {
            textView.run { append("$newMessage\n") }
            scrollView.run {fullScroll(View.FOCUS_DOWN)}
        }
    }

    //Update current state



    //Handle press for manual message publishing
    fun debugOnClick(view: View) {

        val button =  view as Button

        when (button.hint as String) {

            "Zero ON"    -> send.pub("0", "o")
            "Zero OFF"   -> send.pub("0", "f")
            "One ON"     -> send.pub("1", "o")
            "One OFF"    -> send.pub("1", "f")
            "Two ON"     -> send.pub("2", "o")
            "Two OFF"    -> send.pub("2", "f")
            "Three ON"   -> send.pub("3", "o")
            "Three OFF"  -> send.pub("3", "f")
            "Toggle All" -> send.pub("A", "A")
        }

    }

    fun masterOnClick(view: View) {
        val button: ImageButton = view as ImageButton
        button.isClickable = false
        send.masterCommand()
    }

    fun wolOnClick(view: View) {
        val button: ImageButton = view as ImageButton
        button.isClickable = false
        toast("Requesting WOL packet...")
        send.wolPC()
    }

    fun syncOnClick(view: View) {
        hardware.startSync()
    }

    fun ping (view: View) {
        val msg = JsonObject()
        msg.addProperty("ping", "Custom Ping Android")
        pubNub.publish(msg)
    }

    fun state (view: View) {
        val msg = JsonObject()
        msg.addProperty("req", "STATE_MKR1000")
        pubNub.publish(msg)
    }

    fun showPopUp(view: View) {
        val wrapper = ContextThemeWrapper(this, R.style.BasePopupMenu)
        val popupMenu = PopupMenu(wrapper, view)
        val inflater = popupMenu.menuInflater
        inflater.inflate(R.menu.header_menu, popupMenu.menu)
        popupMenu.show()
        var id = 0
        when (view.id) {
            R.id.arduinoButton -> {
                val button: ImageButton = view as ImageButton
                button.isClickable = false
            }
            R.id.MKR1000 -> id = 1
            R.id.ESP     -> id = 2
        }

        fun status(id: Int) {
            when (id) {
                0 -> toast("Requesting status Not Available")
                1 -> {
                    val msg = JsonObject()
                    msg.addProperty("req", "STATUS_MKR1000")
                    pubNub.publish(msg)
                }
                2 -> {
                    val msg = JsonObject()
                    msg.addProperty("req", "STATUS_ESP")
                    pubNub.publish(msg)
                }
            }

        }

        fun reset(id: Int) {
            when (id) {
                0 -> toast("Requesting Reset Not Available")
                1 -> {
                    val msg = JsonObject()
                    msg.addProperty("req", "RST_MKR1000")
                    pubNub.publish(msg)
                }
                2 -> {
                    toast("ESP Reset Not Available")
                }
            }
        }

        popupMenu.setOnMenuItemClickListener {
            when(it.itemId) {
                R.id.header1 -> {
                    status(id)
                }
                R.id.header2 -> {
                    reset(id)
                }
            }
            true
        }
    }

    private fun toast(text: String) {
        val toast = Toast.makeText(this@MainActivity, text, Toast.LENGTH_SHORT)
        toast.setGravity(Gravity.BOTTOM, 0,220)
        toast.show()
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

      */
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
                    "Reset Subscriber" -> pubNub.restartSubscriber()
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