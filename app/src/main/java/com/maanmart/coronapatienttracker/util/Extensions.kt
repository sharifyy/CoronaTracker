package com.maanmart.coronapatienttracker.util

import android.content.Context
import android.widget.Toast

fun Context.toast(message: String) {
    Toast.makeText(this, message, Toast.LENGTH_LONG).show()
}

fun Int.toPersian():String{
    val number = this.toString()
    var newText = ""
    for (element in number) {
        val ch = element
        val newChar: Char
        newChar = when (ch) {
            '0' -> '۰'
            '1' -> '۱'
            '2' -> '۲'
            '3' -> '۳'
            '4' -> '۴'
            '5' -> '۵'
            '6' -> '۶'
            '7' -> '۷'
            '8' -> '۸'
            '9' -> '۹'
            else -> ch
        }
        newText += newChar
    }
    return newText
}