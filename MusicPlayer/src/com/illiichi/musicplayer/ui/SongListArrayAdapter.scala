package com.illiichi.musicplayer.ui

import scala.concurrent._
import scala.util.{ Success, Failure }
import ExecutionContext.Implicits.global
import android.app.Activity
import android.util.Log
import android.view.{ View, ViewGroup }
import android.widget.{ ArrayAdapter, ImageView, TextView }
import android.graphics.Bitmap
import android.os.{ Handler, Looper }
import android.support.v4.util.LruCache
import com.illiichi.musicplayer.R
import com.illiichi.musicplayer.data.{ Song, DbUtil }

class SongListArrayAdapter(ctx: Activity, songs: Array[Song]) extends ArrayAdapter[Song](ctx, R.layout.row_song, songs) {
  val TAG = this.getClass().getSimpleName()

  val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).asInstanceOf[Int]
  val cacheSize = maxMemory / 8
  private var mMemoryCache: LruCache[String, Bitmap] = new LruCache[String, Bitmap](cacheSize) {
    override def sizeOf(key: String, bitmap: Bitmap): Int = {
      bitmap.getByteCount() / 1024
    }
  }

  override def getView(position: Int, convertView: View, parent: ViewGroup): View = {
    var rowView = convertView
    if (rowView == null) {
      val inflater = ctx.getLayoutInflater()
      rowView = inflater.inflate(R.layout.row_song, null)
      rowView.setTag(
        SongsViewHolder(rowView.findViewById(R.id.row_song_icon).asInstanceOf[ImageView],
          rowView.findViewById(R.id.row_song_title).asInstanceOf[TextView]))
    }
    val holder = rowView.getTag().asInstanceOf[SongsViewHolder]
    val s = songs(position)
    holder.text.setText(s.title.asInstanceOf[CharSequence])
    //    holder.image.setImageBitmap(pic)
    loadBitmap(s, holder.image)
    rowView
  }

  def addBitmapToMemoryCache(key: String, bitmap: Bitmap): Option[Bitmap] =
    getBitmapFromMemCache(key).map(b => mMemoryCache.put(key, b))

  def getBitmapFromMemCache(key: String): Option[Bitmap] =
    Option(mMemoryCache.get(key))

  def loadBitmap(s: Song, imageView: ImageView) {
    val imageKey = s.album.id.toString
    val bitmap = getBitmapFromMemCache(imageKey)
    bitmap match {
      case Some(b) => imageView.setImageBitmap(b)
      case None => queryBitmap(s, imageKey, imageView)
    }
  }

  def queryBitmap(s: Song, imageKey: String, imageView: ImageView) = {
    val f: Future[Option[Bitmap]] =
      future {
        s.album.getCover(ctx, 4)
      }
    f onComplete {
      case Success(b) => b.map(pic => {
        addBitmapToMemoryCache(imageKey, pic)
        ctx.runOnUiThread(new Runnable() {
          def run() = imageView.setImageBitmap(pic)
        })
      })
      case Failure(t) => Log.e(TAG, "Error retrieving a cover image")
    }
  }

}

class SongsViewHolder(val image: ImageView, val text: TextView)

object SongsViewHolder {
  def apply(image: ImageView, text: TextView) = new SongsViewHolder(image, text)
}