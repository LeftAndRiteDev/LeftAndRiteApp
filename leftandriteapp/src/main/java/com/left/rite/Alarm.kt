package com.left.rite

import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.SoundPool
import android.os.Build
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class Alarm private constructor(val context: Guardian) {
    private var pool: SoundPool
    private var id: Int

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val audioAttributes: AudioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()
            pool = SoundPool.Builder()
                .setMaxStreams(5)
                .setAudioAttributes(audioAttributes)
                .build()
        } else {
            @Suppress("DEPRECATION")
            pool = SoundPool(5, AudioManager.STREAM_ALARM, 0)
        }
        id = pool.load(context.applicationContext, R.raw.alarm, 1)
    }

    companion object {
        private var singleton: Alarm? = null

        internal fun instance(context: Guardian): Alarm {
            var singleton = this.singleton
            if (singleton == null) {
                singleton = Alarm(context)
                this.singleton = singleton
            }
            return singleton
        }

        internal fun siren(context: Context) {
            loudest(context, AudioManager.STREAM_ALARM)
            val singleton = this.singleton
            if (singleton != null) {
                val pool = singleton.pool
                pool.play(singleton.id, 1.0f, 1.0f, 1, 3, 1.0f)
            }
        }

        internal fun loudest(context: Context, stream: Int) {
            val manager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val loudest = manager.getStreamMaxVolume(stream)
            manager.setStreamVolume(stream, loudest, 0)
        }

        private val format: SimpleDateFormat = SimpleDateFormat("yyyy.MM.dd HH:mm:ss", Locale.US)

        internal fun alert(context: Context) {

            val contact = Contact[context]
            if (contact != null && "" != contact) {
                Guardian.say(
                    context,
                    android.util.Log.WARN,
                    TAG,
                    "Alerting the emergency phone number ($contact)"
                )
                // get the Latitude and Longitude here somehow
                val battery: Int = 66
                val lat: Double = 30.1329348
                val lon: Double = -97.7711089
                val time: String = format.format(Date())
                val message =
                    "Check up on your friend. He's taken a tumble. \n " +
                            "Battery: $battery % Time: ($time): \n" +
                            "Location: $lat,$lon http://maps.google.com/?q=${lat},${lon}"
                Messenger.sms(context, Contact[context], message)
                Telephony.call(context, contact)
            } else {
                Guardian.say(context, android.util.Log.ERROR, TAG, "ERROR: Emergency phone number not set")
                siren(context)
            }
        }

        private val TAG: String = Alarm::class.java.simpleName
    }
}