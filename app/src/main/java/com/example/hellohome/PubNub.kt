package com.example.hellohome

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.support.v4.content.ContextCompat.getSystemService
import com.google.gson.JsonObject
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.enums.PNStatusCategory
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus
import com.pubnub.api.models.consumer.pubsub.PNMessageResult
import com.pubnub.api.models.consumer.pubsub.PNPresenceEventResult
import kotlinx.android.synthetic.main.activity_main.*
import java.util.*

var pubNub = PubNubInterface()

class PubNubInterface {

    private val pnConfiguration = PNConfiguration()
    private val pubNub = PubNub(pnConfiguration)
    val ping: JsonObject = JsonObject()
    private lateinit var callback: SubscribeCallback

    fun configure(subscribeCallback: SubscribeCallback) {
        pnConfiguration.subscribeKey = "sub-c-05dce56c-3c2e-11e7-847e-02ee2ddab7fe"
        pnConfiguration.publishKey = "pub-c-b6db3020-95a8-4c60-8d16-13345aaf8709"
        ping.addProperty("ping", "Android Home v0.8")
        callback = subscribeCallback

    }

    fun subscribe () {
        pubNub.run {
            addListener(callback)
            subscribe()
                .channels(Arrays.asList("hello_world")) // subscribe to channels
                .execute()
        }
    }

    fun reStartPubNubSubscribe() {

        pubNub.run {
            removeListener(callback)
            unsubscribeAll()
        }
        pubNub.run {
            addListener(callback)
            subscribe()
                .channels(Arrays.asList("hello_world")) // subscribe to channels
                .execute()
        }
    }

    fun publish(msg: JsonObject, flag: Int) {
        pubNub.run {
            publish()
                .message(msg)
                .channel("hello_world")
                .async(object : PNCallback<PNPublishResult>() {
                    override fun onResponse(result: PNPublishResult, status: PNStatus) {
                        if (!status.isError) {
                            if (flag == 1) {
                                println("New status published")
                            }
                        } else {
                            println("Could not publish")
                            error(0)
                        }
                    }
                })
        }
    }
}

