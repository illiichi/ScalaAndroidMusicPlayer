package com.illiichi.musicplayer

import android.net.Uri
import android.net.wifi.WifiManager
import android.graphics.{ Bitmap, BitmapFactory }
import android.app.{ Service, Notification, NotificationManager, PendingIntent }
import android.util.Log
import android.widget.Toast
import android.os.{ Binder, IBinder, PowerManager }
import android.media.{ MediaPlayer, AudioManager, RemoteControlClient, MediaMetadataRetriever }
import android.media.MediaPlayer.{ OnCompletionListener, OnPreparedListener, OnErrorListener }
import android.content.{ Intent, Context, ComponentName, ContentUris }
import android.support.v4.content.LocalBroadcastManager
import java.io.IOException
import com.illiichi.musicplayer.scaloid.{ Logger, TagUtil }
import com.illiichi.musicplayer.data.{ DbUtil, Song }
import com.illiichi.musicplayer.ui.HomeActivity

/**
 * The backbone of the app.
 * Running in the background performing all the media handling in our application.
 */
class MusicService
  extends Service with OnCompletionListener with OnPreparedListener with OnErrorListener
  with Focusable with TagUtil with Logger {

  import MusicService._

  val sdkVersion = android.os.Build.VERSION.SDK_INT
  val notification: Notification = new Notification
  lazy val audioFocusHelper: Option[AudioFocusHelper] =
    if (sdkVersion >= 8) Some(new AudioFocusHelper(getApplicationContext(), this)) else None
  lazy val wifiLock = getSystemService(Context.WIFI_SERVICE).asInstanceOf[WifiManager]
    .createWifiLock(WifiManager.WIFI_MODE_FULL, "mylock")
  lazy val dummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.default_album_art)
  lazy val mediaButtonReceiverComponent = new ComponentName(this, classOf[PlayerIntentReceiver])
  lazy val binder: IBinder = new MusicServiceBinder()
  lazy val broadcaster: LocalBroadcastManager = LocalBroadcastManager.getInstance(this)
  lazy val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
  lazy val audioManager: AudioManager = getSystemService(Context.AUDIO_SERVICE).asInstanceOf[AudioManager]
  private var audioFocus = if (sdkVersion >= 8) AudioFocus.NoFocusNoDuck else AudioFocus.Focused
  private var state = State.Retrieving
  private var whatToPlayAfterRetrieve: Option[Song] = None
  private var songTitle: String = _
  private var streaming: Boolean = _
  private var player: MediaPlayer = _
  private var shuffle: Boolean = _
  private var repeat: Boolean = _
  private var remoteControlClient: RemoteControlClient = _
  private var startPlayingAfterRetrieve: Boolean = _
  private var currentList: List[Song] = _
  private var pos: Int = _
  private var playlistId: Long = _

  override def onCreate = {
  }

  override def onDestroy = {
    state = State.Stopped
    relaxResources(true)
    giveUpAudioFocus()
  }
  override def onStartCommand(intent: Intent, flags: Int, startId: Int) = {
    state = State.Stopped
    tryToGetAudioFocus()
    Service.START_NOT_STICKY
  }

  /** Called when media player is done playing current song. */
  override def onCompletion(player: MediaPlayer): Unit = {
    if (repeat) {
      // we repeat the current song if the repeat mode is on.
      playNextSong(Option(currentList(pos)))
    } else if (shuffle) {
      // get a random position in the list if the random mode is on.
      pos = getRandomPos()
      playNextSong(Option(currentList(pos)))
    } else {
      if (pos < currentList.length - 1) {
        pos += 1
        playNextSong(Option(currentList(pos)))
      } else {
        // stop playing at the end of the playlist.
        pos = 0
        state = State.Stopped
        broadcast(MusicService.ACTION_SONG_CHANGED)
        relaxResources(true)
        giveUpAudioFocus()
        Option(remoteControlClient).map(_.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED))
      }
    }
    broadcast(MusicService.ACTION_SONG_CHANGED)
  }
  /** Called when media player is done preparing. */
  override def onPrepared(player: MediaPlayer) = {
    state = State.Playing
    updateNotification(songTitle + " (playing)")
    configAndStartMediaPlayer()
  }
  /**
   * Called when there's an error playing media. When this happens, the media player goes to
   * the Error state. We warn the user about the error and reset the media player.
   */
  override def onError(mp: MediaPlayer, what: Int, extra: Int): Boolean = {
    Toast.makeText(getApplicationContext(), "Media player error! Resetting.", Toast.LENGTH_SHORT).show()
    state = State.Stopped
    relaxResources(true)
    giveUpAudioFocus()
    true
  }

  override def onGainedAudioFocus: Unit = {
    audioFocus = AudioFocus.Focused
    if (state == State.Playing)
      configAndStartMediaPlayer()
  }
  override def onLostAudioFocus(canDuck: Boolean): Unit = {
    audioFocus = if (canDuck) AudioFocus.NoFocusCanDuck else AudioFocus.NoFocusNoDuck
    if (player != null && player.isPlaying())
      configAndStartMediaPlayer()
  }

  def playSongAtPos(newPos: Int): Unit = {
    if (0 <= newPos && newPos < currentList.length) {
      tryToGetAudioFocus()
      pos = newPos
      playNextSong(Option(currentList(pos)))
      state = State.Playing
      broadcast(MusicService.ACTION_SONG_CHANGED)
    }
  }

  def getCurrentPos: Int = pos

  def getCurrentPlaylist: Long = playlistId

  def getCurrentSong: Song = currentList(pos)

  def addToCurrentPlaylist(uri: String) = currentList :+ uri

  def isShuffleOn(): Boolean = shuffle

  def toggleShuffle(): Boolean = {
    shuffle = !shuffle
    shuffle
  }

  def toggleRepeat(): Boolean = {
    repeat = !repeat
    repeat
  }

  /** updated the playlist and position when the user adds or removes a song from the playlist. */
  def syncWithUI(id: Long, uiSize: Int, needUpdatePos: Boolean): Unit = {
    if (uiSize != currentList.size && playlistId == id) {
      val newList = DbUtil.getSongListForPlayList(getApplicationContext(), id)
      newList.map(l => {
        currentList = l
      })
      if (needUpdatePos) pos = pos - 1
    }
  }

  def setPlaylist(id: Long): Unit = {
    playlistId = id
    val newList = if (id == -1) ServiceProxy.allSongs else DbUtil.getSongListForPlayList(getApplicationContext(), id)
    newList.map(l => {
      currentList = l
      pos = 0
      broadcast(MusicService.ACTION_SONG_CHANGED)
    })
  }

  /** broadcast the current position so the UI get updated. */
  def broadcast(what: String): Unit = {
    val intent = new Intent(what)
    intent.putExtra("pos", pos)
    sendStickyBroadcast(intent)
  }

  def getRandomPos(): Int = {
    val r = (Math.random * (currentList.length - 1)).round.toInt
    if (r == pos) getRandomPos() else r
  }

  def tryToGetAudioFocus() {
    if (audioFocus != AudioFocus.Focused)
      audioFocusHelper.map(helper => {
        helper.requestFocus
        audioFocus = AudioFocus.Focused
      })
  }
  def giveUpAudioFocus() {
    if (audioFocus == AudioFocus.Focused)
      audioFocusHelper.map(helper => {
        helper.abandonFocus
        audioFocus = AudioFocus.NoFocusNoDuck
      })
  }

  def relaxResources(releaseMediaPlayer: Boolean) {
    // stop being a foreground service
    stopForeground(true)
    if (releaseMediaPlayer && player != null) {
      player.reset()
      player.release()
      player = null
    }
    if (wifiLock.isHeld()) wifiLock.release()
  }

  def updateNotification(text: String) {
    val pi = PendingIntent.getActivity(getApplicationContext(), 0,
      new Intent(getApplicationContext(), classOf[HomeActivity]), PendingIntent.FLAG_UPDATE_CURRENT)
    notification.setLatestEventInfo(getApplicationContext(), "SPlayer", text, pi)
    notificationManager.notify(NOTIFICATION_ID, notification)
  }
  def setUpAsForeground(text: String) {
    val pi = PendingIntent.getActivity(getApplicationContext(), 0,
      new Intent(getApplicationContext(), classOf[HomeActivity]), PendingIntent.FLAG_UPDATE_CURRENT)
    notification.tickerText = text
    notification.icon = R.drawable.ic_stat_playing
    notification.flags |= Notification.FLAG_ONGOING_EVENT
    notification.setLatestEventInfo(getApplicationContext(), "SPlayer",
      text, pi)
    startForeground(NOTIFICATION_ID, notification)
  }

  def playNextSong(song: Option[Song]) {
    state = State.Stopped
    relaxResources(false)
    try {
      createMediaPlayerIfNeeded()
      player.setAudioStreamType(AudioManager.STREAM_MUSIC)
      song.map(s => {
        player.setDataSource(getApplicationContext(), s.uri)
        songTitle = s.title
        state = State.Preparing
        setUpAsForeground(songTitle + " (loading)")
        sendCoverUri(Option(s.album.id.toString))
        audioManager.registerMediaButtonEventReceiver(mediaButtonReceiverComponent)
        if (remoteControlClient == null) {
          val intent = new Intent(Intent.ACTION_MEDIA_BUTTON)
          intent.setComponent(mediaButtonReceiverComponent)
          remoteControlClient = new RemoteControlClient(
            PendingIntent.getBroadcast(this,
              0, intent, 0))
          audioManager.registerRemoteControlClient(remoteControlClient)
        }
        remoteControlClient.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING)
        remoteControlClient.setTransportControlFlags(
          RemoteControlClient.FLAG_KEY_MEDIA_PREVIOUS
            | RemoteControlClient.FLAG_KEY_MEDIA_NEXT
            | RemoteControlClient.FLAG_KEY_MEDIA_PLAY
            | RemoteControlClient.FLAG_KEY_MEDIA_PAUSE
            | RemoteControlClient.FLAG_KEY_MEDIA_PLAY_PAUSE
            | RemoteControlClient.FLAG_KEY_MEDIA_STOP)
        remoteControlClient.editMetadata(true)
          .putString(MediaMetadataRetriever.METADATA_KEY_ARTIST, s.artist.name)
          .putString(MediaMetadataRetriever.METADATA_KEY_ALBUM, s.album.title)
          .putString(MediaMetadataRetriever.METADATA_KEY_TITLE, s.title)
          .putLong(MediaMetadataRetriever.METADATA_KEY_DURATION, s.duration)
          .putBitmap(remoteControlClient.MetadataEditor.BITMAP_KEY_ARTWORK, s.album.getCover(this, 2).getOrElse(dummyAlbumArt)).apply()
        player.prepareAsync()
        broadcast(MusicService.ACTION_SONG_CHANGED)
        if (streaming) wifiLock.acquire()
        else if (wifiLock.isHeld()) wifiLock.release()
      })
    } catch {
      case ex: IOException => {
        Log.e("MusicService", "IOException playing next song: " + ex.getMessage())
        ex.printStackTrace
      }
    }
  }

  def createMediaPlayerIfNeeded() {
    if (player == null) {
      player = new MediaPlayer()
      player.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK)
      player.setOnPreparedListener(this)
      player.setOnCompletionListener(this)
      player.setOnErrorListener(this)
    } else player.reset()
  }

  def processTogglePlaybackRequest() {
    if (state == State.Paused || state == State.Stopped)
      processPlayRequest()
    else
      processPauseRequest()
  }

  def processPlayRequest() {
    if (state == State.Retrieving) {
      whatToPlayAfterRetrieve = Option(currentList(pos))
      startPlayingAfterRetrieve = true
    } else {
      tryToGetAudioFocus()
      if (state == State.Stopped) {
        playNextSong(Option(currentList(pos)))
      } else if (state == State.Paused) {
        state = State.Playing
        setUpAsForeground(songTitle + " (playing)")
        configAndStartMediaPlayer()
      }
      Option(remoteControlClient).map(_.setPlaybackState(RemoteControlClient.PLAYSTATE_PLAYING))
    }
  }

  def processPauseRequest() {
    if (state == State.Retrieving) {
      startPlayingAfterRetrieve = false
    } else {
      if (state == State.Playing) {
        state = State.Paused
        player.pause()
        relaxResources(false)
      }
      Option(remoteControlClient).map(_.setPlaybackState(RemoteControlClient.PLAYSTATE_PAUSED))
    }
  }

  def processSkipRequest() {
    if (state == State.Playing || state == State.Paused) {
      tryToGetAudioFocus()
      if (shuffle) {
        pos = getRandomPos()
        playNextSong(Option(currentList(pos)))
      } else if (pos < currentList.length - 1) {
        pos += 1
        playNextSong(Option(currentList(pos)))
      }
    }
  }

  def processRewindRequest() {
    if (state == State.Playing || state == State.Paused) {
      if (player.getCurrentPosition() > 2000 || pos == 0)
        player.seekTo(0)
      else if (pos > 0) {
        if (shuffle) {
          pos = getRandomPos()
          playNextSong(Option(currentList(pos)))
        } else {
          pos -= 1
          playNextSong(Option(currentList(pos)))
        }
      }
    }
  }

  def solfStopRequest() = processStopRequest(false)
  def processStopRequest(force: Boolean) = {
    if (state == State.Playing || state == State.Paused || force) {
      state = State.Stopped
      relaxResources(true)
      giveUpAudioFocus()
      Option(remoteControlClient).map(_.setPlaybackState(RemoteControlClient.PLAYSTATE_STOPPED))
      stopSelf()
    }
  }

  def configAndStartMediaPlayer() {
    audioFocus match {
      case AudioFocus.NoFocusNoDuck => if (player.isPlaying()) player.pause()
      case AudioFocus.NoFocusCanDuck => {
        player.setVolume(DUCK_VOLUME, DUCK_VOLUME)
        if (!player.isPlaying) player.start()
      }
      case _ => {
        player.setVolume(1.0f, 1.0f)
        if (!player.isPlaying) player.start()
      }
    }
  }

  def getCover(albumId: Long): Option[Bitmap] = {
    try {
      val uri = ContentUris.withAppendedId(
        Uri.parse("content://media/external/audio/albumart"), albumId)
      val in = getContentResolver.openInputStream(uri)
      Option(BitmapFactory.decodeStream(in))
    } catch {
      case e: java.io.FileNotFoundException => {
        Log.e("Music", s"Exception when reading cover image: ${e.getMessage}")
        None
      }
      case e: java.lang.SecurityException => {
        Log.e("Music", s"Exception when reading cover image: ${e.getMessage}")
        None
      }
    }
  }

  def sendCoverUri(id: Option[String]) = {
    val intent = new Intent(ACTION_COVER)
    intent.putExtra(COVER_ID, id.getOrElse(""))
    broadcaster.sendBroadcast(intent)
  }

  def isPlaying(): Boolean = if (player != null && player.isPlaying()) true else false
  def getState(): State.Value = state

  override def onBind(intent: android.content.Intent): android.os.IBinder = binder

  class MusicServiceBinder extends Binder {
    def getService(): MusicService = {
      MusicService.this
    }
  }
}

object MusicService {
  val ACTION_TOGGLE_PLAYBACK = "com.illiichi.musicplayer.TOGGLE_PLAYBACK"
  val ACTION_PLAY = "com.illiichi.musicplayer.action.PLAY"
  val ACTION_PAUSE = "com.illiichi.musicplayer.action.PAUSE"
  val ACTION_STOP = "com.illiichi.musicplayer.action.STOP"
  val ACTION_SKIP = "com.illiichi.musicplayer.action.SKIP"
  val ACTION_REWIND = "com.illiichi.musicplayer.action.REWIND"
  val ACTION_URL = "com.illiichi.musicplayer.action.URL"
  val ACTION_COVER = "com.illiichi.musicplayer.action.COVER"
  val ACTION_SONG_CHANGED = "com.illiichi.musicplayer.action.SONG_CHANGED"
  val COVER_ID = "com.illiichi.musicplayer.COVER_URI"
  val DUCK_VOLUME = 0.1f
  val NOTIFICATION_ID = 1
}