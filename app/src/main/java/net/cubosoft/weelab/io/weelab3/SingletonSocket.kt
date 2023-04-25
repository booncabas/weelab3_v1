package net.cubosoft.weelab.io.weelab3

import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.SHARED_PREF_FILE_NAME
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.TEST_TS
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_CHANNEL_ID
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_LAST_ALARM_CREATED_MILLIS
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_LAST_UPDATE_WIDGET_MILLIS
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_NOTIFICATION_ID
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_TAG
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_WS_CONNECT_STRING
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_WS_URL
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import java.net.URI
import java.util.*

open class SingletonSocket<out T: Any, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg: A): T {
        val checkInstance = instance
        if (checkInstance != null) {
            return checkInstance
        }

        return synchronized(this) {
            val checkInstanceAgain = instance
            if (checkInstanceAgain != null) {
                checkInstanceAgain
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}

class SocketManager private constructor(private var context: Context) {
    companion object : SingletonSocket<SocketManager, Context>(::SocketManager)

    private val prefs: SharedPreferences = context.getSharedPreferences(WeelabConst.SHARED_PREF_FILE_NAME, 0)
    private var lastAlarmCreatedTime = prefs.getLong(WEELAB_LAST_ALARM_CREATED_MILLIS, 0L)
    private var prefEditor = prefs.edit()

    private var mNotificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var webSocketClient: WebSocketClient

    fun initWebSocket() {
        try {
            val websocketUri = URI(WEELAB_WS_URL)
            createWebSocketClient(websocketUri)
//            val socketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
//            webSocketClient.setSocketFactory(socketFactory)
            webSocketClient.connect()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun createWebSocketClient(websocketUri: URI?) {
        webSocketClient = object : WebSocketClient(websocketUri) {

            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.w(WEELAB_TAG, "onOpen")
                webSocketClient.send(WEELAB_WS_CONNECT_STRING)
            }


            override fun onMessage(message: String?) {//working
                if (message.toString().isNotEmpty()){
                    checkAlarmOwner(message.toString())
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.w(WEELAB_TAG, "onClose $reason")
            }

            override fun onError(ex: Exception?) {
                Log.w(WEELAB_TAG, "onError: ${ex?.message} in ${Thread.currentThread().name}")
            }
        }
    }

    fun isSocketOpen(): Boolean{
        return webSocketClient.isOpen
    }

    fun checkAlarmOwner(jsonString: String) = runBlocking {
        launch(Dispatchers.IO) {
            var deviceName: String
            var deviceId = ""
            var typeAlarm = ""
            var alarmCreatedTime = 0L
            Log.w(
                WEELAB_TAG,
                "checkAlarmOwner()_in_name_${Thread.currentThread().name}_id_${Thread.currentThread().id}"
            )
            try {
                val root = JSONObject(jsonString)
                val originator = root.getJSONObject("originator")
                deviceId = originator.getString("id")
                typeAlarm = root.getString("name")
                alarmCreatedTime = root.getLong("createdTime")
            } catch (e: JSONException) {
//                e.printStackTrace()
            }
            if (deviceId.isNotEmpty()) {
                if (alarmCreatedTime > 0L) {
                    lastAlarmCreatedTime = prefs.getLong(WEELAB_LAST_ALARM_CREATED_MILLIS, 0L)
                    if (lastAlarmCreatedTime != alarmCreatedTime) {
                        Log.w(WEELAB_TAG, "*****Last_$lastAlarmCreatedTime")
                        Log.w(WEELAB_TAG, "******New_$alarmCreatedTime")
                        prefEditor.putLong(WEELAB_LAST_ALARM_CREATED_MILLIS, alarmCreatedTime)
                        prefEditor.commit()
                        val tbrest = TBRest(context)
                        tbrest.getListDevicesIds()
                        val list = tbrest.getListDevicesIds()
                        for (i in 0 until list.size) {
                            try {
                                if (list[i] == deviceId) {
                                    deviceName = tbrest.getSingleDeviceName(deviceId)
                                    Log.w(WEELAB_TAG, "=>Notification")
                                    deliverNotification(
                                        deviceName,
                                        context.getString(R.string.alarm_text_intro) + " " + typeAlarm
                                    )
                                    val lastUpdateWidgetMillis = prefs.getLong(WEELAB_LAST_UPDATE_WIDGET_MILLIS, 0L)
                                    if (System.currentTimeMillis() >= lastUpdateWidgetMillis + 60000) {
                                        Log.w(WEELAB_TAG, "=>Update_widget")
                                        sendBroadcastUpdate()
                                    }
                                }
                            } catch (e: InterruptedException) {
                                Log.w(WEELAB_TAG, "Something went wrong: $e")
                            }
                        }//end_for
                    }
                }
            } else {
                Log.w(WEELAB_TAG, "NO_ID")
            }
        }
    }


    private fun deliverNotification(title: String, message: String) {
        // Create the content intent for the notification, which launches this activity
        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            WEELAB_NOTIFICATION_ID,
            contentIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        // Build the notification
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, WEELAB_CHANNEL_ID)
                .setSmallIcon(R.drawable.weelab_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(contentPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
        mNotificationManager.notify(WEELAB_NOTIFICATION_ID, builder.build())
    }

    fun sendBroadcastUpdate(){
        val intent = Intent(context, AppWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(context.applicationContext).getAppWidgetIds(
            ComponentName(context.applicationContext, AppWidget::class.java)
        )
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

}

/*
package net.cubosoft.weelab.io.weelab3

import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_CHANNEL_ID
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_LAST_ALARM_CREATED_MILLIS
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_LAST_UPDATE_WIDGET_MILLIS
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_NOTIFICATION_ID
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_TAG
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_WS_CONNECT_STRING
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_WS_URL
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import org.json.JSONException
import org.json.JSONObject
import java.net.URI

open class SingletonSocket<out T: Any, in A>(creator: (A) -> T) {
    private var creator: ((A) -> T)? = creator
    @Volatile private var instance: T? = null

    fun getInstance(arg: A): T {
        val checkInstance = instance
        if (checkInstance != null) {
            return checkInstance
        }

        return synchronized(this) {
            val checkInstanceAgain = instance
            if (checkInstanceAgain != null) {
                checkInstanceAgain
            } else {
                val created = creator!!(arg)
                instance = created
                creator = null
                created
            }
        }
    }
}

class SocketManager private constructor(private var context: Context) {
    companion object : SingletonSocket<SocketManager, Context>(::SocketManager)

    private val prefs: SharedPreferences = context.getSharedPreferences(WeelabConst.SHARED_PREF_FILE_NAME, 0)
    private var lastAlarmCreatedTime = prefs.getLong(WEELAB_LAST_ALARM_CREATED_MILLIS, 0L)
    private var prefEditor = prefs.edit()

    private var mNotificationManager: NotificationManager =
        context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private lateinit var webSocketClient: WebSocketClient

    fun initWebSocket() {
        try {
            val websocketUri = URI(WEELAB_WS_URL)
            createWebSocketClient(websocketUri)
//            val socketFactory: SSLSocketFactory = SSLSocketFactory.getDefault() as SSLSocketFactory
//            webSocketClient.setSocketFactory(socketFactory)
            webSocketClient.connect()
        }catch (e: Exception){
            e.printStackTrace()
        }
    }

    private fun createWebSocketClient(websocketUri: URI?) {
        webSocketClient = object : WebSocketClient(websocketUri) {

            override fun onOpen(handshakedata: ServerHandshake?) {
                Log.w(WEELAB_TAG, "onOpen")
                webSocketClient.send(WEELAB_WS_CONNECT_STRING)
            }


            override fun onMessage(message: String?) {//working
                if (message.toString().isNotEmpty()){
                    checkAlarmOwner(message.toString())
                }
            }

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                Log.w(WEELAB_TAG, "onClose $reason")
            }

            override fun onError(ex: Exception?) {
                Log.w(WEELAB_TAG, "onError: ${ex?.message} in ${Thread.currentThread().name}")
            }
        }
    }

    fun isSocketOpen(): Boolean{
        return webSocketClient.isOpen
    }

    fun checkAlarmOwner(jsonString: String) = runBlocking {
        launch(Dispatchers.IO) {
            var deviceName: String
            var deviceId = ""
            var typeAlarm = ""
            var alarmCreatedTime = 0L
            Log.w(
                WEELAB_TAG,
                "checkAlarmOwner()_in_name_${Thread.currentThread().name}_id_${Thread.currentThread().id}"
            )
            try {
                val root = JSONObject(jsonString)
                val originator = root.getJSONObject("originator")
                deviceId = originator.getString("id")
                typeAlarm = root.getString("name")
                alarmCreatedTime = root.getLong("createdTime")
            } catch (e: JSONException) {
//                e.printStackTrace()
            }
            if (deviceId.isNotEmpty()) {
                if (alarmCreatedTime > 0L) {
                    lastAlarmCreatedTime = prefs.getLong(WEELAB_LAST_ALARM_CREATED_MILLIS, 0L)
                    if (lastAlarmCreatedTime != alarmCreatedTime) {
                        Log.w(WEELAB_TAG, "*****Last_$lastAlarmCreatedTime")
                        Log.w(WEELAB_TAG, "******New_$alarmCreatedTime")
                        prefEditor.putLong(WEELAB_LAST_ALARM_CREATED_MILLIS, alarmCreatedTime)
                        prefEditor.commit()
                        val tbrest = TBRest(context)
                        tbrest.getListDevicesIds()
                        val list = tbrest.getListDevicesIds()
                        for (i in 0 until list.size) {
                            try {
                                if (list[i] == deviceId) {
                                    deviceName = tbrest.getSingleDeviceName(deviceId)
                                    Log.w(WEELAB_TAG, "=>Notification")
                                    deliverNotification(
                                        deviceName,
                                        context.getString(R.string.alarm_text_intro) + " " + typeAlarm
                                    )
                                    val lastUpdateWidgetMillis = prefs.getLong(WEELAB_LAST_UPDATE_WIDGET_MILLIS, 0L)
                                    if (System.currentTimeMillis() >= lastUpdateWidgetMillis + 60000) {
                                        Log.w(WEELAB_TAG, "=>Update_widget")
                                        sendBroadcastUpdate()
                                    }
                                }
                            } catch (e: InterruptedException) {
                                Log.w(WEELAB_TAG, "Something went wrong: $e")
                            }
                        }//end_for
                    }
                }
            } else {
                Log.w(WEELAB_TAG, "NO_ID")
            }
        }
    }


    private fun deliverNotification(title: String, message: String) {
        // Create the content intent for the notification, which launches this activity
        val contentIntent = Intent(context, MainActivity::class.java)
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            WEELAB_NOTIFICATION_ID,
            contentIntent,
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        // Build the notification
        val builder: NotificationCompat.Builder =
            NotificationCompat.Builder(context, WEELAB_CHANNEL_ID)
                .setSmallIcon(R.drawable.weelab_icon)
                .setContentTitle(title)
                .setContentText(message)
                .setContentIntent(contentPendingIntent)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
        mNotificationManager.notify(WEELAB_NOTIFICATION_ID, builder.build())
    }

//    fun sendBroadcastUpdate(){// test boon
fun sendBroadcastUpdate(){
        val intent = Intent(context, AppWidget::class.java)
        intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val ids = AppWidgetManager.getInstance(context.applicationContext).getAppWidgetIds(
            ComponentName(context.applicationContext, AppWidget::class.java)
        )
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
        context.sendBroadcast(intent)
    }

}
*/
