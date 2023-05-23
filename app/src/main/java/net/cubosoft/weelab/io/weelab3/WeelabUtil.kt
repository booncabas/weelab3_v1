package net.cubosoft.weelab.io.weelab3

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Context.CONNECTIVITY_SERVICE
import android.content.Context.NOTIFICATION_SERVICE
import android.graphics.Color
import android.net.ConnectivityManager
import android.os.Build
import android.util.Log
import androidx.work.*
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_CHANNEL_ID
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_CHANNEL_NAME
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_WORK_TAG
import java.util.concurrent.TimeUnit

class WeelabUtil(private val context: Context) {

    private var mNotificationManager: NotificationManager? = null
    private var connManager: ConnectivityManager? = null

    fun setSocketToWork() {
        val constraints: Constraints = Constraints.Builder()
            .setRequiresCharging(false)
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(false)
            .build()

        val socketWorkRequest = PeriodicWorkRequestBuilder<WeelabWorker>(15, TimeUnit.MINUTES)
            .addTag(WEELAB_WORK_TAG)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context.applicationContext).cancelAllWork()
        WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
            "weelab_work_socket_main",
            ExistingPeriodicWorkPolicy.REPLACE,
            socketWorkRequest
        )
        Log.i("WorkInfo", WorkManager.getInstance(context.applicationContext).toString())
    }
    //////////////////////////////////////


    //////////////////////////////////////

    /*fun createNotificationChannel() {
        mNotificationManager = context.applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // Notification channels are only available in OREO and higher.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Create the NotificationChannel with all the parameters.
            val notificationChannel = NotificationChannel(
                WEELAB_CHANNEL_ID,
                WEELAB_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            )
            notificationChannel.enableLights(true)
            notificationChannel.lightColor = Color.RED
            notificationChannel.enableVibration(true)
            notificationChannel.description = "Notifies when there is an alarm"
            mNotificationManager!!.createNotificationChannel(notificationChannel)
        }
    }*/

    fun isDeviceOnline(context: Context): Boolean {
        connManager = context.applicationContext.getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCapabilities = connManager!!.getNetworkCapabilities(connManager!!.activeNetwork)
        return if (networkCapabilities == null) {
            Log.d("tagLog", "Device Offline")
            false
        } else {
            Log.d("tagLog", "Device Online")
            true
        }

    }

}