package com.maanmart.coronapatienttracker.shared

import android.content.Context
import android.content.Context.MODE_PRIVATE

import android.content.SharedPreferences



private const val MY_PREFS_NAME = "AppState"

class SharedPrefs(private val context: Context) {

    private val editor: SharedPreferences.Editor = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE).edit()
    private val prefs = context.getSharedPreferences(MY_PREFS_NAME, MODE_PRIVATE)

    fun setPersonId(personId: String){
        editor.putString("personId", personId)
        editor.apply()
    }

    fun setSendLocationInterval (interval: Int){
        editor.putInt("sendLocationInterval", interval)
        editor.apply()
    }

    fun setGetLocationInterval (interval: Int){
        editor.putInt("getLocationInterval", interval)
        editor.apply()
    }

    fun setDistanceCheckInterval (interval:Int){
        editor.putInt("distanceCheckInterval", interval)
        editor.apply()
    }

    fun getPersonId():String?{
        return prefs.getString("personId", null)
    }

    fun getSendLocationInterval ():Int{
        return prefs.getInt("sendLocationInterval", 120)
    }

    fun getGetLocationInterval ():Int{
        return prefs.getInt("getLocationInterval", 30)
    }

    fun getDistanceCheckInterval ():Int{
        return prefs.getInt("distanceCheckInterval", 30)
    }

}