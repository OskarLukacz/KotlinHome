package com.example.hellohome

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import android.widget.TextView
import android.view.View
import android.widget.Button
import com.google.gson.JsonObject
import java.util.Arrays


class MainActivity : AppCompatActivity() {

    val pnConfiguration = PNConfiguration()
    val pubNub = PubNub(pnConfiguration)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        pnConfiguration.subscribeKey = "sub-c-05dce56c-3c2e-11e7-847e-02ee2ddab7fe"
        pnConfiguration.publishKey = "pub-c-b6db3020-95a8-4c60-8d16-13345aaf8709"

        val subscribeText = findViewById<TextView>(R.id.textView)
        val subscribeCallback: SubscribeCallback = object : SubscribeCallback()  {
            override fun status(pubnub: PubNub, status: PNStatus) {
            }
            override fun message(pubnub: PubNub, message: PNMessageResult) {
                runOnUiThread {
                    subscribeText.run { text = ("Last message: " + message.message.toString()) }
                }
            }
            override fun presence(pubnub: PubNub, presence: PNPresenceEventResult) {
            }
        }

        pubNub.run {
            addListener(subscribeCallback)
            subscribe()
                .channels(Arrays.asList("hello_world")) // subscribe to channels
                .execute()
        }

    }


    fun publish(view: View)
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

        pubNub.run {
            publish()
                .message(msg)
                .channel("hello_world")
                .async(object : PNCallback<PNPublishResult>() {
                    override fun onResponse(result: PNPublishResult, status: PNStatus) {
                        if (!status.isError) {
                            println(msg.toString())
                        }else {
                            println("Could not publish")
                        }
                    }
                })
        }
    }


}



