package com.illiichi.musicplayer.ui

import android.app.{ListFragment,Activity}
import android.widget.{Toast,ListView,ArrayAdapter}
import android.view.View
import android.os.Bundle
import android.content.Intent
import com.illiichi.musicplayer.data.{PlayList,DbUtil}
import com.illiichi.musicplayer.Constants

class PlayListFragment extends ListFragment {

  override def onActivityCreated(savedInstanceState: Bundle) {
    super.onActivityCreated(savedInstanceState)
    setEmptyText("You've no playlist.")
  }

  override def onAttach(act: Activity) {
    super.onAttach(act)
  }

  override def onCreate(b: Bundle) {
    super.onCreate(b)
    val act = getActivity()
    val list = DbUtil.getAllPlaylists(act)
    setListAdapter(new PlayListArrayAdapter(act, list.toArray))
  }

  override def onListItemClick(l: ListView, v: View, position: Int, id: Long) {
    val act = getActivity()
    val pl = getListAdapter().getItem(position).asInstanceOf[PlayList]
    Toast.makeText(act, pl.name + " is selected", Toast.LENGTH_SHORT).show()
    val intent = new Intent(act, classOf[SongListActivity])
    intent.putExtra(Constants.PLAYLIST_ID, pl.id)
    startActivity(intent)
  }

}
