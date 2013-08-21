package com.illiichi.musicplayer

import android.content.{ ContentResolver, ContentUris }
import android.net.Uri
import android.provider.{ MediaStore, BaseColumns }
import android.database.Cursor
import android.provider.MediaStore.MediaColumns
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.MediaStore.Audio
import scala.concurrent._
import ExecutionContext.Implicits.global
import com.illiichi.musicplayer.scaloid.{ TagUtil, Logger }
import com.illiichi.musicplayer.data.{ Song, Album, Artist }

class Retriever(val contentResolver: ContentResolver) extends TagUtil with Logger {

  val songs: Future[List[Song]] = future {
    val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    info(s"Querying media...URI: ${uri.toString}")
    val cur = contentResolver.query(uri, null,
      MediaStore.Audio.AudioColumns.IS_MUSIC + " = 1", null, null)
    info("Query finished.")
    Option(cur).map(queryMedia).get
  }

  def queryMedia(cur: Cursor): List[Song] = {
    val c = List(
      BaseColumns._ID,
      MediaColumns.TITLE,
      MediaColumns.DATA,
      AudioColumns.IS_MUSIC,
      AudioColumns.ALBUM,
      AudioColumns.ALBUM_ID,
      AudioColumns.ALBUM_KEY,
      AudioColumns.ARTIST,
      AudioColumns.ARTIST_ID,
      AudioColumns.ARTIST_KEY,
      AudioColumns.COMPOSER,
      AudioColumns.DURATION)
      .map(s => (s, cur.getColumnIndexOrThrow(s)))
      .toMap

    cur.moveToFirst

    Stream.continually((
      Song(
        cur.getLong(c(BaseColumns._ID)),
        cur.getString(c(MediaColumns.TITLE)),
        cur.getInt(c(AudioColumns.IS_MUSIC)) == 1,
        Album(
          cur.getString(c(AudioColumns.ALBUM)),
          cur.getLong(c(AudioColumns.ALBUM_ID)),
          cur.getString(c(AudioColumns.ALBUM_KEY))),
        Artist(
          cur.getString(c(AudioColumns.ARTIST)),
          cur.getLong(c(AudioColumns.ARTIST_ID)),
          cur.getString(c(AudioColumns.ARTIST_KEY))),
        cur.getString(c(AudioColumns.COMPOSER)),
        cur.getLong(c(AudioColumns.DURATION)),
        cur.getString(c(MediaColumns.DATA))),
        cur.moveToNext))
      .takeWhile(_._2)
      .map(_._1).toList
  }
}
