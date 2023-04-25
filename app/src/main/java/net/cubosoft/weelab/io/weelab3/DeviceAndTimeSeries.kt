package net.cubosoft.weelab.io.weelab3

class DeviceAndTimeSeries {
    private var id: String? = null
    private var name: String? = null
    private var temperatureThresholdHigh: Float? = null
    private var temperatureThresholdLow: Float? = null
    private var lastTelemetryTemperature: Float? = null
    private var lastTelemetryHumidity: Float? = null
    private var listTelemetryToDraw: ArrayList<Float>? = null
    private var isAlarmActive = false
    private var isMaintenanceActive = false

    fun getId(): String? {
        return id
    }

    fun setId(id: String?) {
        this.id = id
    }

    fun getName(): String? {
        return name
    }

    fun setName(name: String?) {
        this.name = name
    }

    fun getTemperatureThresholdHigh(): Float? {
        return temperatureThresholdHigh
    }

    fun setTemperatureThresholdHigh(temperatureThresholdHigh: Float?) {
        this.temperatureThresholdHigh = temperatureThresholdHigh
    }

    fun getTemperatureThresholdLow(): Float? {
        return temperatureThresholdLow
    }

    fun setTemperatureThresholdLow(temperatureThresholdLow: Float?) {
        this.temperatureThresholdLow = temperatureThresholdLow
    }

    fun getLastTelemetryTemperature(): Float? {
        return lastTelemetryTemperature
    }

    fun setLastTelemetryTemperature(lastTelemetryTemperature: Float?) {
        this.lastTelemetryTemperature = lastTelemetryTemperature
    }

    fun getLastTelemetryHumidity(): Float? {
        return lastTelemetryHumidity
    }

    fun setLastTelemetryHumidity(lastTelemetryHumidity: Float?) {
        this.lastTelemetryHumidity = lastTelemetryHumidity
    }

    fun getListTelemetryToDraw(): ArrayList<Float>? {
        return listTelemetryToDraw
    }

    fun setListTelemetryToDraw(listTelemetryToDraw: ArrayList<Float>?) {
        this.listTelemetryToDraw = listTelemetryToDraw
    }

    fun isAlarmActive(): Boolean {
        return isAlarmActive
    }

    fun setAlarmActive(alarmActive: Boolean) {
        isAlarmActive = alarmActive
    }

    fun isMaintenanceActive(): Boolean {
        return isMaintenanceActive
    }

    fun setMaintenanceActive(maintenanceActive: Boolean) {
        isMaintenanceActive = maintenanceActive
    }
}