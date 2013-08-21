package com.illiichi.musicplayer

import android.os.IBinder
import android.content.Intent
import android.net.Uri
import android.app.{ ActivityManager, Activity }
import android.app.ActivityManager.RunningServiceInfo
import android.content.{ ServiceConnection, ComponentName, ContextWrapper, Context }
import android.util.Log
import scala.collection.mutable.Map
import com.illiichi.musicplayer.data.Song

/**
 * abstract away the MusicPlayer.
 */
object ServiceProxy extends RetrieverCompletedListener {

  var service: MusicService = _
  var allSongs: Option[List[Song]] = _
  val connectionMap: scala.collection.mutable.Map[Context, MusicServiceConnection] = Map.empty

  def init(ctx: Context) = {
    val mRetriever = new Retriever(ctx.getContentResolver())
    new PrepareRetrieverTask(mRetriever, this)
  }

  def bindToService(context: Context, callback: ServiceConnection): Option[ServiceToken] = {
    val realActivity =
      Option((context.asInstanceOf[Activity]).getParent()).getOrElse(context.asInstanceOf[Activity])
    val contextWrapper = new ContextWrapper(realActivity)
    if (!isMyServiceRunning(context))
      contextWrapper.startService(new Intent(contextWrapper, classOf[MusicService]))
    val binder = new MusicServiceConnection(callback)
    if (contextWrapper.bindService(new Intent().setClass(contextWrapper, classOf[MusicService]), binder, 0)) {
      connectionMap.update(contextWrapper, binder)
      Option(ServiceToken(contextWrapper))
    } else
      None
  }

  def unbindFromService(token: ServiceToken): Unit = {
    Option(token).map({ t =>
      val mContextWrapper = t.mWrappedContext
      val mBinder = connectionMap.remove(mContextWrapper)
      mBinder.map(b => {
        mContextWrapper.unbindService(b)
        if (connectionMap.isEmpty)
          service = null
      })
    })
  }

  def getCurrentPos(): Option[Int] = if (isServiceThere) Option(service.getCurrentPos) else None

  def syncWithUI(id: Long, uiSize: Int, needUpdatePos: Boolean) = if (isServiceThere) service.syncWithUI(id, uiSize, needUpdatePos: Boolean)

  def getState(): State.Value = if (isServiceThere) service.getState else State.Stopped

  def togglePlayPause() = if (isServiceThere) service.processTogglePlaybackRequest

  def next() = if (isServiceThere) service.processSkipRequest

  def replayCurrentSong() = if (isServiceThere) service.processRewindRequest

  def setPlaylist(playListId: Long) = if (isServiceThere) service.setPlaylist(playListId)

  def getCurrentSong: Option[Song] = if (isServiceThere) Option(service.getCurrentSong) else None

  def getCurrentPlaylist: Option[Long] = if (isServiceThere) Option(service.getCurrentPlaylist) else None

  def playSongAtPos(newPos: Int): Unit = if (isServiceThere) service.playSongAtPos(newPos)

  def isMyServiceRunning(ctx: Context): Boolean = {
    val manager = ctx.getSystemService(Context.ACTIVITY_SERVICE).asInstanceOf[ActivityManager]
    manager.getRunningServices(Integer.MAX_VALUE).toArray.exists {
      x =>
        classOf[MusicService].getName() == x.asInstanceOf[RunningServiceInfo].service.getClassName()
    }
  }

  def isShuffleOn(): Option[Boolean] = if (isServiceThere) Option(service.isShuffleOn) else None

  def toggleShuffle(): Option[Boolean] =
    if (isServiceThere)
      Option(service.toggleShuffle)
    else None

  def toggleRepeat(): Option[Boolean] =
    if (isServiceThere)
      Option(service.toggleRepeat)
    else None

  override def onCompleted(songs: Option[List[Song]]) = {
    allSongs = songs
  }

  def isServiceThere = Option(service).isDefined

  class MusicServiceConnection(callback: ServiceConnection) extends ServiceConnection {
    override def onServiceConnected(className: ComponentName, binder: IBinder): Unit = {
      service = (binder.asInstanceOf[MusicService#MusicServiceBinder]).getService()
      if (callback != null) callback.onServiceConnected(className, binder)
    }

    override def onServiceDisconnected(className: ComponentName): Unit = {
      if (callback != null) callback.onServiceDisconnected(className)
      service = null
    }
  }
}

class ServiceToken(ctx: ContextWrapper) {
  val mWrappedContext = ctx
}

object ServiceToken {
  def apply(ctx: ContextWrapper) = new ServiceToken(ctx)
}