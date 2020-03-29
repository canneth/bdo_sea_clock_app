package com.example.bdoseatime

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log.d
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.content_main.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit


class BackupActivity : AppCompatActivity() {

    // Create notification manager
    lateinit var notification_manager: NotificationManager

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        // Create notification channel
        notification_manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        val channel_id = "com.example.bdoseatime"
        val channel_name = "main_notif_channel"
        val channel_description = "main_notif_description"
        val channel_importance = NotificationManager.IMPORTANCE_HIGH
        var channel = NotificationChannel(channel_id, channel_name, channel_importance)
        channel.description = channel_description
        channel.enableVibration(true)
        notification_manager.createNotificationChannel(channel) // Registers channel with manager
        val builder = NotificationCompat.Builder(this, channel_id) // Create builder for channel
        // Intents for notifications
        val intent = Intent(applicationContext, BackupActivity::class.java)
        val pending_intent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT)

//        notifButton.setOnClickListener {// Grab time from phone
//            val current_real_time = LocalDateTime.now()
//            // Format time
//            val time_formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
//            val formatted_time = current_real_time.format(time_formatter)
//            // Build notification
//            builder.setContentTitle("BDO SEA Clock!")
//            builder.setContentText("It is $formatted_time now in BDO SEA.")
//            builder.setSmallIcon(R.mipmap.ic_launcher)
//            builder.setContentIntent(pending_intent)
//            // Send notification
//            notification_manager.notify(0, builder.build())
//        }

        fun timeSpanInSeconds(fromDateTime: LocalDateTime, toDateTime: LocalDateTime): Long {
            var tempDateTime = LocalDateTime.from(fromDateTime)
            val years = tempDateTime.until(toDateTime, ChronoUnit.YEARS)
            tempDateTime = tempDateTime.plusYears(years)
            val months = tempDateTime.until(toDateTime, ChronoUnit.MONTHS)
            tempDateTime = tempDateTime.plusMonths(months)
            val days = tempDateTime.until(toDateTime, ChronoUnit.DAYS)
            tempDateTime = tempDateTime.plusDays(days)
            val hours = tempDateTime.until(toDateTime, ChronoUnit.HOURS)
            tempDateTime = tempDateTime.plusHours(hours)
            val minutes = tempDateTime.until(toDateTime, ChronoUnit.MINUTES)
            tempDateTime = tempDateTime.plusMinutes(minutes)
            val seconds = tempDateTime.until(toDateTime, ChronoUnit.SECONDS)
            // Convert time difference into seconds
            val seconds_between =
                (years*3.154e7
                + months*2.628e6
                + days*86400
                + hours*3600
                + minutes*60
                + seconds).toLong()
            return seconds_between
        }

        val bdo_day_time = LocalDateTime.of(1, 1, 1, 7, 0, 0)
        val bdo_night_time = LocalDateTime.of(1, 1, 1, 22, 0, 0)
        val bdo_midnight_time = LocalDateTime.of(1, 1, 2, 0, 0, 0)

        // Each 24hr BDO day is exactly equivalent to a real 4hr (240 mins) period.
        // Between 0700 BDO and 2200 BDO passes exactly 200 real minutes.
        // Between 2200 BDO to 0700 BDO passes exactly 40 real minutes.
        // At (200 * 60 + 20 * 60) seconds past 0000 HRS real time, time in BDO is exactly 0700 BDO.

        // Set reference_real_time to 0000 HRS of the current real date
        var reference_real_time = LocalDateTime.now()
        reference_real_time = reference_real_time.minusHours(reference_real_time.hour.toLong())
        reference_real_time = reference_real_time.minusMinutes(reference_real_time.minute.toLong())
        reference_real_time = reference_real_time.minusSeconds(reference_real_time.second.toLong())
        reference_real_time = reference_real_time.minusNanos(reference_real_time.nano.toLong())
        // Set reference_bdo_time to 0700 BDO
        val reference_bdo_time = bdo_day_time

        val DAY = true
        val NIGHT = false
        var last_state = DAY

        val thread: Thread = object : Thread() {
            override fun run() {
                try {
                    while (!this.isInterrupted) {
                        sleep(1000) // Run loop every 1 second
                        runOnUiThread {
                            // Grab time from phone
                            val current_real_time = LocalDateTime.now()
                            // Format time
                            val time_formatter = DateTimeFormatter.ofPattern("HH:mm:ss")
                            val date_formatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
                            val formatted_time = current_real_time.format(time_formatter)
                            val formatted_date = current_real_time.format(date_formatter)

                            // Find time difference between reference time and current time
                            var real_seconds_since_reference_real_time = timeSpanInSeconds(reference_real_time, current_real_time)
                            d("reference_real_time", "$reference_real_time")
                            real_seconds_since_reference_real_time += (200 * 60 + 20 * 60)
                            // Update bdo_time
                            var current_bdo_time = reference_bdo_time
                            if (real_seconds_since_reference_real_time >= 0) {
                                val remainder = (real_seconds_since_reference_real_time%(240*60))
                                d("remainder", "$remainder")
                                if ((remainder >= 0) and (remainder < 200 * 60)) {
                                    // Day time

                                    // Set day colours
                                    toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimaryDay))
                                    getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDarkDay))

                                    // Calculate current_bdo_time
                                    current_bdo_time = reference_bdo_time.plusSeconds((remainder * 15 * 60 / 200))
                                    d("current_bdo_time DAY", "$current_bdo_time")
                                    dayNightText.text = "Day Time"
                                    dayNightText.setTextColor(getResources().getColor(R.color.dayHighlight))

                                    if (last_state == NIGHT) {
                                        // Cancel previous notification
                                        notification_manager.cancelAll()
                                    } else {
                                        builder.setContentTitle("It's a new day!")
                                        // Build notification
                                        val formatted_current_bdo_time = current_bdo_time.format(time_formatter)
                                        builder.setContentText("It is $formatted_current_bdo_time (day time) now in BDO SEA!")
                                        builder.setSmallIcon(R.mipmap.ic_launcher)
                                        builder.setContentIntent(pending_intent)
                                        builder.setOngoing(true)
                                        builder.setOnlyAlertOnce(true)
                                        // Send notification
                                        notification_manager.notify(0, builder.build())
                                    }
                                    last_state = DAY

                                } else if (remainder >= 200 * 60) {
                                    // Night time

                                    // Set night colours
                                    toolbar.setBackgroundColor(getResources().getColor(R.color.colorPrimaryNight))
                                    getWindow().setStatusBarColor(ContextCompat.getColor(getApplicationContext(), R.color.colorPrimaryDarkNight))

                                    // Calculate current_bdo_time
                                    current_bdo_time = bdo_night_time.plusSeconds(((remainder - 200 * 60) * 9 * 60 / 40))
                                    d("current_bdo_time NIGHT", "$current_bdo_time")
                                    dayNightText.text = "Night Time"
                                    dayNightText.setTextColor(getResources().getColor(R.color.nightHighlight))

                                    if (last_state == DAY) {
                                        // Cancel previous notification
                                        notification_manager.cancelAll()
                                    } else {
                                        // Build notification
                                        builder.setContentTitle("Night has fallen!")
                                        val formatted_current_bdo_time = current_bdo_time.format(time_formatter)
                                        builder.setContentText("It is $formatted_current_bdo_time (night time) now in BDO SEA!")
                                        builder.setSmallIcon(R.mipmap.ic_launcher)
                                        builder.setContentIntent(pending_intent)
                                        builder.setOngoing(true)
                                        builder.setOnlyAlertOnce(true)
                                        // Send notification
                                        notification_manager.notify(0, builder.build())
                                    }
                                    last_state = NIGHT
                                }
                            }
                            // Display formatted current_bdo_time
                            bdo_time.text = current_bdo_time.format(time_formatter)
                        }
                    }
                } catch (e: InterruptedException) {
                    onDestroy()
                }
            }
        }
        thread.start()
    }

    override fun onDestroy() {
        notification_manager.cancelAll()
        d("Debug", "HELLO onDestroy() HAS BEEN CALLED AS PLANNED.")
        super.onDestroy()
    }
}