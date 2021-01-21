package com.maanmart.coronapatienttracker.shared.dto

import com.google.gson.annotations.SerializedName

data class SettingResponse(
        @SerializedName("ResultCode")
        val resultCode: Int,
        @SerializedName("GetLocationInterval")
        val getLocationInterval: Int,
        @SerializedName("SendLocationsInterval")
        val sendLocationsInterval: Int,
        @SerializedName("DistanceCheckInterval")
        val distanceCheckInterval: Int
)