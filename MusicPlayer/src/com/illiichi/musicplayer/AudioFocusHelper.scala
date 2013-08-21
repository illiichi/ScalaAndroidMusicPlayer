package com.illiichi.musicplayer

import android.content.Context
import android.media.AudioManager
import com.illiichi.musicplayer.scaloid.{ TagUtil, Logger }

/**
 * Helper class to deal with audio focus.
 * It helps request and abandon focus,
 * and will intercept focus change events and deliver them to a MusicFocusable interface.
 */
class AudioFocusHelper(ctx: Context, focusable: Focusable)
  extends AudioManager.OnAudioFocusChangeListener with TagUtil with Logger {
  val mAM = ctx.getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]
  val mFocusable: Focusable = focusable

  /** Requests audio focus. Returns whether request was successful or not. */
  def requestFocus: Boolean =
    AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
      mAM.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

  /** Abandons audio focus. Returns whether request was successful or not. */
  def abandonFocus: Boolean =
    AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAM.abandonAudioFocus(this)

  /**
   * Called by AudioManager on audio focus changes. We implement this by calling our
   * MusicFocusable appropriately to relay the message.
   */
  def onAudioFocusChange(focusChange: Int) = focusChange match {
    case AudioManager.AUDIOFOCUS_GAIN => mFocusable.onGainedAudioFocus
    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT => mFocusable.onLostAudioFocus(false)
    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK => mFocusable.onLostAudioFocus(true)
    case _ => wtf("Shouldn't get here")
  }
}