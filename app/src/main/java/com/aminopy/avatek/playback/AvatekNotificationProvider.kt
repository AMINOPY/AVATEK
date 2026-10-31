@file:OptIn(UnstableApi::class)

package com.aminopy.avatek.playback

import android.app.Notification
import android.content.Context
import android.os.Bundle
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaNotification
import androidx.media3.session.MediaSession
import com.aminopy.avatek.R
import com.aminopy.avatek.data.AccentTheme
import com.aminopy.avatek.ui.theme.paletteFor
import com.google.common.collect.ImmutableList

/**
 * Notification provider with two modes. When "custom notification" is on in Settings, the media notification
 * uses AVATEK's own channel, a colorized card in the chosen accent and an AVATEK label. When it is off,
 * the plain stock Media3 song notification is used. The setting is read on every update, so it applies immediately.
 */
class AvatekNotificationProvider(private val context: Context) : MediaNotification.Provider {
    private val stock = DefaultMediaNotificationProvider(context)
    private val branded = DefaultMediaNotificationProvider.Builder(context)
        .setChannelId("avatek_branded")
        .setChannelName(R.string.channel_branded)
        .build()

    private fun prefs() = context.getSharedPreferences("avatek_settings", Context.MODE_PRIVATE)

    override fun createNotification(
        mediaSession: MediaSession,
        customLayout: ImmutableList<CommandButton>,
        actionFactory: MediaNotification.ActionFactory,
        onNotificationChangedCallback: MediaNotification.Provider.Callback
    ): MediaNotification {
        val p = prefs()
        if (!p.getBoolean("custom_notification", true)) {
            return stock.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
        }
        val n = branded.createNotification(mediaSession, customLayout, actionFactory, onNotificationChangedCallback)
        val accent = paletteFor(AccentTheme.fromId(p.getString("accent", null))).accent
        n.notification.color = android.graphics.Color.argb(
            255, (accent.red * 255).toInt(), (accent.green * 255).toInt(), (accent.blue * 255).toInt()
        )
        n.notification.extras.putBoolean("android.colorized", true)
        n.notification.extras.putCharSequence(Notification.EXTRA_SUB_TEXT, "AVATEK")
        return n
    }

    override fun handleCustomCommand(session: MediaSession, action: String, extras: Bundle): Boolean =
        stock.handleCustomCommand(session, action, extras)
}
