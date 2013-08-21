package com.illiichi.musicplayer.ui

import android.os.Bundle
import android.app.{ ListFragment, Activity }
import android.widget.{ Toast, AbsListView, ListView }
import android.view.View
import android.graphics.Color
import android.util.Log
import de.timroes.swipetodismiss.SwipeDismissList
import scala.concurrent._
import scala.util.{ Success, Failure }
import ExecutionContext.Implicits.global

import com.illiichi.musicplayer.{ MusicStateListener, ServiceProxy }
import com.illiichi.musicplayer.data.{ DbUtil, Song }

class SongListFragment extends ListFragment {

  private var pos: Int = _ //need to be updated when song changes?
  private var displayedPlId: Option[Long] = _
  private var adapter: SongListArrayAdapter = _
  private val TAG = this.getClass().getSimpleName()

  override def onAttach(act: Activity) {
    super.onAttach(act)
  }

  override def onCreate(b: Bundle) {
    super.onCreate(b)
    displayedPlId = getPlaylistId
    val act = getActivity()
    displayedPlId.map(id => {
      val songs: Future[Option[List[Song]]] =
        future {
          if (id == -1) ServiceProxy.allSongs else DbUtil.getSongListForPlayList(act, id)
        }
      songs onComplete {
        case Success(songList) => {
          adapter = songList match {
            case Some(list) if (list.size > 0) => new SongListArrayAdapter(act, list.toArray)
            case _ => new SongListArrayAdapter(act, Array[Song]())
          }
          getActivity().runOnUiThread(new Runnable() {
            def run() = setListAdapter(adapter)
          })
        }
        case Failure(t) => Log.e(TAG, "Error retrieving songs")
      }
    })
  }

  override def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)
    setEmptyText("No song!")
    val callback = new DismissCallback()
    val swipeList = new SwipeDismissList(getListView(), callback, SwipeDismissList.UndoMode.SINGLE_UNDO)
  }

  /*
  def updateColor(n: Int): Unit = {
    val lv = getListView()
    val firstPosition = lv.getFirstVisiblePosition() - lv.getHeaderViewsCount()
    val posChild = pos - firstPosition
    val nChild = n - firstPosition
    lv.getChildAt(posChild).setBackgroundColor(Color.BLACK)
    lv.getChildAt(nChild).setBackgroundColor(Color.DKGRAY)
    pos = n
  }
  */

  override def onListItemClick(l: ListView, v: View, position: Int, id: Long) {
    val song = getListAdapter().getItem(position).asInstanceOf[Song]
    //    updateColor(position)
    pos = position
    ServiceProxy.setPlaylist(displayedPlId.get)
    ServiceProxy.playSongAtPos(position)
    val hostAct = getActivity().asInstanceOf[HomeActivity]
    hostAct.updatePlaylist(displayedPlId, displayedPlId)
  }

  def getPlaylistId(): Option[Long] = {
    if (getArguments() == null) None
    else Option(getArguments().getLong("playlist_id"))
  }

  class DismissCallback extends SwipeDismissList.OnDismissCallback {
    override def onDismiss(listView: AbsListView, position: Int): SwipeDismissList.Undoable = {
      val act = getActivity()
      ServiceProxy.getCurrentPos.map(p => {
        pos = p
        if (displayedPlId.get == -1) {
          Toast.makeText(act,
            "Can't remove a song from the 'All' playlist!",
            Toast.LENGTH_LONG).show()
        } else if (position != pos) {

          val song = adapter.getItem(position)
          val s = DbUtil.getSongForUriStrAndTitle(act, song.uriString, song.title)
          DbUtil.removeSongFromPlaylist(act, s.get.id, displayedPlId.get)
          val songList = DbUtil.getSongListForPlayList(act, displayedPlId.get)
          adapter = songList match {
            case Some(list) if (list.size > 0) => new SongListArrayAdapter(act, list.toArray)
            case _ => new SongListArrayAdapter(act, Array[Song]())
          }
          setListAdapter(adapter)
          ServiceProxy.syncWithUI(displayedPlId.get, adapter.getCount(), (position < pos))
          Toast.makeText(act,
            song.title + " is removed from this playlist.",
            Toast.LENGTH_LONG).show()
        } else {
          Toast.makeText(act,
            "Can't remove a now-playing song!",
            Toast.LENGTH_LONG).show()
        }
      })
      null
    }
  }

}