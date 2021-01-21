package com.maanmart.coronapatienttracker.data

import android.util.Log
import com.google.android.gms.maps.model.LatLng
import com.maanmart.coronapatienttracker.shared.Result
import com.maanmart.coronapatienttracker.shared.ResultCode
import com.maanmart.coronapatienttracker.shared.dto.*
import java.io.IOException
import kotlin.random.Random

private const val TAG = "DataSource"

class DataSource (private val api:CoronaTrackerApi){


    // لاگین کردن کاربر
    suspend fun personLogin(identityCode:String,mobile:String): Result<AuthUser> {
        return try {
            val loginResponse = api.login(identityCode,mobile)
            if(loginResponse.resultCode == ResultCode.OK.code){
                val userId = loginResponse.data
                if(userId!=null) Result.Success(AuthUser(userId)) else Result.Error(IOException("login failed"))
            }else {
                Result.Error(IOException("login failed"))
            }
        }catch (e: Throwable) {
            Result.Error(IOException("Error logging in", e))
        }
    }

//    fun logout() {
//        // TODO: revoke authentication
//    }

    // دریافت تنظیمات
    suspend fun getSettings(personId: String): Result<SettingResponse> {
        return try {
            val settingResponse = api.getSettings()
            if(settingResponse.resultCode == ResultCode.OK.code){
                Result.Success(settingResponse)
            }else{
                Result.Error(IOException("failed to get settings"))
            }
        }catch (e:Throwable){
            Result.Error(IOException("Error getting settings", e))
        }
    }

    // ارسال موقعیت کاربر به سرور
    suspend fun sendPersonLocation(personId:String,locations:String): Result<SavePersonLocationResponse> {
        return try {
            val locationResponse = api.sendLocations(personId,locations)
            if(locationResponse.resultCode == ResultCode.OK.code){
                Log.d(TAG,"sendPersonLocation Success")
                Result.Success(locationResponse)
            }else{
                Log.d(TAG,"sendPersonLocation Failed")
                Result.Error(IOException("failed to get settings"))
            }
        }catch (e:Throwable){
            Log.e(TAG,"sendPersonLocation Exception")
            Result.Error(IOException("Error getting settings", e))
        }
    }

    // چک کردن وضعیت بیماران کرونایی
    suspend fun checkPatientsLocation(personId:String):Result<List<List<LatLng>>>{
//        val random = Random(System.currentTimeMillis())
        return try {
            val locationResponse = api.checkCoronaPatientsLocation(personId)
            if(locationResponse.resultCode == ResultCode.OK.code){
                Log.d(TAG,"checkPatientsLocation Success: ${locationResponse.patientsLocation}")
//                if ((random.nextInt(10) < 5)) {
//                    val fakeLocations = listOf("35.302856214217876, 46.9970568031791;35.30692039001074, 46.9929694661923;35.303698317547834, 46.993999434490036;35.30075631323439, 46.99571604831958;35.29823451011434, 46.996746016617315;35.29557252156546, 46.99863429182382;35.29234999722823, 46.99983592150449;35.29389122052725, 46.99502940278175;35.29739389161076, 46.992797804803345;35.30383841032316, 46.9945144186329")
//                    Result.Success(parseLocations(fakeLocations))
//                }else{
//                    Result.Success(parseLocations(locationResponse.patientsLocation))
//                }
                Result.Success(parseLocations(locationResponse.patientsLocation))

            }else{
                Log.d(TAG,"checkPatientsLocation Failed")
                Result.Error(IOException("failed to get settings"))
            }
        }catch (e:Throwable){
            Log.e(TAG,"checkPatientsLocation Exception")
            Result.Error(IOException("Error checkingPatientsLocation", e))

        }
    }

    // پارس کردن موقعیت بیماران کرونایی جهت نمایش روی نقشه
    private fun parseLocations(locations:List<String>):List<List<LatLng>>{
        return locations.map {
            val locationsForPerson = mutableListOf<LatLng>()
            val splitList = it.split(";")
            for (i in splitList.indices) {
                val location = splitList[i].split(",")
                locationsForPerson.add(LatLng(location[0].toDouble(), location[1].toDouble()))
            }
            locationsForPerson.toList()
        }.toList()
    }
}