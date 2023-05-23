package net.cubosoft.weelab.io.weelab3

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.PREFS_CUSTOMER_ID_KEY
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.SHARED_PREF_FILE_NAME
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_IDS_STRING
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_LAST_UPDATE_IDS_MILLIS
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_REFRESH_TOKEN
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.URL


class TBRest(private val context: Context) {

    private fun checkLastUpdateIdsMillis(): Boolean {
        val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREF_FILE_NAME, 0)
        val lastUpdateIdsMillis = prefs.getLong(WEELAB_LAST_UPDATE_IDS_MILLIS, 0)
        if (lastUpdateIdsMillis == 0L){
            return true
        }
        else if(System.currentTimeMillis() > lastUpdateIdsMillis + 3600000){//3600000 boon set to 1 hour
            return true
        }
        return  false
    }

    private fun getNewToken(): String{
        val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREF_FILE_NAME, 0)
        val refreshToken = prefs.getString(WEELAB_REFRESH_TOKEN, "") + ""
        val prefEditor: SharedPreferences.Editor = prefs.edit()
        var tokenResult = ""
        try{
            val client = OkHttpClient()
            val stringBody = "{\"refreshToken\":\"$refreshToken\"}"
            val body: RequestBody = stringBody.toRequestBody()
            val request: Request = Request.Builder()
                .url("https://app.weelab.io:8080/api/auth/token")
                .post(body)
                .addHeader("Accept", "application/json")
                .addHeader("Content-Type", "application/json")
                .build()

            val response1Body = client.newCall(request).execute().use {
                if (!it.isSuccessful) throw IOException("Unexpected code $it")
                return@use it.body!!.string()
            }
            try {
                val root = JSONObject(response1Body)
                val tokenResp = root.getString("token")
                tokenResult = tokenResp
                val tokenRefResp = root.getString("refreshToken")
                if(tokenRefResp.isNotEmpty()) {
                    prefEditor.putString(WEELAB_REFRESH_TOKEN, tokenRefResp)
                    prefEditor.commit()
                }
            } catch (e: JSONException) {
                e.printStackTrace()
            }
        }catch (e: Exception){
            e.printStackTrace()
        }
        return tokenResult
    }

    fun getListDevicesIds(): ArrayList<String>{
        val listDevicesIds = ArrayList<String>()
        val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREF_FILE_NAME, 0)
        val customerId = prefs.getString(PREFS_CUSTOMER_ID_KEY, "")
        val prefEditor: SharedPreferences.Editor = prefs.edit()
        val savedIdsString = prefs.getString(WEELAB_IDS_STRING, "")
        runBlocking {
            launch(Dispatchers.IO) {
                if (checkLastUpdateIdsMillis()) {
                    val token = getNewToken()
                    try {
                        val client = OkHttpClient()
                        val url = URL("https://app.weelab.io:8080/api/customer/"+ customerId +"/devices?pageSize=10000&page=0")
                        val request = Request.Builder()
                            .addHeader("Authorization", "Bearer $token")
                            .url(url)
                            .get()
                            .build()
                        val response = client.newCall(request).execute()
                        val responseBody = response.body!!.string()
                        //Response///////////////////////////////////////////////////7
                        var stringIds = ""
                        try {
                            val root = JSONObject(responseBody)
                            val arrayData = root.getJSONArray("data")
                            for (i in 0 until arrayData.length()){
                                val deviceData = arrayData.getJSONObject(i)
                                val deviceIdObj = deviceData.getJSONObject("id")
                                val deviceId = deviceIdObj.getString("id")
                                stringIds += "$deviceId,"
                            }
                        } catch (e: JSONException) {
                            e.printStackTrace()
                        }
                        if (stringIds.isNotEmpty())
                            prefEditor.putString(WEELAB_IDS_STRING, stringIds.dropLast(1))
                        prefEditor.putLong(WEELAB_LAST_UPDATE_IDS_MILLIS, System.currentTimeMillis())
                        prefEditor.apply()
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                } else {
                    if (savedIdsString!!.isNotEmpty()) {
                        val stringArray: List<String> = savedIdsString.split(",")
                        for (i in stringArray.indices) {//boon error
                            listDevicesIds.add(stringArray[i])
                        }
                    }
                }
            }
        }
        return listDevicesIds
    }

    fun getSingleDeviceName(deviceId: String): String{
        var singleDeviceName = ""
        runBlocking {
            launch(Dispatchers.IO) {
                val token = getNewToken()
                try {
                    val client = OkHttpClient()
                    val url = URL("https://app.weelab.io:8080/api/device/"+ deviceId)
                    val request = Request.Builder()
                        .addHeader("Authorization", "Bearer $token")
                        .url(url)
                        .get()
                        .build()
                    val response = client.newCall(request).execute()
                    val responseBody = response.body!!.string()
                    try{
                        val root = JSONObject(responseBody)
                        var name = root.getString("label")
                        if (name.isEmpty()){
                            name = root.getString("name")
                        }
                        singleDeviceName = name
                    }catch (_: Exception){}

                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
        return  singleDeviceName
    }

    fun getCountActiveAlarms(): Int {
        var activeAlarms = -1
        val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREF_FILE_NAME, 0)
        val customerId = prefs.getString(PREFS_CUSTOMER_ID_KEY, "")
        runBlocking {
            launch(Dispatchers.IO) {
                val token = getNewToken()
                try {
                    val client = OkHttpClient()
                    var url = URL("https://app.weelab.io:8080/api/customer/"+ customerId +"/devices?pageSize=10000&page=0")
                    var request = Request.Builder()
                        .addHeader("Authorization", "Bearer $token")
                        .url(url)
                        .get()
                        .build()
                    var response = client.newCall(request).execute()
                    var responseBody = response.body!!.string()
//                    var responseN = response.networkResponse!!.toString()
                    //Response///////////////////////////////////////////////////7
                    try {
                        val root = JSONObject(responseBody)
                        val arrayData = root.getJSONArray("data")
                        if (arrayData.length() > 0){
                            activeAlarms = 0
                        }
                        for (i in 0 until arrayData.length()){
                            val deviceData = arrayData.getJSONObject(i)
                            ///////////////////////////////
                            val deviceIdObj = deviceData.getJSONObject("id")
                            val deviceId = deviceIdObj.getString("id")
                            /////////////////////////////////////////////// get active alarms
                            url = URL("https://app.weelab.io:8080/api/alarm/DEVICE/" + deviceId + "?fetchOriginator=true&searchStatus=ACTIVE&pageSize=1000&page=0")
                            request = Request.Builder()
                                .addHeader("Authorization", "Bearer $token")
                                .url(url)
                                .get()
                                .build()
                            response = client.newCall(request).execute()
                            responseBody = response.body!!.string()
                            try{
                                val root2 = JSONObject(responseBody)
                                val arrayData2 = root2.getInt("totalElements")
                                if (arrayData2 > 0) {
                                    //////////////////////////////////////////////// get maintenance, if  false alarm + 1
                                    val url2 = URL("https://app.weelab.io:8080/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/SERVER_SCOPE?keys=maintenance")
                                    val request2 = Request.Builder()
                                        .addHeader("Authorization", "Bearer $token")
                                        .url(url2)
                                        .get()
                                        .build()
                                    val response2 = client.newCall(request2).execute()
                                    val responseBody2 = response2.body!!.string()
                                    try{
                                        val root4 = JSONArray(responseBody2)
                                        var objMaintenance = root4.getJSONObject(0)
                                        val maintenance = objMaintenance.getBoolean("value")
                                        if(!maintenance){
                                            activeAlarms += 1
                                        }
                                    }catch (e: Exception){
                                        e.printStackTrace()
                                    }
                                    ////////////////////////////////////////////////
                                }
                            }catch (e: Exception){
                                e.printStackTrace()
                            }
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }

                } catch (e: Exception) {
                    e.printStackTrace()
                }

            }
        }
        return activeAlarms
    }

    @Synchronized
    fun getDevicesAndTimeSeries(): ArrayList<DeviceAndTimeSeries> {
        val listDevicesAndTimeSeries = ArrayList<DeviceAndTimeSeries>()
        val prefs: SharedPreferences = context.getSharedPreferences(SHARED_PREF_FILE_NAME, 0)
        val customerId = prefs.getString(PREFS_CUSTOMER_ID_KEY, "")
        runBlocking {
            launch(Dispatchers.IO) {
                Log.w(WeelabConst.WEELAB_TAG,"**************_start_get_devices_and_time_series")
                var thresholdHigh = -99999.99f
                var thresholdLow = -99999.99f
                var maintenance = false
                val token = getNewToken()
                try {
                    val client = OkHttpClient()
                    var url = URL("https://app.weelab.io:8080/api/customer/"+ customerId +"/devices?pageSize=10000&page=0")
                    var request = Request.Builder()
                        .addHeader("Authorization", "Bearer $token")
                        .url(url)
                        .get()
                        .build()
                    var response = client.newCall(request).execute()
                    var responseBody = response.body!!.string()
                    //Response///////////////////////////////////////////////////7
                    var temperature = -99999F
                    var humidity = -99999F
                    try {
                        val root = JSONObject(responseBody)
                        val arrayData = root.getJSONArray("data")
                        for (i in 0 until arrayData.length()){
                            val deviceAndTimeSeriesTemp = DeviceAndTimeSeries()
                            val floatArray = ArrayList<Float>()
                            val deviceData = arrayData.getJSONObject(i)
                            val deviceIdObj = deviceData.getJSONObject("id")
                            val deviceId = deviceIdObj.getString("id")
                            /////////////////////////////////////////////// get active alarms
                            url = URL("https://app.weelab.io:8080/api/alarm/DEVICE/" + deviceId + "?fetchOriginator=true&searchStatus=ACTIVE&pageSize=1000&page=0")
                            request = Request.Builder()
                                .addHeader("Authorization", "Bearer $token")
                                .url(url)
                                .get()
                                .build()
                            response = client.newCall(request).execute()
                            responseBody = response.body!!.string()
                            try{
                                val root2 = JSONObject(responseBody)
                                val arrayData2 = root2.getInt("totalElements")
                                if (arrayData2 > 0)
                                    deviceAndTimeSeriesTemp.setAlarmActive(true)
                            }catch (e: Exception){
                                e.printStackTrace()
                            }
                            /////////////////////////////////////////////// get last telemetry
                            url = URL("https://app.weelab.io:8080/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?keys=temperature,humidity&useStrictDataTypes=true")
                            request = Request.Builder()
                                .addHeader("Authorization", "Bearer $token")
                                .url(url)
                                .get()
                                .build()
                            response = client.newCall(request).execute()
                            responseBody = response.body!!.string()
                            try{
                                val root2 = JSONObject(responseBody)
                                val arrayData2 = root2.getJSONArray("temperature")
                                val temperatureArray = arrayData2.getJSONObject(0)
                                temperature = temperatureArray.getDouble("value").toFloat()
                            }catch (e: Exception){
                                e.printStackTrace()
                            }
                            try{
                                val root3 = JSONObject(responseBody)
                                val arrayData3 = root3.getJSONArray("humidity")
                                val humidityArray = arrayData3.getJSONObject(0)
                                humidity = humidityArray.getDouble("value").toFloat()
                            }catch (e: Exception){
                                e.printStackTrace()
                            }
                            //////////////////////////////////////////////// get maintenance
                            url = URL("https://app.weelab.io:8080/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/SERVER_SCOPE?keys=maintenance")
                            request = Request.Builder()
                                .addHeader("Authorization", "Bearer $token")
                                .url(url)
                                .get()
                                .build()
                            response = client.newCall(request).execute()
                            responseBody = response.body!!.string()
                            try{
                                val root4 = JSONArray(responseBody)
                                var objMaintenance = root4.getJSONObject(0)
                                maintenance = objMaintenance.getBoolean("value")
                            }catch (e: Exception){
                                e.printStackTrace()
                            }
                            //////////////////////////////////////////////// get threshold
                            url = URL("https://app.weelab.io:8080/api/plugins/telemetry/DEVICE/" + deviceId + "/values/attributes/SERVER_SCOPE?keys=temperatureThresholdHigh&keys=temperatureThresholdLow")
                            request = Request.Builder()
                                .addHeader("Authorization", "Bearer $token")
                                .url(url)
                                .get()
                                .build()
                            response = client.newCall(request).execute()
                            responseBody = response.body!!.string()
                            try{
                                val root4 = JSONArray(responseBody)
                                var objHighAndLow = root4.getJSONObject(0)
                                val nameThreshold = objHighAndLow.getString("key")
                                if (nameThreshold == "temperatureThresholdLow"){
                                    thresholdLow = objHighAndLow.getDouble("value").toFloat()
                                    objHighAndLow = root4.getJSONObject(1)
                                    thresholdHigh = objHighAndLow.getDouble("value").toFloat()
                                }else{
                                    thresholdHigh = objHighAndLow.getDouble("value").toFloat()
                                    objHighAndLow = root4.getJSONObject(1)
                                    thresholdLow = objHighAndLow.getDouble("value").toFloat()
                                }
                            }catch (e: Exception){
                                e.printStackTrace()
                            }
                            ////////////////////////////////////////////////// get time series
                            val now = System.currentTimeMillis()
                            val startTime = now - (3600000 * 24 * 7)
                            url = URL("https://app.weelab.io:8080/api/plugins/telemetry/DEVICE/" + deviceId + "/values/timeseries?keys=temperature&interval=3600000&limit=1000&agg=NONE&useStrictDataTypes=false&orderBy=ASC&startTs=" + startTime.toString() + "&endTs=" + now.toString())
                            request = Request.Builder()
                                .addHeader("Authorization", "Bearer $token")
                                .url(url)
                                .get()
                                .build()
                            response = client.newCall(request).execute()
                            responseBody = response.body!!.string()
                            try{
                                val root5 = JSONObject(responseBody)
                                val arrayData2 = root5.getJSONArray("temperature")
                                for(j in 0 until arrayData2.length()){
                                    val temperatureArray = arrayData2.getJSONObject(j)
                                    floatArray.add(temperatureArray.getDouble("value").toFloat())
                                }
                            }catch (e: Exception){
                                e.printStackTrace()
                            }
                            /////////////////////////////////////////////// set values
                            var deviceName = deviceData.getString("label")
                            if (deviceName.isEmpty()){
                                deviceName = root.getString("name")
                            }

//                            deviceAndTimeSeriesTemp.setName("$deviceName "+ (0..1000).random())//test
                            deviceAndTimeSeriesTemp.setName(deviceName)//test
                            if (temperature == -99999F)
                                deviceAndTimeSeriesTemp.setLastTelemetryTemperature(null)
                            else
                                deviceAndTimeSeriesTemp.setLastTelemetryTemperature(temperature)
                            if (humidity == -99999F)
                                deviceAndTimeSeriesTemp.setLastTelemetryHumidity(null)
                            else
                                deviceAndTimeSeriesTemp.setLastTelemetryHumidity(humidity)
                            deviceAndTimeSeriesTemp.setListTelemetryToDraw(floatArray)
                            deviceAndTimeSeriesTemp.setTemperatureThresholdHigh(thresholdHigh)
                            deviceAndTimeSeriesTemp.setTemperatureThresholdLow(thresholdLow)
                            deviceAndTimeSeriesTemp.setMaintenanceActive(maintenance)//boon last thing today
                            listDevicesAndTimeSeries.add(deviceAndTimeSeriesTemp)
                            ////////////////////////////////////////////////////////////////////
                        }
                    } catch (e: JSONException) {
                        e.printStackTrace()
                    }
                    //Response///////////////////////////////////////////////////7
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                Log.w(WeelabConst.WEELAB_TAG,"**************_end_get_devices_and_time_series")
            }
        }
        return listDevicesAndTimeSeries
    }

}
