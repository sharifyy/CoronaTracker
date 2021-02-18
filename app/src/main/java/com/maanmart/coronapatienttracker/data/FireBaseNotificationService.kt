package com.maanmart.coronapatienttracker.data

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.media.RingtoneManager
import android.os.Build
import android.os.Vibrator
import android.util.Log
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.maanmart.coronapatienttracker.MainActivity
import com.maanmart.coronapatienttracker.R
import java.io.IOException

class FireBaseNotificationService : FirebaseMessagingService() {
    private val TAG  = "FireBaseNotificationService"
    private lateinit var vibrator: Vibrator

    override fun onCreate() {
        super.onCreate()
        vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }
    override fun onNewToken(token: String) {
        Log.d(TAG, "Refreshed token: $token")
        saveFirebaseToken(token)
    }

    private fun saveFirebaseToken(token: String) {
        Log.d(TAG, "sending token to server $token")
        val editor: SharedPreferences.Editor = getSharedPreferences("AppPreffs", MODE_PRIVATE).edit()
        editor.putString("firebaseToken", token)
        editor.apply()
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        Log.d(TAG,"Message Received")
        if (remoteMessage.data.isNotEmpty()) {
//            val target: String? = remoteMessage.data["target"]
            val message: String? = remoteMessage.data["message"]
//            val imgUrl: String? = remoteMessage.data["image"]
            val title: String? = remoteMessage.data["title"]
            val intent = Intent(this, MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            intent.putExtra("title",title)
            intent.putExtra("message",message)
//            intent.putExtra("target", target)
            val pendingIntent =
                PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
            //            Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            try {
                val channelId = "default"
                val builder = NotificationCompat.Builder(
                    applicationContext, channelId
                )
                if (title != null && title.isNotEmpty()) {
                    builder.setContentTitle(title)
                }
                if (message != null && message.isNotEmpty()) {
                    builder.setContentText(message)
                }
                builder.setSmallIcon(R.drawable.ic_virus)
                    .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                    .setVibrate(longArrayOf(1000L, 1000L, 1000L))
                    .setContentIntent(pendingIntent)
                builder.setStyle(NotificationCompat.BigTextStyle().bigText(message))
                builder.setAutoCancel(true) // to dismiss notification after clicking
                builder.setDefaults(Notification.DEFAULT_VIBRATE)

                val manager =
                    applicationContext.getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        channelId,
                        "Default channel",
                        NotificationManager.IMPORTANCE_HIGH
                    )
                    channel.enableVibration(true)
                    channel.vibrationPattern = longArrayOf(1000L, 1000L, 1000L)
                    manager.createNotificationChannel(channel)
                }
                manager.notify(0, builder.build())
                vibrator.vibrate(2500)
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

}