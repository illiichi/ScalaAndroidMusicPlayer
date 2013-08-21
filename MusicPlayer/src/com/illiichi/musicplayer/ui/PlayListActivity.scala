package com.illiichi.musicplayer.ui

import android.os.{Bundle,AsyncTask}
import android.content.{ Intent, DialogInterface, SharedPreferences,Context }
import android.view.{ View, MenuItem, Menu, ContextThemeWrapper }
import android.widget.{ Toast, ListView, ArrayAdapter, BaseAdapter, EditText, AbsListView }
import android.app.{ AlertDialog, Dialog, ListActivity, ProgressDialog }
import android.graphics.Color
import android.util.Log

import de.timroes.swipetodismiss.SwipeDismissList
import concurrent.{ ExecutionContext, future }
import scala.util.{ Success, Failure }
import ExecutionContext.Implicits.global
import scala.collection.JavaConversions

import com.illiichi.musicplayer.data.{ DbUtil, PlayList }
import com.illiichi.musicplayer.R
import com.illiichi.musicplayer.Constants

class PlayListActivity extends ListActivity with AddDialogListener {

  val TAG = this.getClass.getSimpleName

  var list: List[PlayList] = _
  var adapter: PlayListArrayAdapter = _
  var removedList: List[String] = List.empty
  override def onCreate(b: Bundle) {
    super.onCreate(b)
    list = DbUtil.getAllPlaylists(getApplicationContext())
    adapter = new PlayListArrayAdapter(this, list.toArray)
    setListAdapter(adapter)
    val callback = new DismissCallback()
    val swipeList = new SwipeDismissList(getListView(), callback, SwipeDismissList.UndoMode.SINGLE_UNDO)
  }

  override def onListItemClick(l: ListView, v: View, position: Int, id: Long) {
    val pl = getListAdapter().getItem(position).asInstanceOf[PlayList]
//    Toast.makeText(this, pl.name + " is selected", Toast.LENGTH_SHORT).show()
    val intent = new Intent(this, classOf[SongListActivity])
    intent.putExtra(Constants.PLAYLIST_ID, pl.id)
    startActivity(intent)
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    val inflater = getMenuInflater()
    inflater.inflate(R.menu.playlist, menu)
    super.onCreateOptionsMenu(menu)
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId() match {
      case R.id.action_create_playlist => {
        showAddPlaylistDialog()
        //        showCreatePlaylistDialog
        true
      }
      case _ => super.onOptionsItemSelected(item)
    }
  }

  def showAddPlaylistDialog(): Unit = {
    val fm = getFragmentManager()
    val addDialog = new AddPlaylistDialog()
    addDialog.show(fm, "fragment_add_playlist")
  }

  def onFinishAddDialog(inputText: String) {
    future {
      val prefs = PlayListActivity.this.getSharedPreferences(
        "com.illiichi.musicplayer", Context.MODE_PRIVATE)
      prefs.edit().putBoolean("DbUpdated", true).commit()
    }
    val plTitle = inputText
    new AsyncTask[AnyRef, Void, String] {
      val mDialog = new ProgressDialog(PlayListActivity.this)
      override def onPreExecute() {
        mDialog.setMessage("Creating the playlist...")
        mDialog.setCancelable(false);
        mDialog.show()
      }
      def doInBackground(p1: AnyRef*): String = {
        val newPl = DbUtil.createPlaylist(getApplicationContext(), p1.head.asInstanceOf[String])
        newPl.map(n => {
          list = list :+ n
          adapter = new PlayListArrayAdapter(PlayListActivity.this, list.toArray)
        })
        newPl.toString()
      }
      override def onPostExecute(result: String) {
        mDialog.dismiss()
        setListAdapter(adapter)
      }
    }.execute(plTitle)
  }

  override def onBackPressed() {
    super.onBackPressed
    if (removedList != null && !removedList.isEmpty) {
      val set = JavaConversions.asJavaSet(removedList.toSet)
      val prefs = PlayListActivity.this.getSharedPreferences(
        "com.illiichi.musicplayer", Context.MODE_PRIVATE)
      prefs.edit().putStringSet("removedSet", set).commit()
    }
  }

  class DismissCallback extends SwipeDismissList.OnDismissCallback {
    override def onDismiss(listView: AbsListView, position: Int): SwipeDismissList.Undoable = {
      val pl = adapter.getItem(position)
      DbUtil.deletePlaylist(PlayListActivity.this, pl.id)
      val prefs = PlayListActivity.this.getSharedPreferences(
        "com.illiichi.musicplayer", Context.MODE_PRIVATE)
      prefs.edit().putBoolean("DbUpdated", true).commit()
      removedList = pl.id.toString :: removedList
      list = DbUtil.getAllPlaylists(PlayListActivity.this)
      adapter = new PlayListArrayAdapter(PlayListActivity.this, list.toArray)
      setListAdapter(adapter)

      null
    }
  }

} 