package net.cubosoft.weelab.io.weelab3

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Build.VERSION.SDK_INT
import android.text.format.DateFormat
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.SHARED_PREF_FILE_NAME
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_LAST_UPDATE_WIDGET_MILLIS
import java.text.SimpleDateFormat
import java.util.*

class AppWidget : AppWidgetProvider() {

    private fun partialUpdateAppWidgetProgressBar(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int ) {
        Log.w(WeelabConst.WEELAB_TAG,"PARTIAL_UPDATE_PROGRESS_BAR")
        val remoteViews = RemoteViews(context.packageName, R.layout.app_widget)
        remoteViews.setViewVisibility(R.id.button_update, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.progress_bar, View.GONE)
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)
    }

    private fun partialUpdateAppWidgetNoToken(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int ) {
        Log.w(WeelabConst.WEELAB_TAG,"PARTIAL_UPDATE")
        val remoteViews = RemoteViews(context.packageName, R.layout.app_widget)
        remoteViews.setViewVisibility(R.id.button_update, View.VISIBLE)
        remoteViews.setViewVisibility(R.id.progress_bar, View.GONE)
        remoteViews.setTextViewText(R.id.appwidget_tv_alarms_count, " ")
        remoteViews.setImageViewResource(R.id.appwidget_img_alarm_count, R.drawable.baseline_no_accounts_24)
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, remoteViews)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int ) {
        Log.w(WeelabConst.WEELAB_TAG,"UPDATE_WIDGET")
        val views = RemoteViews(context.packageName, R.layout.app_widget)
        ////////////////////////////////////////////
        val arrayOfDays = daysOfWeek()
        views.setTextViewText(R.id.day_1, arrayOfDays[0])
        views.setTextViewText(R.id.day_2, arrayOfDays[1])
        views.setTextViewText(R.id.day_3, arrayOfDays[2])
        views.setTextViewText(R.id.day_4, arrayOfDays[3])
        views.setTextViewText(R.id.day_5, arrayOfDays[4])
        views.setTextViewText(R.id.day_6, arrayOfDays[5])
        views.setTextViewText(R.id.day_7, arrayOfDays[6])
        val nowMillis = System.currentTimeMillis()
        val dateString = getLocalizedHHMMStamp(context, nowMillis)
        views.setRemoteAdapter( R.id.widget_list, Intent(context.applicationContext, WidgetService::class.java)) //put list adapter
        views.setTextViewText(R.id.appwidget_last_update_hour, " $dateString")
        val countAlarms = checkActiveAlarms(context.applicationContext)
        views.setTextViewText(R.id.appwidget_tv_alarms_count, " $countAlarms")
        if (countAlarms > 0) {
            views.setTextColor(R.id.appwidget_tv_alarms_count, Color.parseColor("#ff0000"))
            views.setImageViewResource(R.id.appwidget_img_alarm_count, R.drawable.bell_red)
        } else if(countAlarms == 0) {
            views.setImageViewResource(R.id.appwidget_img_alarm_count, R.drawable.bell_gray)
            views.setTextColor(R.id.appwidget_tv_alarms_count, Color.parseColor("#bebebe"))
        } else{
            views.setTextViewText(R.id.appwidget_tv_alarms_count, " ...")
            views.setTextColor(R.id.appwidget_tv_alarms_count, Color.parseColor("#bebebe"))
        }
        ////////////////////////////////////////////
        views.setViewVisibility(R.id.button_update, View.GONE)
        views.setViewVisibility(R.id.progress_bar, View.VISIBLE)
        /////////////////////////////////////////// Setup update button
        val intentUpdate = Intent(context.applicationContext, AppWidget::class.java)
        intentUpdate.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        val idArray = intArrayOf(appWidgetId)
        intentUpdate.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, idArray)
        val pendingUpdate = PendingIntent.getBroadcast(
            context.applicationContext, appWidgetId, intentUpdate,
            if (SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
        )
        //////////////////////////////////////
        val pendingIntent: PendingIntent = PendingIntent.getActivity(
            /* context = */ context.applicationContext,
            /* requestCode = */  0,
            /* intent = */ Intent(context, MainActivity::class.java),
            /* flags = */ PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        //////////////////////////////////////
        views.setOnClickPendingIntent(R.id.layout_intent_main, pendingIntent)
        views.setOnClickPendingIntent(R.id.layout_refresh, pendingUpdate)
        //////////////////////////////////////////////// update
        appWidgetManager.updateAppWidget(appWidgetId, views)
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_list)
        ////////////////////////////////////////////////
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        // There may be multiple widgets active, so update all of them
        val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREF_FILE_NAME, 0)
        val lastUpdatedWidgetInMillis = prefs.getLong(WEELAB_LAST_UPDATE_WIDGET_MILLIS, 0L)
        if(lastUpdatedWidgetInMillis == 0L || System.currentTimeMillis() >= lastUpdatedWidgetInMillis + 6000) {//least 1 min
            if (checkNetworkAndServer(context) > -1) {
                val prefEditor = prefs.edit()
                prefEditor.putLong(WEELAB_LAST_UPDATE_WIDGET_MILLIS, System.currentTimeMillis())
                prefEditor.apply()
                for (appWidgetId in appWidgetIds) {
                    updateAppWidget(context, appWidgetManager, appWidgetId)
                }
                try {
                    Thread.sleep(3000)
                } catch (e: java.lang.Exception) {
                    e.printStackTrace()
                }
                for (appWidgetId in appWidgetIds) {
                    partialUpdateAppWidgetProgressBar(context, appWidgetManager, appWidgetId)
                }
            }
            else{
                val refreshToken = prefs.getString(WeelabConst.WEELAB_REFRESH_TOKEN, "") + ""
                if(refreshToken.isEmpty()){
                    try {
                        Thread.sleep(5000)
                    } catch (e: java.lang.Exception) {
                        e.printStackTrace()
                    }
                    for (appWidgetId in appWidgetIds) {
                        partialUpdateAppWidgetNoToken(context, appWidgetManager, appWidgetId)
                    }
                }
            }
        } else{
            try {
                Thread.sleep(3000)
            } catch (e: java.lang.Exception) {
                e.printStackTrace()
            }
            for (appWidgetId in appWidgetIds) {
                partialUpdateAppWidgetProgressBar(context, appWidgetManager, appWidgetId)
            }
        }
    }

    override fun onEnabled(context: Context) {
        // Enter relevant functionality for when the first widget is created
    }

    override fun onDisabled(context: Context) {
        // Enter relevant functionality for when the last widget is disabled
    }


    private fun getLocalizedHHMMStamp(context: Context, timeMillis: Long): String? {
        // Different formatters for 12 and 24 hour timestamps
        val formatter24 = SimpleDateFormat("HH:mm", Locale.getDefault())
        val formatter12 = SimpleDateFormat("h:mm a", Locale.getDefault())
        // According to users preferences the OS clock is displayed in 24 hour format
        return if (DateFormat.is24HourFormat(context)) {
            formatter24.format(timeMillis)
        } else {
            formatter12.format(timeMillis)
        }
    }

    private fun daysOfWeek(): Array<String?> {
        val currentTimestamp = System.currentTimeMillis()
        val dayInMillis = 24 * 3600000
        val myArray = arrayOfNulls<String>(7)
        val simpleDateFormat = SimpleDateFormat("EEEE, dd-MM-yyyy", Locale.getDefault())
        var nameOfDay: String?
        for (i in 0..6) {
            val r: Int = dayInMillis * (i + 1)
            nameOfDay = simpleDateFormat.format(currentTimestamp + r)
            myArray[i] = nameOfDay.toString().uppercase().substring(0,1)
        }
        return myArray
    }

    private fun checkActiveAlarms(context: Context): Int {
        val tbrest = TBRest(context.applicationContext)
        return tbrest.getCountActiveAlarms()
    }

    private fun checkNetworkAndServer(context: Context): Int {
        val tbrest = TBRest(context.applicationContext)
        return tbrest.getCountActiveAlarms()
    }

}
