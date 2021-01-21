package com.maanmart.coronapatienttracker.shared.dto

import com.google.gson.annotations.SerializedName

data class LoginResponse (
        @SerializedName("ResultCode")
        val resultCode:Int,
        @SerializedName("Data")
        val data:String?)