package com.maeda.player

import android.media.MediaPlayer
import android.os.Environment

import org.scaloid.common._

class Controller extends LocalService {
  Player init

  def togglePlayPause: Boolean = isPlaying match {
    case true => { Player pause; false }
    case false => { Player play; true }
  }

  def isPlaying = Player isPlaying

  def selectPlaySong(songIndex: Int) = {

  }
  //  def stop = ???

}

object Player extends MediaPlayer.OnPreparedListener
  with MediaPlayer.OnErrorListener
  with MediaPlayer.OnCompletionListener {
  val mp = new MediaPlayer
  def init = {
    mp.setOnPreparedListener(this)
    mp.setOnErrorListener(this)
    mp.setOnCompletionListener(this)
    mp.reset()
    mp.setDataSource(Environment.getExternalStorageDirectory.getAbsolutePath + "/song.mp3")
    mp.prepareAsync()
  }

  /*
  def selectPlaySong(uri: Uri) = {
    mp.reset()
    mp.setDataSource(uri)
    mp.prepareAsync();
  }
  * */
  

  def play = {
    mp.start()
  }
  def pause = {
    mp.pause()
  }
  def isPlaying = mp.isPlaying()

  //MediaPlayer callbacks
  override def onPrepared(m: MediaPlayer) = {

  }

  override def onCompletion(m: MediaPlayer) = {
    m.stop()
  }

  override def onError(p: MediaPlayer, what: Int, extra: Int) = {
    //error("onError")
    true
  }
}

