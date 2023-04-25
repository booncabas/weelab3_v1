package net.cubosoft.weelab.io.weelab3

import android.content.SharedPreferences
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.PREFS_CUSTOMER_ID_KEY
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.SHARED_PREF_FILE_NAME
import net.cubosoft.weelab.io.weelab3.WeelabConst.Companion.WEELAB_REFRESH_TOKEN


class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        ////////////////////////////// boon test
        //  from prefs.
        val prefs: SharedPreferences = this.getSharedPreferences(SHARED_PREF_FILE_NAME, 0)
        val prefEditor = prefs.edit()
        prefEditor.putString(PREFS_CUSTOMER_ID_KEY, "b72b2570-69a2-11ed-95fe-4732c9ea5bca")
//        prefEditor.putString(WEELAB_ACCESS_TOKEN, "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkZW1vQHdlZWxhYi5pbyIsInVzZXJJZCI6ImU4ZWZjNzAwLTY5YTItMTFlZC05NWZlLTQ3MzJjOWVhNWJjYSIsInNjb3BlcyI6WyJDVVNUT01FUl9VU0VSIl0sImlzcyI6InRoaW5nc2JvYXJkLmlvIiwiaWF0IjoxNjc4NDk0MzIwLCJleHAiOjE2Nzg1MDMzMjAsImZpcnN0TmFtZSI6IlVzZXIiLCJsYXN0TmFtZSI6IkRlbW8iLCJlbmFibGVkIjp0cnVlLCJpc1B1YmxpYyI6ZmFsc2UsInRlbmFudElkIjoiMDU4NDNjZTAtNWM2OC0xMWVkLTkyMDYtNTUxYWU1MjMzZjZmIiwiY3VzdG9tZXJJZCI6ImI3MmIyNTcwLTY5YTItMTFlZC05NWZlLTQ3MzJjOWVhNWJjYSJ9.SWgYa_0Zbs0NDDMnaBW8mdTANV4gAUPiUzWpF1OadTxSsctjFM_IfpH2JNoAknKyPR9BO_w_KQxoJ0s7enM6pA")
        prefEditor.putString(WEELAB_REFRESH_TOKEN, "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkZW1vQHdlZWxhYi5pbyIsInVzZXJJZCI6ImU4ZWZjNzAwLTY5YTItMTFlZC05NWZlLTQ3MzJjOWVhNWJjYSIsInNjb3BlcyI6WyJSRUZSRVNIX1RPS0VOIl0sImlzcyI6InRoaW5nc2JvYXJkLmlvIiwiaWF0IjoxNjgyMDk3MTEzLCJleHAiOjE2ODI3MDE5MTMsImlzUHVibGljIjpmYWxzZSwianRpIjoiZGNiODQ0MzktYjk5Ni00Y2ZjLTg4MzktZTE3ZDhhM2JjOWMwIn0.3VnFRa4EkjRu_OB6C3IoP7SFGrNxagyXwa_iPPqlTFN1tsox9_FWbqMrLulm0Ic1NQEVrWULle9ZuE7TnHo96A")
//        prefEditor.putString(WEELAB_ACCESS_TOKEN, "")
//        prefEditor.putString(WEELAB_REFRESH_TOKEN, "")
        prefEditor.apply()
        //////////////////////////////////////
        val weelabUtil = WeelabUtil(this)
//        weelabUtil.createNotificationChannel()
        weelabUtil.setSocketToWork()
        /*Handler(Looper.getMainLooper()).postDelayed({
            updateCall()
        }, 120000)*/
    }

}