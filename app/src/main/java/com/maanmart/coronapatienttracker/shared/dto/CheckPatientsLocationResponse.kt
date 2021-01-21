package com.maanmart.coronapatienttracker.shared.dto

import com.google.gson.annotations.SerializedName

data class CheckPatientsLocationResponse(
        @SerializedName("ResultCode")
        val resultCode: Int,
        @SerializedName("PossibilityOfContacts")
        val numberOfPatients: Int = 0,
        @SerializedName("PatientsLocationsList")
        val patientsLocation: List<String>
)