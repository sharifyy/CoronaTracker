package com.maanmart.coronapatienttracker

import android.Manifest
import android.app.Activity
import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Color
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.maps.model.PolylineOptions
import com.google.android.gms.tasks.Task
import com.maanmart.coronapatienttracker.shared.SharedState
import com.maanmart.coronapatienttracker.util.toPersian
import com.maanmart.coronapatienttracker.util.toast
import java.util.*


const val ERROR_DIALOG_REQUEST = 101
const val PERMISSIONS_REQUEST_ENABLE_GPS = 102
const val PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION = 103

private const val NOTIFICATION_CHANNEL_ID = "notification_channel_01"

class MapsActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapsViewModel: MapsViewModel
    private val TAG = "MapsActivity"
    private var defaultZoom = 13f
    private val defaultLocation = LatLng(34.323948, 47.073625)

    private lateinit var mMap: GoogleMap
    private var isLocationPermissionGranted = false
    private var gpsEnabled = false
    private lateinit var serviceIntent: Intent
    private val random = Random()

    private lateinit var alertText: TextView
    private lateinit var vibrator: Vibrator

    // چک کردن وجود بیماران کرونایی به صورت زمانبندی شده
    private val handler = Handler()
    private val runnable = object : Runnable {
        override fun run() {
            // todo send location to server and remove them from db
            getCoronaPatientsLocation()
//            handler.postDelayed(this,30 * 1000)
            handler.postDelayed(this, ((SharedState.distanceCheckInterval?.toLong()) ?: 30) * 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        createNotificationChannel()
        createLocationRequest()

        mapsViewModel = ViewModelProvider(this, MapsViewModelFactory())
                .get(MapsViewModel::class.java)

        alertText = findViewById(R.id.alertTxt)

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

        //مشاهده وضعیت وجود بیماران کرونایی
        mapsViewModel.coronaLocations.observe(this@MapsActivity, Observer {
            val locations = it ?: return@Observer

            if (locations.isNotEmpty()) {
                updateLocationsGraphOnMap(locations)
                notifyUser()
            } else {
                if (this::mMap.isInitialized) mMap.clear()
            }
            updateAlertText(locations.size)
        })

        mapsViewModel.firebaseTokenRegistration.observe(this@MapsActivity, Observer {
            if(it.isNotBlank()) Log.e(TAG,it)
        })

        registerFirebaseToken()

    }

    override fun onBackPressed() {

    }

    private fun checkBackgroundNotification(){
        val title = intent.getStringExtra("title")?:return
        val message = intent.getStringExtra("message")?:return
        if (this::mMap.isInitialized) {
            val fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return
            }
            fusedLocationClient.lastLocation
                    .addOnSuccessListener { location : Location? ->
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(
                                LatLng(location?.latitude?:defaultLocation.latitude,location?.longitude?:defaultLocation.longitude), 18f))
                    }
            updateAlertText(1)

        }
//        notifyUser(title,message)
    }

    private fun registerFirebaseToken() {
        val prefs = getSharedPreferences("AppPreffs", MODE_PRIVATE)
        val mobile = prefs.getString("mobile", null) ?: return
        val identityCode = prefs.getString("identityCode", null) ?: return
        val token = prefs.getString("firebaseToken", null) ?: return
        mapsViewModel.registerFirebaseToken(mobile,identityCode,token)
    }

    // هشدار در صورت وجود بیمار کرونایی
    private fun notifyUser(title:String = "خطر کووید ۱۹",message:String="بیمار کرونایی در اطراف شما وجود دارد") {
        val builder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_virus)
                .setContentTitle(title)
                .setContentText(message)
                .setStyle(NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVibrate(longArrayOf(1000L, 1000L, 1000L))
                .setAutoCancel(true)

        val notificationId = 13

        with(NotificationManagerCompat.from(this)) {
            // notificationId is a unique int for each notification that you must define
            notify(notificationId, builder.build())
        }

//        vibrator.vibrate(2500)
        updateAlertText(1)
    }

    // اپدیت متن وجود یا عدم وجود بیمار کرونایی
    private fun updateAlertText(size: Int) {
        if (size == 0) {
            alertText.text = "بیمار کرونایی در اطراف شما وجود ندارد"
            alertText.setBackgroundColor(Color.parseColor("#00af91"))
        } else {
            vibrator.vibrate(2500)
            alertText.text = "${size.toPersian()} بیمار کرونایی در اطراف شما وجود دارد"
            alertText.setBackgroundColor(Color.parseColor("#ff4646"))
        }
    }

    // اپدیت مسیر (ده لوکیشن آخر) بیماران کرونایی روی نقشه
    private fun updateLocationsGraphOnMap(locations: List<List<LatLng>>) {
        if (this::mMap.isInitialized) {
            locations.forEachIndexed { index, list ->
                Log.d(TAG, list.toString())
                mMap.addPolyline(PolylineOptions().add(*list.toTypedArray()).color(getRandomColor()))
                mMap.addMarker(MarkerOptions().position(list.last()).title("Covid-19: Patient-${index + 1}"))
            }

            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locations[0][0], defaultZoom))
        }
    }

    // ایجاد رنگ رندوم برای نمایش مسیر بیمار کرونایی روی نقشه
    private fun getRandomColor() = Color.argb(255, random.nextInt(256), random.nextInt(256), random.nextInt(256))

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        checkBackgroundNotification()
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        isLocationPermissionGranted = true
        startLocationService()

        mMap.isMyLocationEnabled = true
        moveCameraToDefaultLocation()
        runnable.run()

    }

    // تغییر موقعیت نقشه به شهر سنندج
    private fun moveCameraToDefaultLocation() {
        if (!this::mMap.isInitialized) return
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, defaultZoom))
    }

    //دریافت موقعیت بیماران کرونایی
    private fun getCoronaPatientsLocation() {
        val personId = SharedState.personId
        if (personId == null) finish() else mapsViewModel.getCoronaPatientsLocation(personId)
    }


    override fun onResume() {
        super.onResume()
        if (checkMapServices()) {
            if (isLocationPermissionGranted) {
                if(this::mMap.isInitialized) onMapReady(mMap)
                startLocationService()
            } else {
                getLocationPermission()
            }
        }
    }

    private fun checkMapServices(): Boolean {
        if (isServicesOK()) {
            return gpsEnabled
//            return if (isMapsEnabled()) {
//                true
//            }else{
//                createLocationRequest()
//                isLocationPermissionGranted
//            }
        }
        return false
    }

    // بررسی وضعیت googly play services روی موبایل کاربر
    private fun isServicesOK(): Boolean {

        val available = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this)

        when {
            available == ConnectionResult.SUCCESS -> {
                //everything is fine and the user can make map requests
                return true
            }
            GoogleApiAvailability.getInstance().isUserResolvableError(available) -> {
                val dialog =
                        GoogleApiAvailability.getInstance()
                                .getErrorDialog(this, available, ERROR_DIALOG_REQUEST)
                dialog.show()
            }
            else -> toast("شما قادر به استفاده از نوقعیت یاب نیستید")
        }
        return false
    }

    private fun isMapsEnabled(): Boolean {
        val manager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager

        if (!manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
//            buildAlertMessageNoGps()

            return false
        }
        return true
    }

    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("This application requires GPS to work properly, do you want to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes") { _, _ ->
                    // dialog,id
                    val enableGpsIntent =
                            Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivityForResult(enableGpsIntent, PERMISSIONS_REQUEST_ENABLE_GPS)
                }
        val alert = builder.create()
        alert.show()
    }


    // گرفتن اجازه استفاده از نقشه از کاربر
    private fun getLocationPermission() {
        /*
         * Request location permission, so that we can get the location of the
         * device. The result of the permission request is handled by a callback,
         * onRequestPermissionsResult.
         */
        if (Build.VERSION.SDK_INT > 28) {
            if (ContextCompat.checkSelfPermission(
                            this.applicationContext,
                            android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED) {

                isLocationPermissionGranted = true
                if(this::mMap.isInitialized) onMapReady(mMap)
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.ACCESS_BACKGROUND_LOCATION, android.Manifest.permission.ACCESS_FINE_LOCATION),
                        300
                )
            }
        } else {
            if (ContextCompat.checkSelfPermission(
                            this.applicationContext,
                            android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED
            ) {
                isLocationPermissionGranted = true
                if(this::mMap.isInitialized) onMapReady(mMap)
            } else {
                ActivityCompat.requestPermissions(
                        this,
                        arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                        PERMISSIONS_REQUEST_ACCESS_FINE_LOCATION
                )
            }
        }

    }

    // استارت سرویس گرفتن موقعیت کاربر به صورت زمان بندی شده در پس زمینه
    private fun startLocationService() {
        if (!isLocationServiceRunning()) {
            serviceIntent = Intent(this, LocationServiceImpl::class.java)
//            this.startService(serviceIntent)

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                this.startForegroundService(serviceIntent)
            } else {
                this.startService(serviceIntent)
            }
        }
    }

    private fun stopLocationService() {
        serviceIntent = Intent(this, LocationServiceImpl::class.java)
        this.stopService(serviceIntent)
    }

    // تعیین وضعیت سرویس موقعیت یاب
    private fun isLocationServiceRunning(): Boolean {
        val manager = this.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        for (service in manager.getRunningServices(Integer.MAX_VALUE)) {
            if (LocationServiceImpl::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(runnable)
    }

    //ایجاد کانال نوتیفیکیشن
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "NotificationChannel"
            val descriptionText = "NotificationDescription"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    // نمایش منو روی تولبار
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    // لاگ اوت کردن با کلیک بر روی منوی لاگ اوت
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle item selection
        return when (item.itemId) {
            R.id.menu_logout -> {
                val editor: SharedPreferences.Editor = getSharedPreferences("AppPreffs", MODE_PRIVATE).edit()
                editor.clear()
                editor.apply()
                startActivity(Intent(this, MainActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    // نمایش دیالوگ استفاده از gps در صورت خاموش بودن
    private fun createLocationRequest() {
        val locationRequest = LocationRequest.create()?.apply {
            interval = 10000
            fastestInterval = 5000
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder = LocationSettingsRequest.Builder()
                .addLocationRequest(locationRequest!!)

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener { locationSettingsResponse ->
            // All location settings are satisfied. The client can initialize
            // location requests here.
            // ...
            gpsEnabled = true
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException){
                // Location settings are not satisfied, but this can be fixed
                // by showing the user a dialog.
                try {
                    // Show the dialog by calling startResolutionForResult(),
                    // and check the result in onActivityResult().
                    exception.startResolutionForResult(this@MapsActivity,
                            PERMISSIONS_REQUEST_ENABLE_GPS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }

    }

    // دریافت پاسخ نمایش دیالوگ gps
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == PERMISSIONS_REQUEST_ENABLE_GPS)
            when(resultCode){
                Activity.RESULT_OK -> {
                    gpsEnabled = true
                }
            }
    }

}