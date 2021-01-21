package com.maanmart.coronapatienttracker.shared.dto

import com.google.gson.annotations.SerializedName

data class SavePersonLocationResponse(
    @SerializedName("ResultCode")
    val resultCode: Int,
    @SerializedName("GetLocationInterval")
    val getLocationInterval: Int?,
    @SerializedName("SendLocationsInterval")
    val sendLocationInterval: Int?,
    @SerializedName("DistanceCheckInterval")
    val distanceCheckInterval:Int?
)