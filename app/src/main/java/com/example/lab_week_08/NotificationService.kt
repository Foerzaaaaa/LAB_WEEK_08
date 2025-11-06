package com.example.lab_week_08

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import android.app.PendingIntent.FLAG_IMMUTABLE

class NotificationService : Service() {
    // Create the notification builder
    private lateinit var notificationBuilder: NotificationCompat.Builder
    // System handler to control thread execution
    private lateinit var serviceHandler: Handler

    // One-way communication, return null
    override fun onBind(intent: Intent): IBinder? = null

    // Lifecycle callback - called when service is created
    override fun onCreate() {
        super.onCreate()

        // Create notification with configurations
        notificationBuilder = startForegroundService()

        // Create handler to control which thread notification executes on
        val handlerThread = HandlerThread("SecondThread")
            .apply { start() }
        serviceHandler = Handler(handlerThread.looper)
    }

    // Create notification with all configurations
    private fun startForegroundService(): NotificationCompat.Builder {
        val pendingIntent = getPendingIntent()
        val channelId = createNotificationChannel()
        val notificationBuilder = getNotificationBuilder(
            pendingIntent, channelId
        )

        // Start foreground service and show notification
        startForeground(NOTIFICATION_ID, notificationBuilder.build())
        return notificationBuilder
    }

    // Pending Intent to execute when user clicks notification
    private fun getPendingIntent(): PendingIntent {
        // Flag exists only for API 31+, check SDK version
        val flag = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            FLAG_IMMUTABLE else 0

        // Redirect to MainActivity when notification clicked
        return PendingIntent.getActivity(
            this, 0, Intent(
                this,
                MainActivity::class.java
            ), flag
        )
    }

    // Create notification channel (API 26+)
    private fun createNotificationChannel(): String =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelId = "001"
            val channelName = "001 Channel"
            // IMPORTANCE_DEFAULT - makes sound, no heads-up
            val channelPriority = NotificationManager.IMPORTANCE_DEFAULT

            val channel = NotificationChannel(
                channelId,
                channelName,
                channelPriority
            )

            // Get NotificationManager and bind channel
            val service = requireNotNull(
                ContextCompat.getSystemService(this,
                    NotificationManager::class.java)
            )
            service.createNotificationChannel(channel)

            channelId
        } else { "" }

    // Build notification with contents and configurations
    private fun getNotificationBuilder(pendingIntent: PendingIntent, channelId: String) =
        NotificationCompat.Builder(this, channelId)
            .setContentTitle("Second worker process is done")
            .setContentText("Check it out!")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setTicker("Second worker process is done, check it out!")
            .setOngoing(true)

    // Lifecycle callback - called when service starts
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val returnValue = super.onStartCommand(intent, flags, startId)

        // Get channel ID from Intent
        val Id = intent?.getStringExtra(EXTRA_ID)
            ?: throw IllegalStateException("Channel ID must be provided")

        // Post notification task to handler (different thread)
        serviceHandler.post {
            // Countdown from 10 to 0 in notification
            countDownFromTenToZero(notificationBuilder)
            // Notify MainActivity that service process is done
            notifyCompletion(Id)
            // Stop foreground service and close notification
            stopForeground(STOP_FOREGROUND_REMOVE)
            // Stop and destroy service
            stopSelf()
        }

        return returnValue
    }

    // Update notification to display countdown from 10 to 0
    private fun countDownFromTenToZero(notificationBuilder: NotificationCompat.Builder) {
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Count down from 10 to 0
        for (i in 10 downTo 0) {
            Thread.sleep(1000L)
            // Update notification content text
            notificationBuilder.setContentText("$i seconds until last warning")
                .setSilent(true)
            // Notify manager about content update
            notificationManager.notify(
                NOTIFICATION_ID,
                notificationBuilder.build()
            )
        }
    }

    // Update LiveData with returned channel ID through Main Thread
    private fun notifyCompletion(Id: String) {
        Handler(Looper.getMainLooper()).post {
            mutableID.value = Id
        }
    }

    companion object {
        const val NOTIFICATION_ID = 0xCA7
        const val EXTRA_ID = "Id"

        // LiveData to automatically update UI
        private val mutableID = MutableLiveData<String>()
        val trackingCompletion: LiveData<String> = mutableID
    }
}
