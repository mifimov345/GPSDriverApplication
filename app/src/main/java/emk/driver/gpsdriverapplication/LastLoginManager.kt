package emk.driver.gpsdriverapplication

object LastLoginManager {
    var lastLogin: String = ""
    var pointsAmount: Int = 0
    var enable: String = "Скорость не превышена"

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