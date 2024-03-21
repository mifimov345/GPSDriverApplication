package emk.driver.gpsdriverapplication

import android.location.Location

object LastLoginManager {
    var lastLogin: String = ""
    var pointsAmount: Int = 0
    var currentLocation: Location = Location("dummyprovider")
    var enable: String = "Скорость не превышена"

    private var onLocationIsChanged: ((Location) -> Unit)? = null

    fun setOnLocationIsChangedListener(listener: (Location) -> Unit){
        onLocationIsChanged = listener
    }

    fun updateLocation(location: Location) {
        currentLocation = location
        onLocationIsChanged?.invoke(currentLocation)
    }

    private var onLimitBrokenListener: ((String) -> Unit)? = null
    fun setEnableListener(listener: (String) -> Unit){
        onLimitBrokenListener = listener
    }

    fun updateavaliability(newAvaliability: String){
        enable = newAvaliability
        onLimitBrokenListener?.invoke(enable)
    }

    private var onPointsAmountChangedListener: ((Int) -> Unit)? = null

    fun setOnPointsAmountChangedListener(listener: (Int) -> Unit) {
        onPointsAmountChangedListener = listener
    }

    fun updatePointsAmount(newPointsAmount: Int) {
        pointsAmount = newPointsAmount
        onPointsAmountChangedListener?.invoke(pointsAmount)
    }
}