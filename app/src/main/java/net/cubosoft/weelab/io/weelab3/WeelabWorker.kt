package net.cubosoft.weelab.io.weelab3

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.work.Worker
import androidx.work.WorkerParameters
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.SHARED_PREF_FILE_NAME
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_LAST_UPDATE_WIDGET_MILLIS
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_TAG
import java.util.*

class WeelabWorker(context: Context, userParameters: WorkerParameters) : Worker(context, userParameters) {

    private val myObjCount = CounterManager.getInstance(context)
    private val objSocket = SocketManager.getInstance(context)
    private val mContext = context
    override fun doWork(): Result {
        try {
            if (myObjCount.getCounter() == 0){
                Log.w(WEELAB_TAG,"INIT_counter_started_in_${Thread.currentThread().name}")
                myObjCount.initCounter(Thread.currentThread().id.toString())
            }
            ///////////////////////////////////////////////////
            val prefs: SharedPreferences = mContext.getSharedPreferences(SHARED_PREF_FILE_NAME, 0)
            val lastUpdatedWidgetInMillis = prefs.getLong(WEELAB_LAST_UPDATE_WIDGET_MILLIS, System.currentTimeMillis())
            if(System.currentTimeMillis() >= lastUpdatedWidgetInMillis + 2700000) {
                objSocket.sendBroadcastUpdate()
            }
            val c2 = Calendar.getInstance()
            val hour2 = c2.get(Calendar.HOUR_OF_DAY)
            val minute2 = c2.get(Calendar.MINUTE)
            Log.i(WEELAB_TAG, hour2.toString() + ":" + minute2.toString() + "_main_WORK_STARTED in ${Thread.currentThread().id} ${Thread.currentThread().name}")
            ///////////////////////////////////////////////////
            val prefEditor = prefs.edit()
            for (i in 0 .. 9){
                val c = Calendar.getInstance()
                val hour = c.get(Calendar.HOUR_OF_DAY)
                val minute = c.get(Calendar.MINUTE)
                var allTs = prefs.getString(WeelabConst.TEST_TS, "")
                Log.w(WEELAB_TAG,hour.toString() + ":" + minute.toString() + "_main_loop_" + i + "_***************->" + hour.toString() + ":" + minute.toString())
                if( i == 0){
                    allTs += hour.toString() + ":" + minute.toString() + "_main_start_looooooooop*"
                }
                try {
                    if (!objSocket.isSocketOpen()){
                        objSocket.initWebSocket()
                        Log.w(WEELAB_TAG, hour.toString() + ":" + minute.toString() + "_main_SOCKET_STARTED_in_try")
                        allTs += hour.toString() + ":" + minute.toString() + "_main_started_in_try*"
                    } else {
                        allTs += hour.toString() + ":" + minute.toString() + "_main_already_open*"
                    }
                }catch (e: Exception){
                    objSocket.initWebSocket()
                    Log.w(WEELAB_TAG, hour.toString() + ":" + minute.toString() + "_main_SOCKET_STARTED_in_catch")
                    allTs += hour.toString() + ":" + minute.toString() + "_main_started_in_catch*"
                }
                prefEditor.putString(WeelabConst.TEST_TS, allTs)
                prefEditor.apply()
                /////////////////////////////////////////////////
                Thread.sleep(57000)// boon 14 minutes max, 15 should start a new one
                /////////////////////////////////////////////////
                val idLastThread = myObjCount.getIdThreadLastUpdate() + ""
                if(myObjCount.getIdThreadToClose() == Thread.currentThread().id.toString()){
                    myObjCount.setIdThreadToClose("")
                    Log.w(WEELAB_TAG,hour.toString() + ":" + minute.toString() + "_main_End_in_${Thread.currentThread().id}")
                    Log.w(WEELAB_TAG,hour.toString() + ":" + minute.toString() + "_main_End_work_terminated_rejected_SUCCESS_${Thread.currentThread().id}")
                    return  Result.success()
                }
                if(idLastThread != Thread.currentThread().id.toString()){
                    myObjCount.setIdThreadToClose(idLastThread)
                }
                Log.w(WEELAB_TAG,hour.toString() + ":" + minute.toString() + "_main_for_" + i + "_in ${Thread.currentThread().id} _counter_"+myObjCount.getCounter())
//                Log.w(WEELAB_TAG,hour.toString() + ":" + minute.toString() + "_main_CCCCounter_add_in_name_${Thread.currentThread().name}_id_${Thread.currentThread().id}")
                myObjCount.setCounterAndName(Thread.currentThread().id.toString())
            }
            val util = WeelabUtil(mContext)
            util.setSocketToWorkA()
            Log.w(WEELAB_TAG,hour2.toString() + ":" + minute2.toString() + "_main_Result_work_completed_SUCCESS_in ${Thread.currentThread().id} _counter_"+myObjCount.getCounter())
            return Result.success()
        } catch (e: Exception) {
            Log.w(WEELAB_TAG,"_main_Result_work_incomplete_RETRY_in ${Thread.currentThread().id} _counter_"+myObjCount.getCounter())
            return Result.retry()
        }
    }

    override fun onStopped() {
        super.onStopped()
        Log.w(WEELAB_TAG,"Cleanup, work_stopped_id_" + Thread.currentThread().id)
    }

}
