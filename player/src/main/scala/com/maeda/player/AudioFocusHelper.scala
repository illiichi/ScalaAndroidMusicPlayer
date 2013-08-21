package com.maeda.player

import android.content.Context
import android.media.AudioManager
import android.util.Log
import org.scaloid.common._

class AudioFocusHelper(ctx: Context, focusable: MusicFocusable)
  extends AudioManager.OnAudioFocusChangeListener with TagUtil with Logger {
  //  require(focusable != null, "")

  val mAM = ctx.getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]
  val mFocusable: MusicFocusable = focusable

  def requestFocus: Boolean =
    AudioManager.AUDIOFOCUS_REQUEST_GRANTED ==
      mAM.requestAudioFocus(this, AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN)

  def abandonFocus: Boolean =
    AudioManager.AUDIOFOCUS_REQUEST_GRANTED == mAM.abandonAudioFocus(this)

  def onAudioFocusChange(focusChange: Int) = focusChange match {
    case AudioManager.AUDIOFOCUS_GAIN => mFocusable.onGainedAudioFocus
    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT => mFocusable.onLostAudioFocus(false)
    case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK =>  mFocusable.onLostAudioFocus(true)
    case _ => wtf("Shouldn't get here")
  }
}