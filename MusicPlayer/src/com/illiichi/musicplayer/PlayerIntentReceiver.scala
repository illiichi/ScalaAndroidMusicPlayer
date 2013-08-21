package com.illiichi.musicplayer

import android.content.{ BroadcastReceiver, Context, Intent }
import android.util.Log
import android.view.KeyEvent

class PlayerIntentReceiver extends BroadcastReceiver {

  override def onReceive(context: Context, intent: Intent) {
    intent.getAction match {
      case android.media.AudioManager.ACTION_AUDIO_BECOMING_NOISY => Log.v("PlayerIntentReceiver", "Headphones disconnected.")
      case Intent.ACTION_MEDIA_BUTTON => {
        val keyEvent = intent.getExtras().get(Intent.EXTRA_KEY_EVENT).asInstanceOf[KeyEvent]
        if (keyEvent.getAction() != KeyEvent.ACTION_DOWN) None
        else startService(context, keyEvent.getKeyCode)
      }
    }
  }

  def startService(context: Context, code: Int): Unit = {
    code match {
      case KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE =>
        ServiceProxy.togglePlayPause
      case KeyEvent.KEYCODE_MEDIA_PLAY =>
      case KeyEvent.KEYCODE_MEDIA_PAUSE =>
      case KeyEvent.KEYCODE_MEDIA_STOP =>
      case KeyEvent.KEYCODE_MEDIA_NEXT =>
        ServiceProxy.next
      case KeyEvent.KEYCODE_MEDIA_PREVIOUS =>
        ServiceProxy.replayCurrentSong
      case KeyEvent.KEYCODE_HEADSETHOOK => None
    }
  }
}