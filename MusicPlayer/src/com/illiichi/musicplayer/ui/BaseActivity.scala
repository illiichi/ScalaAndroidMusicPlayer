package com.illiichi.musicplayer.ui

import android.content.{ Intent, BroadcastReceiver, Context, IntentFilter, ServiceConnection, ComponentName }
import android.widget.{ TextView, ImageView, Button }
import android.graphics.BitmapFactory
import android.os.{ IBinder, Environment }
import android.net.Uri
import android.view.View
import android.view.View.OnClickListener
import com.illiichi.musicplayer.scaloid.SActivity
import com.illiichi.musicplayer.{
  MusicStateListener,
  MusicService,
  ServiceProxy,
  ServiceToken,
  R,
  RetrieverCompletedListener,
  Retriever,
  PrepareRetrieverTask
}
import com.illiichi.musicplayer.ui.widget.PlayPauseButton
import com.illiichi.musicplayer.data.Song

trait BaseActivity extends SActivity with ServiceConnection {

  lazy val receiver = new MusicStateReceiver()
  lazy val filter = new IntentFilter()
  //ControllerView
  lazy val albumCoverIV: ImageView = findViewById(R.id.album_cover).asInstanceOf[ImageView]
  lazy val playPauseBtn: PlayPauseButton = findViewById(R.id.btn_play_pause).asInstanceOf[PlayPauseButton]
  lazy val shuffleBtn: Button = findViewById(R.id.btn_shuffle).asInstanceOf[Button]
  lazy val repeatBtn: Button = findViewById(R.id.btn_repeat).asInstanceOf[Button]
  lazy val previousBtn: Button = findViewById(R.id.btn_previous).asInstanceOf[Button]
  lazy val nextBtn: Button = findViewById(R.id.btn_next).asInstanceOf[Button]
  lazy val trackNameTV: TextView = findViewById(R.id.view_controller_info_line_one).asInstanceOf[TextView]
  lazy val artistNameTV: TextView = findViewById(R.id.view_controller_info_line_two).asInstanceOf[TextView]

  lazy val mDummyAlbumArt = BitmapFactory.decodeResource(getResources(), R.drawable.default_album_art)
  private var token: Option[ServiceToken] = _

  onCreate {
    filter.addAction(MusicService.ACTION_SONG_CHANGED)
    setContentView(contentView)
    initControllerView
    //scan the sdcard
    sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED, Uri.parse("file://" + Environment.getExternalStorageDirectory())))
    //start MusicService if it's not started yet.
    ServiceProxy.init(this)
    if (!ServiceProxy.isMyServiceRunning(this) || token == None) //startService(new Intent(this, classOf[MusicService]))
      token = ServiceProxy.bindToService(this, this)
  }

  onResume {
    registerReceiver(receiver, filter)
  }

  onPause {
    try {
      unregisterReceiver(receiver)
    } catch {
      case ex: Exception => error(ex.toString())
    }
  }

  onDestroy {
    token.map(t => ServiceProxy.unbindFromService(t))
  }

  def initControllerView: Unit = {
    shuffleBtn.setOnClickListener(new OnClickListener() {
      def onClick(v: View) {
        val btnState = ServiceProxy.toggleShuffle()
        btnState.map(s => {
          if (s) shuffleBtn.setBackgroundResource(R.drawable.shuffle_pressed)
          else shuffleBtn.setBackgroundResource(R.drawable.shuffle)
        })
      }
    })
    repeatBtn.setOnClickListener(new OnClickListener() {
      def onClick(v: View) {
        val btnState = ServiceProxy.toggleRepeat()
        btnState.map(s => {
          if (s) repeatBtn.setBackgroundResource(R.drawable.repeat_pressed)
          else repeatBtn.setBackgroundResource(R.drawable.repeat)
        })
      }
    })
    previousBtn.setOnClickListener(new OnClickListener() {
      def onClick(v: View) {
        ServiceProxy.replayCurrentSong()
        playPauseBtn.updateState()
      }
    })
    nextBtn.setOnClickListener(new OnClickListener() {
      def onClick(v: View) {
        ServiceProxy.next()
        playPauseBtn.updateState()
      }
    })
  }

  def contentView: Int

  override def onServiceConnected(className: ComponentName, binder: IBinder): Unit = {
    updateControllerBar()
  }
  override def onServiceDisconnected(className: ComponentName): Unit = {

  }

  def updateControllerBar(): Unit = {
    val song = ServiceProxy.getCurrentSong.map(s => {
      if (s != null) {
        playPauseBtn.updateState()
        trackNameTV.setText(s.title)
        artistNameTV.setText(s.artist.name)
        albumCoverIV.setImageBitmap(s.album.getCover(getApplicationContext(), 4).getOrElse(mDummyAlbumArt))
        //        .map(b => albumCoverIV.setImageBitmap(b))
      } else println("song is null")
    })
  }

  class MusicStateReceiver extends BroadcastReceiver {
    override def onReceive(context: Context, intent: Intent) {
      intent.getAction match {
        case MusicService.ACTION_SONG_CHANGED => {
          val pos = intent.getIntExtra("pos", 0)
          //          val fragment = getFragmentManager().findFragmentByTag("SongListFragment").asInstanceOf[SongListFragment]
          //          if (fragment != null)
          //            fragment.updateColor(pos)
          updateControllerBar
        }
      }
    }
  }
}