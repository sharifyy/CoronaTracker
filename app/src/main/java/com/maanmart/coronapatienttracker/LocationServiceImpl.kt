package com.maanmart.coronapatienttracker

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.maanmart.coronapatienttracker.data.CoronaTrackerApi
import com.maanmart.coronapatienttracker.data.DataSource
import com.maanmart.coronapatienttracker.shared.Result.Success
import com.maanmart.coronapatienttracker.shared.ResultCode
import com.maanmart.coronapatienttracker.shared.SharedState.distanceCheckInterval
import com.maanmart.coronapatienttracker.shared.SharedState.getLocationInterval
import com.maanmart.coronapatienttracker.shared.SharedState.personId
import com.maanmart.coronapatienttracker.shared.SharedState.sendLocationInterval
import kotlinx.coroutines.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.coroutines.CoroutineContext


private const val TAG = "LocationServiceImpl"

class LocationServiceImpl : Service() ,CoroutineScope{

    private lateinit var job: Job

    override val coroutineContext: CoroutineContext
        get() = job + Dispatchers.Main


    lateinit var mFusedLocationClient: FusedLocationProviderClient
    private var mLocationCallback: LocationCallback? = null

    // ارسال موقعیت کاربر به صورت زمان بندی شده
    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            // todo send location to server and remove them from db
            sendLocationsToServer()
//            handler.postDelayed(this,30 * 1000)
            handler.postDelayed(this, (sendLocationInterval?.toLong()?:120) * 1000)
        }
    }


    val builder = StringBuilder()


    override fun onCreate() {
        super.onCreate()
        job = Job()
        mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        if (Build.VERSION.SDK_INT >= 26) {
            val CHANNEL_ID = "my_channel_01"
            val channel = NotificationChannel(
                CHANNEL_ID,
                "My Channel",
                NotificationManager.IMPORTANCE_DEFAULT
            )

            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(
                channel
            )

            val notification = NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("")
                .setContentText("").build()

            startForeground(1, notification)
        }
        runnable.run()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent, flags: Int, startId: Int): Int {
        getLocation()
        return START_NOT_STICKY
    }

    // دریافت موقعیت کاربر
    private fun getLocation() {
        val gpsTime = getLocationInterval
        // ---------------------------------- LocationRequest ------------------------------------
        // Create the location request to start receiving updates
        val mLocationRequestHighAccuracy = LocationRequest()
        mLocationRequestHighAccuracy.priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        mLocationRequestHighAccuracy.interval = if (gpsTime != null && gpsTime != 0) gpsTime * 1000L else 30_000
        mLocationRequestHighAccuracy.fastestInterval = if (gpsTime != null && gpsTime != 0) gpsTime * 1000L else 30_000


        // new Google API SDK v11 uses getFusedLocationProviderClient(this)
        val locationCondition = if(Build.VERSION.SDK_INT>29)
            ActivityCompat.checkSelfPermission(  this,Manifest.permission.ACCESS_BACKGROUND_LOCATION ) != PackageManager.PERMISSION_GRANTED
        else
            ActivityCompat.checkSelfPermission(  this,Manifest.permission.ACCESS_FINE_LOCATION ) != PackageManager.PERMISSION_GRANTED
        if (locationCondition ) {
            if (Build.VERSION.SDK_INT >= 26) {
                stopForeground(true)
            } else {
                stopSelf()
            }
            return
        }
        mLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult?) {


                val location = locationResult?.lastLocation
                Log.d(TAG,"New Location: ${location?.longitude},${location?.latitude}")

                if (location != null) {
                    val df = SimpleDateFormat("HH,mm,ss", Locale.ENGLISH)
                    builder
                        .append(location.latitude)
                        .append(",")
                        .append(location.longitude)
                        .append(",")
                        .append(df.format(Calendar.getInstance().time.time))
                        .append(";")

                    Log.d(TAG, "onLocationResult: $builder")
                }
            }
        }
        mFusedLocationClient.requestLocationUpdates(mLocationRequestHighAccuracy, mLocationCallback, null)
    }


    // ارسال موقعیت کاربر به سرور
    private fun sendLocationsToServer() {
        val loginDataSource = DataSource(CoronaTrackerApi.invoke())
        launch {

            Log.d(TAG, "Send Location: $builder")
            withContext(Dispatchers.IO) {

                try {
                    val call = loginDataSource.sendPersonLocation(personId!!, builder.toString())
                    Log.d(TAG, "Sending Locations To Server Success: $builder")
                    when (call) {
                        is Success -> {
                            if(call.data.resultCode == ResultCode.OK.code){
                                sendLocationInterval = call.data.sendLocationInterval?:sendLocationInterval
                                getLocationInterval = call.data.getLocationInterval?:getLocationInterval
                                distanceCheckInterval = call.data.distanceCheckInterval?: distanceCheckInterval
                                removeLocationsFromDb()
                            }else{
                                Log.e(TAG,"sending locations to server failed")
                            }
                        }
                        else -> {
                            Log.d(TAG, "sending locations to server failed")
                        }
                    }

                } catch (error: Exception) {
                    Log.d(TAG, "Sending Locations To Server Error: ${error.message}")
                }
            }
        }
    }

    // حذف موقعیت های ارسال شده از حافظه جهت جلوگیری از ارسال مجدد
    private fun removeLocationsFromDb() {
        builder.clear()
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
        removeLocationUpdates()
        handler.removeCallbacks(runnable)
        if (Build.VERSION.SDK_INT >= 26) {
            stopForeground(true)
        } else {
            stopSelf()
        }
    }

    // کنسل کردن دریافت موقعیت کاربر
    private fun removeLocationUpdates() {
        if (mLocationCallback != null) {
            mFusedLocationClient.removeLocationUpdates(mLocationCallback)
        }
    }
}