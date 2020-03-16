package com.example.hellohome

import android.app.AlertDialog
import android.content.DialogInterface
import android.os.Handler

val handler = Handler()

class Run {
    companion object {
        fun after(delay: Long, process: () -> Unit) {
            handler.postDelayed({
                process()
            }, delay)
        }

        fun cancelAll() {
            handler.removeCallbacksAndMessages(null)
        }
    }
}

/*fun errorOccured(flag: Int)
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