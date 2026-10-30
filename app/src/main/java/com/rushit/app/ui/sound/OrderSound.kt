package com.rushit.app.ui.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import com.rushit.app.R

/** A short locally generated chime; it is never required for visual confirmation. */
object OrderSound {
    private const val PREFS = "rushit_sound"
    private const val KEY = "order_confirmation"
    fun isEnabled(context: Context): Boolean = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY, true)
    fun setEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean(KEY, enabled).apply()
    }

    fun playConfirmation(context: Context) {
        var player: MediaPlayer? = null
        try {
            if (!isEnabled(context)) return
            val audio = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
            if (audio.ringerMode != AudioManager.RINGER_MODE_NORMAL || audio.getStreamVolume(AudioManager.STREAM_NOTIFICATION) == 0) return
            val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
            player = MediaPlayer.create(context.applicationContext, R.raw.order_confirmation, attributes, audio.generateAudioSessionId()) ?: return
            player.setVolume(0.45f, 0.45f)
            player.setOnCompletionListener { it.release() }
            player.setOnErrorListener { media, _, _ -> media.release(); true }
            player.start()
        } catch (_: Exception) {
            player?.release()
        }
    }
}
