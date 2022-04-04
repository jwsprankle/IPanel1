package com.example.i_panel1

val axes3_1 = Axes3(1.0, 1.0, 1.0)
val axes3_2 = Axes3(2.0, 2.0, 2.0)

fun byteArrayToInt(buffer: ByteArray, offset: Int): Int {
    return ((buffer[offset + 1].toInt() shl 8) or
            (buffer[offset + 0].toInt() and 0xff))
}


class Axes3( var axisX: Double = 0.0, var axisY: Double = 0.0, var axisZ: Double = 0.0, var scale: Double = 1.0) {

    companion object Factory {
        fun create(array: ByteArray, offset: Int, scale: Double = 1.0): Axes3 {
            val x = byteArrayToInt(array, offset).toDouble() * scale
            val y = byteArrayToInt(array, offset + 2).toDouble() * scale
            val z = byteArrayToInt(array, offset + 4).toDouble() * scale
            return Axes3(x, y, z)
        }
    }

    fun scale(scalVal: Double) {
        axisX *= scalVal
        axisY *= scalVal
        axisZ *= scalVal
    }

    fun getMin(otherAxis: Axes3) : Axes3 {
        var minAxes = Axes3()
        minAxes.axisX = minOf(axisX, otherAxis.axisX)
        minAxes.axisY = minOf(axisY, otherAxis.axisY)
        minAxes.axisZ = minOf(axisZ, otherAxis.axisZ)

        return minAxes
    }

    fun getMax(otherAxis: Axes3) : Axes3 {
        var minAxes = Axes3()
        minAxes.axisX = minOf(axisX, otherAxis.axisX)
        minAxes.axisY = minOf(axisY, otherAxis.axisY)
        minAxes.axisZ = minOf(axisZ, otherAxis.axisZ)

        return minAxes
    }

    fun add(otherAxis: Axes3) : Axes3 {
        var retAxis = Axes3()
        retAxis.axisX = axisX + otherAxis.axisX
        retAxis.axisY = axisY + otherAxis.axisY
        retAxis.axisZ = axisZ + otherAxis.axisZ

        return retAxis
    }


    fun subtract(otherAxis: Axes3) : Axes3 {
        var retAxis = Axes3()
        retAxis.axisX = axisX - otherAxis.axisX
        retAxis.axisY = axisY - otherAxis.axisY
        retAxis.axisZ = axisZ - otherAxis.axisZ

        return retAxis
    }

    fun multiply(multiplier: Axes3) : Axes3 {
        var retAxis = Axes3()
        retAxis.axisX = axisX * multiplier.axisX
        retAxis.axisY = axisY * multiplier.axisY
        retAxis.axisZ = axisZ * multiplier.axisZ

        return retAxis
    }

    fun divide(divisor: Axes3) : Axes3 {
        var retAxis = Axes3()
        retAxis.axisX = axisX / divisor.axisX
        retAxis.axisY = axisY / divisor.axisY
        retAxis.axisZ = axisZ / divisor.axisZ
        return retAxis
    }
}




