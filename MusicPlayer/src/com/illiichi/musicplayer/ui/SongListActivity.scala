package com.illiichi.musicplayer.ui

import android.app.{Activity,ListActivity}
import android.widget.{ListView,ArrayAdapter}
import android.view.View
import android.os.Bundle
import com.illiichi.musicplayer.data.{DbUtil,Song}
import com.illiichi.musicplayer.ServiceProxy

class SongListActivity extends ListActivity {

  override def onCreate(b: Bundle) {
    super.onCreate(b)
    //    val intent = getIntent()
    //    val plId = intent.getLongExtra(Constants.PLAYLIST_ID, -1)
    //    val songList = if (plId != -1) DbUtil.getSongListForPlayList(getApplicationContext(), plId) else None
    val songList = ServiceProxy.allSongs
    songList.map(list => {
      if (list.size > 0)
        setListAdapter(new SongListArrayAdapter(this, list.toArray))
    })
  }

  override def onListItemClick(l: ListView, v: View, position: Int, id: Long) {
    val song = getListAdapter().getItem(position).asInstanceOf[Song]
    val intent = getIntent()
    intent.putExtra("uri_string", song.uriString)
    intent.putExtra("title", song.title)
    setResult(Activity.RESULT_OK, intent)
//    Toast.makeText(SongListActivity.this, song.title + " is selected", Toast.LENGTH_SHORT).show()
    finish()
  }
}