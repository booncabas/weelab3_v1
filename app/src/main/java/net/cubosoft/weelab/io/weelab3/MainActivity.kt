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
        val prefs: SharedPreferences = this.getSharedPreferences(SHARED_PREF_FILE_NAME, 0)
        val prefEditor = prefs.edit()
        prefEditor.putString(PREFS_CUSTOMER_ID_KEY, "b72b2570-69a2-11ed-95fe-4732c9ea5bca")
        prefEditor.putString(WEELAB_REFRESH_TOKEN, "eyJhbGciOiJIUzUxMiJ9.eyJzdWIiOiJkZW1vQHdlZWxhYi5pbyIsInVzZXJJZCI6ImU4ZWZjNzAwLTY5YTItMTFlZC05NWZlLTQ3MzJjOWVhNWJjYSIsInNjb3BlcyI6WyJSRUZSRVNIX1RPS0VOIl0sImlzcyI6InRoaW5nc2JvYXJkLmlvIiwiaWF0IjoxNjg0ODAyNTc0LCJleHAiOjE2ODU0MDczNzQsImlzUHVibGljIjpmYWxzZSwianRpIjoiNmIyMDZjMjctOGU0OC00MGFiLTk0YjEtNmExZjVmZTQwNmFjIn0.PFzshGD6NowNJGMe83K6LwXlWafHqJDTPauQP89sPNTrgmYpq1LW6zsmMZpmdhPoxIVVi1oi0QZAU37QtrwH0A")
        prefEditor.apply()
        //////////////////////////////////////
//        val weelabUtil = WeelabUtil(this)
//        weelabUtil.setSocketToWork()
        /*Handler(Looper.getMainLooper()).postDelayed({
            updateCall()
        }, 120000)*/
    }

}