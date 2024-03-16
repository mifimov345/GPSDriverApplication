package emk.driver.gpsdriverapplication

object LastLoginManager {
    var lastLogin: String = ""
    var pointsAmount: Int = 0

    private var onPointsAmountChangedListener: ((Int) -> Unit)? = null

    fun setOnPointsAmountChangedListener(listener: (Int) -> Unit) {
        onPointsAmountChangedListener = listener
    }

    fun updatePointsAmount(newPointsAmount: Int) {
        pointsAmount = newPointsAmount
        onPointsAmountChangedListener?.invoke(pointsAmount)
    }
}