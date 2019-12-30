package com.samurai.mifareclassicedit.utilities

import android.content.Context
import android.widget.Toast

object Utils {
    fun formatCoin(l: Long): String {
        return "$" + String.format("%d", l)
    }

    fun leToNumeric(array: ByteArray, n: Int): Long {
        var n2 = 0L
        for (i in 0 until n) {
            n2 += array[i].toLong() and 0xFFL shl i * 8
        }
        return n2
    }

    fun leToNumericString(array: ByteArray, n: Int): String {
        return leToNumeric(array, n).toString()
    }

    fun toastMessage(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }
}
