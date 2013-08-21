package com.illiichi.musicplayer.ui

import android.app.Activity
import android.os.{ Bundle, IBinder }
import android.widget.{ AdapterView, ListView, Toast }
import android.widget.AdapterView.OnItemClickListener
import android.view.{ View, Menu, MenuItem }
import android.support.v4.app.ActionBarDrawerToggle
import android.support.v4.widget.DrawerLayout
import android.support.v4.view.GravityCompat
import android.content.res.Configuration
import android.content.{ ComponentName, Intent, Context }
import android.net.Uri
import android.util.Log
import scala.util.{ Success, Failure }
import scala.collection.JavaConversions
import concurrent.{ ExecutionContext, future }
import ExecutionContext.Implicits.global
import com.illiichi.musicplayer.{ R, ServiceProxy }
import com.illiichi.musicplayer.data.{ DbUtil, PlayList }
import com.illiichi.musicplayer.Constants

class HomeActivity extends BaseActivity {

  private var drawerToggle: ActionBarDrawerToggle = _
  private var plAdapter: PlayListArrayAdapter = _
  private var plList: List[PlayList] = _
  private var currentPlaylist: Option[Long] = _
  private var displayedPlaylist: Option[Long] = _
  private var paused: Boolean = _
  private val PICK_SONG_REQUEST = 0
  lazy val drawerLayout = findViewById(R.id.drawer_layout).asInstanceOf[DrawerLayout]
  lazy val drawerList = findViewById(R.id.left_drawer).asInstanceOf[ListView]
  override def contentView: Int = R.layout.activity_base

  override def onCreate(savedInstanceState: Bundle) {
    super.onCreate(savedInstanceState)
    val title = getTitle().toString()
    val drawerTitle = title
    drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START)
    plList = DbUtil.getAllPlaylists(this)
    plList = (new PlayList(-1, "All", 0, 0)) :: plList
    plAdapter = new PlayListArrayAdapter(this, plList.toArray)
    drawerList.setAdapter(plAdapter)
    drawerList.setOnItemClickListener(new DrawerItemClickListener())
    val ab = getActionBar()
    ab.setDisplayHomeAsUpEnabled(true)
    ab.setHomeButtonEnabled(true)
    drawerToggle = new ActionBarDrawerToggle(
      this,
      drawerLayout,
      R.drawable.ic_drawer,
      R.string.drawer_open,
      R.string.drawer_close) {
      override def onDrawerClosed(view: View): Unit = {
        getActionBar().setTitle(title)
        invalidateOptionsMenu()
      }
      override def onDrawerOpened(drawerView: View): Unit = {
        getActionBar().setTitle(drawerTitle)
        invalidateOptionsMenu()
      }
    }
    drawerLayout.setDrawerListener(drawerToggle)
    val songListFrag = new SongListFragment()

    currentPlaylist = if (!plList.isEmpty) {
      val b = new Bundle()
      val id = plList.head.id
      b.putLong("playlist_id", id)
      songListFrag.setArguments(b)
      Option(id)
    } else None
    displayedPlaylist = currentPlaylist
    getFragmentManager().beginTransaction()
      .replace(R.id.activity_base_content, songListFrag, "SongListFragment").commit()
    if (savedInstanceState == null) {
      selectItem(0)
    }
    
  }

  override def onResume() {
    super.onResume()
    if (paused) {
      val prefs = this.getSharedPreferences("com.illiichi.musicplayer", Context.MODE_PRIVATE)
      if (prefs.getBoolean("DbUpdated", false)) {
        plList = DbUtil.getAllPlaylists(this)
        plList = (new PlayList(-1, "All", 0, 0)) :: plList
        plAdapter = new PlayListArrayAdapter(this, plList.toArray)
        drawerList.setAdapter(plAdapter)
        val removedSet = prefs.getStringSet("removedSet", null)
        if (removedSet != null && !removedSet.isEmpty() && removedSet.contains(displayedPlaylist.get.toString)) {
          JavaConversions.asScalaSet(removedSet).toList.map(println)
          currentPlaylist = Option(-1)
          displayedPlaylist = currentPlaylist
          val songListFrag = new SongListFragment()
          val b = new Bundle()
          b.putLong("playlist_id", -1)
          songListFrag.setArguments(b)
          getFragmentManager().beginTransaction()
            .replace(R.id.activity_base_content, songListFrag, "SongListFragment").commit()
        }
        prefs.edit().clear().commit()
      }
      paused = false
    }
  }

  override def onPause() {
    super.onPause()
    paused = true
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater()
    inflater.inflate(R.menu.main, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onPrepareOptionsMenu(menu: Menu): Boolean = {
    val drawerOpen = drawerLayout.isDrawerOpen(drawerList)
    menu.findItem(R.id.action_add_songs).setVisible(!drawerOpen)
    menu.findItem(R.id.action_manage_playlist).setVisible(drawerOpen)
    super.onPrepareOptionsMenu(menu)
  }

  override def onPostCreate(savedInstanceState: Bundle) {
    super.onPostCreate(savedInstanceState)
    drawerToggle.syncState()
  }

  override def onConfigurationChanged(newConfig: Configuration) {
    super.onConfigurationChanged(newConfig)
    drawerToggle.onConfigurationChanged(newConfig)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    if (drawerToggle.onOptionsItemSelected(item)) true
    else {
      item.getItemId() match {
        case R.id.action_manage_playlist => {
          val intent = new Intent(this, classOf[PlayListActivity])
          intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
          startActivity(intent)
          true
        }
        case R.id.action_add_songs => {
          displayedPlaylist.map(plId => {
            if (plId != -1) {
              val intent = new Intent(this, classOf[SongListActivity])
              intent.putExtra(Constants.PLAYLIST_ID, plId)
              startActivityForResult(intent, PICK_SONG_REQUEST)
            } else Toast.makeText(this,
              "Can't add songs to the 'All' playlist.", Toast.LENGTH_SHORT).show
          })
          true
        }
        case _ => super.onOptionsItemSelected(item)
      }
    }
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, intent: Intent) {
    requestCode match {
      case PICK_SONG_REQUEST => {
        if (resultCode == Activity.RESULT_OK) {
          val uriString = intent.getStringExtra("uri_string")
          val title = intent.getStringExtra("title")
          val song = DbUtil.getSongForUriStrAndTitle(HomeActivity.this, uriString, title)
          val count = DbUtil.addSongToPlaylist(HomeActivity.this, song.get.id.toString, displayedPlaylist.get)
          val songListFrag = new SongListFragment()
          val b = new Bundle()
          b.putLong("playlist_id", displayedPlaylist.get)
          songListFrag.setArguments(b)
          getFragmentManager().beginTransaction()
            .replace(R.id.activity_base_content, songListFrag, "SongListFragment").commit()
          ServiceProxy.syncWithUI(displayedPlaylist.get, count, false)
        }
      }
      case _ =>
    }
  }

  override def setTitle(t: CharSequence) {
    val title = t.toString()
    getActionBar().setTitle(title)
  }

  override def onServiceConnected(className: ComponentName, binder: IBinder): Unit = {
    currentPlaylist.foreach(ServiceProxy.setPlaylist)
  }
  override def onServiceDisconnected(className: ComponentName): Unit = {

  }

  def updatePlaylist(currentPlaylistId: Option[Long], displayedPlaylistId: Option[Long]) {
    currentPlaylist = currentPlaylistId
    displayedPlaylist = displayedPlaylistId
  }

  def selectItem(position: Int) {
    val songListFrag = new SongListFragment()
    val b = new Bundle()
    val id = if (plList.isEmpty) -1 else plList(position).id
    b.putLong("playlist_id", id)
    songListFrag.setArguments(b)
    getFragmentManager().beginTransaction()
      .replace(R.id.activity_base_content, songListFrag, "SongListFragment").commit()
    displayedPlaylist = Option(id)
  }

  class DrawerItemClickListener extends OnItemClickListener {
    override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long) {
      selectItem(position)
    }
  }

}

