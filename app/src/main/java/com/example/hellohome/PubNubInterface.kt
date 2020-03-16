package com.example.hellohome

import com.google.gson.JsonObject
import com.pubnub.api.PNConfiguration
import com.pubnub.api.PubNub
import com.pubnub.api.callbacks.PNCallback
import com.pubnub.api.callbacks.SubscribeCallback
import com.pubnub.api.models.consumer.PNPublishResult
import com.pubnub.api.models.consumer.PNStatus

var pubNub = PubNubInterface()

class PubNubInterface {
    private val pnConfiguration = PNConfiguration()
    private val pubNub = PubNub(pnConfiguration)
    private lateinit var callback: SubscribeCallback

    fun configure(subscribeCallback: SubscribeCallback) {
        pnConfiguration.subscribeKey = "sub-c-05dce56c-3c2e-11e7-847e-02ee2ddab7fe"
        pnConfiguration.publishKey = "pub-c-b6db3020-95a8-4c60-8d16-13345aaf8709"
        callback = subscribeCallback
    }

    fun subscribe () {
        pubNub.run {
            addListener(callback)
            subscribe()
                .channels(listOf("hello_world")) // subscribe to channels
                .execute()
        }
    }

    fun restartSubscriber() {

        pubNub.run {
            removeListener(callback)
            unsubscribeAll()
        }
        pubNub.run {
            addListener(callback)
            subscribe()
                .channels(listOf("hello_world")) // subscribe to channels
                .execute()
        }
    }

    fun publish(msg: JsonObject) {
        pubNub.run {
            publish()
                .message(msg)
                .channel("hello_world")
                .async(object : PNCallback<PNPublishResult>() {
                    override fun onResponse(result: PNPublishResult, status: PNStatus) {
                        if (!status.isError) {
                            println("Publishing:     $msg")
                        } else {
                            println("ERROR: Could not publish: $msg     -   PubNub error")
                        }
                    }
                })
        }
    }
}