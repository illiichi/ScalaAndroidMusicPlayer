package com.illiichi.musicplayer.ui.widget

import android.util.AttributeSet
import android.view.View
import android.view.View.OnClickListener
import android.widget.ImageButton
import android.content.{ Context, Intent }
import android.util.Log
import com.illiichi.musicplayer.{ MusicService, ServiceProxy, State, UiUtil }

class PlayPauseButton(context: Context, attrs: AttributeSet) extends ImageButton(context, attrs) with OnClickListener {
  setOnClickListener(this)

  val PLAY = "btn_play"
  val PAUSE = "btn_pause"

  def onClick(v: View): Unit = {
    ServiceProxy.togglePlayPause()
    updateState()
  }

  /**
   * shuffle between PLAY and PAUSE.
   */
  def updateState(): Unit = {
    val s = ServiceProxy.getState
    if (s == State.Paused || s == State.Stopped) {
      val play = UiUtil.getDrawable(context, PLAY)
      play.map(d => setBackgroundDrawable(d))
    } else {
      val pause = UiUtil.getDrawable(context, PAUSE)
      pause.map(d => setBackgroundDrawable(d))
    }
  }
}