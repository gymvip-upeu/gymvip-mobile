package pe.edu.upeu.gymvip.domain.model

data class MoneyPen(val cents: Long) {
    val amountInSoles: String
        get() {
            val soles = cents / 100
            val remainder = cents % 100
            val positiveRemainder = if (remainder < 0) -remainder else remainder
            val paddedRemainder = if (positiveRemainder < 10) "0$positiveRemainder" else "$positiveRemainder"
            return "S/ $soles.$paddedRemainder"
        }

    operator fun plus(other: MoneyPen): MoneyPen = MoneyPen(this.cents + other.cents)
    operator fun minus(other: MoneyPen): MoneyPen = MoneyPen(this.cents - other.cents)

    companion object {
        val ZERO = MoneyPen(0L)

        fun fromSoles(soles: Long): MoneyPen = MoneyPen(soles * 100L)

        fun fromDouble(amount: Double): MoneyPen {
            val cents = (amount * 100.0 + 0.5).toLong()
            return MoneyPen(cents)
        }
    }
}
