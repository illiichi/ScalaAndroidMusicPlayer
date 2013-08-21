package com.illiichi.musicplayer.ui

import android.app.Activity
import android.widget.{ ImageView, TextView, ArrayAdapter }
import android.view.{ ViewGroup, View }
import com.illiichi.musicplayer.R
import com.illiichi.musicplayer.data.{ PlayList, DbUtil }

class PlayListArrayAdapter(ctx: Activity, playLists: Array[PlayList]) extends ArrayAdapter[PlayList](ctx, R.layout.row_playlist, playLists) {

  override def remove(pl: PlayList) {
    DbUtil.deletePlaylist(ctx, pl.id)
  }

  override def add(pl: PlayList) {
    playLists :+ pl
  }

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    var rowView = convertView
    if (rowView == null) {
      val inflater = ctx.getLayoutInflater()
      rowView = inflater.inflate(R.layout.row_playlist, null)
      rowView.setTag(PLViewHolder(rowView.findViewById(R.id.name_tv).asInstanceOf[TextView]))
    }
    val holder = rowView.getTag().asInstanceOf[PLViewHolder]
    val pl = playLists(position)
    holder.text.setText(pl.name)
    rowView
  }

}

class PLViewHolder(val text: TextView)

object PLViewHolder {
  def apply(text: TextView) = new PLViewHolder(text)
}