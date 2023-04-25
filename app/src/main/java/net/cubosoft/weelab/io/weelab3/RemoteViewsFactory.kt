package net.cubosoft.weelab.io.weelab3

import android.content.Context
import android.graphics.Color
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking


class RemoteViewsFactory(
    context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private var myChart: LineChart? = null
    //    private var myListDevices = mutableListOf<String>()
    private var scale = 2f
    var myListDevices = ArrayList<DeviceAndTimeSeries>()
    private var sizeOfText = 12f
    private var sizeOfLineTS = 3f
    private var sizeOfLineThreshold = 2f

    private var mContext: Context = context

    override fun onCreate() {
        println("1111111111111111111111111111111111111111111111111111111111")
        myListDevices.clear()
        scale = mContext.resources.displayMetrics.density
        if (scale < 2f) {
            sizeOfText *= (1.5f / scale)
            sizeOfLineTS *= (2f / scale)
            sizeOfLineThreshold *= (3f / scale)
        }
        myChart = LineChart(mContext)
        myChart!!.measure(
            View.MeasureSpec.makeMeasureSpec(600, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(200, View.MeasureSpec.EXACTLY)
        )
        myChart!!.layout(0, 0, myChart!!.measuredWidth, myChart!!.measuredHeight)
    }

    override fun onDataSetChanged() {
        println("222222222222222222222222222222222222222222222222222222")
        println("onDataSeeeeeeeeeeeeeeeeeeeeeeeeeeeet")
        initList()
    }


    override fun onDestroy() {
    }

    override fun getCount(): Int {
        return myListDevices.size
    }

    override fun getViewAt(position: Int): RemoteViews? {
        if (position > count - 1){
            return loadingView
        }
        if (myListDevices[position].getListTelemetryToDraw()!!.isNotEmpty()) {
            myListDevices[position].getTemperatureThresholdLow()?.let {
                myListDevices[position].getTemperatureThresholdHigh()?.let { it1 ->
                    myListDevices[position].getListTelemetryToDraw()?.let { it2 ->
                        setData(
                            it2,
                            it,
                            it1,
                            myListDevices[position].isAlarmActive()
                        )
                    }
                }
            } //
        }
        val view = RemoteViews(mContext.packageName, R.layout.device_item)
        try {
            RemoteViews(mContext.packageName, R.layout.app_widget)
            view.setTextViewText(R.id.tv_name, myListDevices[position].getName())
            if(myListDevices[position].isMaintenanceActive()) {
                view.setImageViewResource(R.id.img_chart, R.drawable.tools)
                view.setImageViewResource(R.id.img_thermo, R.drawable.ellipsis)
                view.setImageViewResource(R.id.img_humi, R.drawable.ellipsis)
                view.setViewVisibility(R.id.layout_no_maintenance, View.GONE)
                view.setViewVisibility(R.id.layout_maintenance, View.VISIBLE)
            }
            else {
                //************************************************************************
                if (myListDevices[position].getLastTelemetryTemperature() != null) {
                    view.setTextViewText(
                        R.id.tv_temperature,
                        String.format("%.1f", myListDevices[position].getLastTelemetryTemperature())
                                + "°C"
                    )
                } else {
                    view.setTextViewText(R.id.tv_temperature, "...")
                }
                if (myListDevices[position].getLastTelemetryHumidity() != null) {
                    view.setTextViewText(
                        R.id.tv_humidity,
                        String.format(
                            "%.1f",
                            myListDevices[position].getLastTelemetryHumidity()
                        ) + "%"
                    )
//                    view.setTextViewText(R.id.tv_humidity, "99.99%")
                } else {
                    view.setTextViewText(R.id.tv_humidity, "...")
                }
                if (myListDevices[position].isAlarmActive()) {
                    view.setImageViewResource(R.id.img_alarm, R.drawable.bell_red)
                } else {
                    view.setImageViewResource(R.id.img_alarm, R.drawable.ellipsis)
                }
                if (myListDevices[position].getListTelemetryToDraw()!!.isNotEmpty()) {
                    view.setImageViewBitmap(R.id.img_chart, myChart!!.chartBitmap) //put image
                } else {
                    view.setImageViewResource(R.id.img_chart, R.drawable.ellipsis) //put image
                }
                //*************************************************************************
            }
        }catch(_:Exception){

        }
        return view
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getItemId(position: Int): Long {
        return position.toLong()
    }

    override fun hasStableIds(): Boolean {
        return true
    }

    @Synchronized
    private fun initList() {
        println("LLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLLIST")
        myListDevices.clear()
        val tbrest = TBRest(mContext.applicationContext)
        tbrest.getDevicesAndTimeSeries()
        myListDevices = tbrest.getDevicesAndTimeSeries()
    }

    private fun setData(
        timeSeries: ArrayList<Float>,
        limitMin: Float,
        limitMax: Float,
        isAlarmActive: Boolean
    ) = runBlocking {
        launch(Dispatchers.IO) {
            myChart!!.clear()
            val entries = ArrayList<Entry>()
            var max = 0f
            var min = 0f
            if (timeSeries.size > 0) {
                max = timeSeries[0]
                min = timeSeries[0]
            }
            for (i in timeSeries.indices) {
                entries.add(Entry(i.toFloat(), timeSeries[i]))
                if (timeSeries[i] < min) {
                    min = timeSeries[i]
                }
                if (timeSeries[i] > max) {
                    max = timeSeries[i]
                }
            }
            //////////////////////////// ThresholdHigh ///////////////////////////////////////////
            val entries2 = ArrayList<Entry>()
            val setLimitMax = LineDataSet(entries2, "")
            if (limitMax != -99999.99f) {
                entries2.add(Entry(0F, limitMax))
                entries2.add(Entry(timeSeries.size.toFloat(), limitMax))
                setLimitMax.setDrawFilled(false)
                setLimitMax.setDrawCircles(false)
                setLimitMax.lineWidth = sizeOfLineThreshold
                setLimitMax.isHighlightEnabled = false
                setLimitMax.color = Color.parseColor("#727272")
                setLimitMax.setDrawHorizontalHighlightIndicator(false)
                setLimitMax.enableDashedLine(10f, 10f, 0f)
            }
            //////////////////////////// ThresholdLow ///////////////////////////////////////////
            val entries3 = ArrayList<Entry>()
            val setLimitMin = LineDataSet(entries3, "")
            if (limitMin != -99999.99f) {
                entries3.add(Entry(0F, limitMin))
                entries3.add(Entry(timeSeries.size.toFloat(), limitMin))
                setLimitMin.setDrawFilled(false)
                setLimitMin.setDrawCircles(false)
                setLimitMin.lineWidth = sizeOfLineThreshold
                setLimitMin.isHighlightEnabled = false
                setLimitMin.color = Color.parseColor("#727272")
                setLimitMin.setDrawHorizontalHighlightIndicator(false)
                setLimitMin.enableDashedLine(10f, 10f, 0f)
            }
            //////////////////////////// ZeroLine ///////////////////////////////////////////
            val entries4 = ArrayList<Entry>()
            entries4.add(Entry(0f, -0.3f)) //0.3 por conflicto para graficar con valor 0
            entries4.add(Entry(timeSeries.size.toFloat(), -0.3f))
            val setZeroLine = LineDataSet(entries4, "")
            setZeroLine.setDrawFilled(false)
            setZeroLine.setDrawCircles(false)
            setZeroLine.lineWidth = sizeOfLineThreshold * 1.5f
            setZeroLine.isHighlightEnabled = false
            setZeroLine.color = Color.parseColor("#090084")
            setZeroLine.setDrawHorizontalHighlightIndicator(false)
//            setZeroLine.enableDashedLine(10f, 10f, 0f)
            //////////////////////////// invisibleHeight ///////////////////////////////////////////
            val entries5 = ArrayList<Entry>()
            var auxMax = 0F
            var auxMin = 0F
            if (limitMin != -99999.99f) {
                /////////////////////////////////////
                if (limitMin < min) {
                    if (limitMin < 0f) {
                        entries5.add(Entry(0f, limitMin - 3))
                        auxMin = limitMin - 3
                    } else {
                        entries5.add(Entry(0f, -3F))
                        auxMin = -3F
                    }
                } else {
                    if (min < 0f) {
                        entries5.add(Entry(0f, min - 3))
                        auxMin = min - 3
                    } else {
                        entries5.add(Entry(0f, -3F))
                        auxMin = -3F
                    }
                }
            } else {
                if (min < 0f) {
                    entries5.add(Entry(0f, min - 3))
                    auxMin = min - 3
                } else {
                    entries5.add(Entry(0f, -3F))
                    auxMin = -3f
                }
            }
            if (limitMax != -99999.99f) {
                if (limitMax > max) {
                    if (limitMax > 0f) {
                        entries5.add(Entry(0f, limitMax + 3))
                        auxMax = limitMax + 3
                    } else {
                        entries5.add(Entry(0f, 3F))
                        auxMax = 3F
                    }
                } else {
                    if (max > 0f) {
                        entries5.add(Entry(0f, max + 3))
                        auxMax = max + 3
                    } else {
                        entries5.add(Entry(0f, 3F))
                        auxMax = 3F
                    }
                }
            } else {
                if (max > 0f) {
                    entries5.add(Entry(0f, max + 3))
                    auxMax = max + 3
                } else {
                    entries5.add(Entry(0f, +3F))
                    auxMax = 3f
                }
            }
            val lineHeight = LineDataSet(entries5, "")
            lineHeight.setDrawFilled(false)
            lineHeight.setDrawCircles(false)
            lineHeight.lineWidth = 5f
            lineHeight.isHighlightEnabled = false
            lineHeight.color = Color.parseColor("#12151f")
            lineHeight.setDrawHorizontalHighlightIndicator(false)
            //////////////////////////// ArrowHigh ///////////////////////////////////////////
            val percentageArrow = auxMax - auxMin
            val entriesArrowH1 = ArrayList<Entry>()
            val setArrowH1 = LineDataSet(entriesArrowH1, "")
            val entriesArrowH2 = ArrayList<Entry>()
            val setArrowH2 = LineDataSet(entriesArrowH2, "")
            if (limitMax != -99999.99f) {
                entriesArrowH1.add(Entry(timeSeries.size.toFloat() * 0.98F, limitMax + (percentageArrow * 0.05F)))
                entriesArrowH1.add(Entry(timeSeries.size.toFloat(), limitMax))
                setArrowH1.setDrawFilled(false)
                setArrowH1.setDrawCircles(false)
                setArrowH1.lineWidth = sizeOfLineThreshold
                setArrowH1.isHighlightEnabled = false
                setArrowH1.color = Color.parseColor("#727272")
                setArrowH1.setDrawHorizontalHighlightIndicator(false)
                entriesArrowH2.add(Entry(timeSeries.size.toFloat() * 0.98F, limitMax - (percentageArrow * 0.05F)))
                entriesArrowH2.add(Entry(timeSeries.size.toFloat(), limitMax))
                setArrowH2.setDrawFilled(false)
                setArrowH2.setDrawCircles(false)
                setArrowH2.lineWidth = sizeOfLineThreshold
                setArrowH2.isHighlightEnabled = false
                setArrowH2.color = Color.parseColor("#727272")
                setArrowH2.setDrawHorizontalHighlightIndicator(false)
            }
            //////////////////////////// ArrowLow ///////////////////////////////////////////
//            val percentageArrow = auxMax - auxMin
            val entriesArrowL1 = ArrayList<Entry>()
            val setArrowL1 = LineDataSet(entriesArrowL1, "")
            val entriesArrowL2 = ArrayList<Entry>()
            val setArrowL2 = LineDataSet(entriesArrowL2, "")
            if (limitMax != -99999.99f) {
                entriesArrowL1.add(Entry(timeSeries.size.toFloat() * 0.98F, limitMin + (percentageArrow * 0.05F)))
                entriesArrowL1.add(Entry(timeSeries.size.toFloat(), limitMin))
                setArrowL1.setDrawFilled(false)
                setArrowL1.setDrawCircles(false)
                setArrowL1.lineWidth = sizeOfLineThreshold
                setArrowL1.isHighlightEnabled = false
                setArrowL1.color = Color.parseColor("#727272")
                setArrowL1.setDrawHorizontalHighlightIndicator(false)
                entriesArrowL2.add(Entry(timeSeries.size.toFloat() * 0.98F, limitMin - (percentageArrow * 0.05F)))
                entriesArrowL2.add(Entry(timeSeries.size.toFloat(), limitMin))
                setArrowL2.setDrawFilled(false)
                setArrowL2.setDrawCircles(false)
                setArrowL2.lineWidth = sizeOfLineThreshold
                setArrowL2.isHighlightEnabled = false
                setArrowL2.color = Color.parseColor("#727272")
                setArrowL2.setDrawHorizontalHighlightIndicator(false)
            }
            /////////////////////////////// Timeseries /////////////////////////////////////////
            // create a dataset and give it a type
            val set1 = LineDataSet(entries, "")
            set1.mode = LineDataSet.Mode.CUBIC_BEZIER
            set1.cubicIntensity = 0.2f
            set1.setDrawFilled(false)
            set1.setDrawCircles(false)
            set1.lineWidth = sizeOfLineTS
            set1.isHighlightEnabled = false
            var normalOrDanger = Color.parseColor("#00ff00")
            if (isAlarmActive) {
                normalOrDanger = Color.parseColor("#ff0000")
            }
            set1.color = normalOrDanger //
            set1.setDrawHorizontalHighlightIndicator(false)

            // create a data object with the datasets
            val setsToGraph: MutableList<ILineDataSet> = ArrayList()
            setsToGraph.add(lineHeight)
            setsToGraph.add(set1)
            setsToGraph.add(setZeroLine)
            if (limitMin != -99999.99f) {
                setsToGraph.add(setLimitMin)
                setsToGraph.add(setArrowL1)
                setsToGraph.add(setArrowL2)
            }
            if (limitMax != -99999.99f) {
                setsToGraph.add(setLimitMax)
                setsToGraph.add(setArrowH1)
                setsToGraph.add(setArrowH2)
            }
            val data = LineData(setsToGraph)
            data.setValueTextSize(sizeOfText)
            data.setDrawValues(false)
            myChart!!.data = data
            myChart!!.legend.isEnabled = false
            myChart!!.description.isEnabled = false
            myChart!!.axisLeft.setDrawGridLines(false)
            myChart!!.axisLeft.setDrawLabels(false)
            myChart!!.axisLeft.setDrawAxisLine(false)
            myChart!!.xAxis.setDrawGridLines(false)
            myChart!!.xAxis.setDrawLabels(false)
            myChart!!.xAxis.setDrawAxisLine(false)
            myChart!!.xAxis.setDrawGridLines(false)
            myChart!!.xAxis.setDrawLabels(false)
            myChart!!.xAxis.setDrawAxisLine(false)
            myChart!!.axisRight.setDrawGridLines(false)
            myChart!!.axisRight.setDrawLabels(false)
            myChart!!.axisRight.textColor = Color.parseColor("#ffffff")
            myChart!!.axisRight.textSize = sizeOfText
            myChart!!.axisRight.setDrawAxisLine(false)
            myChart!!.setBackgroundColor(Color.parseColor("#000000"))

            //////////////////////////////////////////////////////////////////////////////////////
            val yAxis = myChart!!.axisLeft
            yAxis.removeAllLimitLines()
            myChart!!.invalidate()
        }
    }

}