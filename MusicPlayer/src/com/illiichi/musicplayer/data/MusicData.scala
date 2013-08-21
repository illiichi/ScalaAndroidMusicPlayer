package com.illiichi.musicplayer.data

import android.net.Uri
import android.database.Cursor
import android.util.Log
import android.graphics.{ BitmapFactory, Bitmap }
import android.content.{ ContentUris, Context }
import android.provider.MediaStore.{ MediaColumns, Audio }
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.{ MediaStore, BaseColumns }
import scala.concurrent._
import ExecutionContext.Implicits.global

class MusicData(ctx: Context, uri: Uri) {
  
  val songs: Future[List[Song]] =
    future {
      val cursor = ctx.getContentResolver.query(uri,
        null, MediaStore.Audio.AudioColumns.IS_MUSIC + "=1",
        null, null)
      Song.cursorToList(cursor) filter (_.is_music)
    }
}

case class Album(title: String, id: Long, key: String) {
  def getCover(context: Context, sampleSize: Int): Option[Bitmap] = {
    try {
      val opt = new BitmapFactory.Options()
      opt.inSampleSize = sampleSize
      val uri = ContentUris.withAppendedId(
        Uri.parse("content://media/external/audio/albumart"), id)
      val in = Option(context.getContentResolver.openInputStream(uri))
      in.flatMap(x => {
        val r = Option(BitmapFactory.decodeStream(x, null, opt))
        in.map(_.close())
        r
      })
    } catch {
      case e: java.io.FileNotFoundException => {
        //        error(s"Album#getCover ${e.getMessage}")
        None
      }
      case e: java.lang.SecurityException => {
        //        error(s"Album#getCover ${e.getMessage}")
        None
      }
    }
  }
}

case class PlayList(val id: Long, val name: String, val dateAdded: Long, val dateModified: Long)

case class Artist(name: String, id: Long, key: String)

case class Song(
  id: Long,
  title: String,
  is_music: Boolean,
  album: Album,
  artist: Artist,
  composer: String,
  duration: Long,
  uriString: String) {
  lazy val uri = {
    val b = new Uri.Builder
    b.appendPath(uriString)
    b.build
  }
}

object Song {
  def cursorToList(cursor: Cursor): List[Song] = {
    val c = List(
      BaseColumns._ID, MediaColumns.TITLE, MediaColumns.DATA,
      AudioColumns.IS_MUSIC, AudioColumns.ALBUM, AudioColumns.ALBUM_ID,
      AudioColumns.ALBUM_KEY, AudioColumns.ARTIST, AudioColumns.ARTIST_ID,
      AudioColumns.ARTIST_KEY, AudioColumns.COMPOSER, AudioColumns.DURATION)
      .map(s => (s, cursor.getColumnIndexOrThrow(s)))
      .toMap
    def loadSong = Song(
      cursor.getLong(c(BaseColumns._ID)),
      cursor.getString(c(MediaColumns.TITLE)),
      cursor.getInt(c(AudioColumns.IS_MUSIC)) == 1,
      Album(
        cursor.getString(c(AudioColumns.ALBUM)),
        cursor.getLong(c(AudioColumns.ALBUM_ID)),
        cursor.getString(c(AudioColumns.ALBUM_KEY))),
      Artist(
        cursor.getString(c(AudioColumns.ARTIST)),
        cursor.getLong(c(AudioColumns.ARTIST_ID)),
        cursor.getString(c(AudioColumns.ARTIST_KEY))),
      cursor.getString(c(AudioColumns.COMPOSER)),
      cursor.getLong(c(AudioColumns.DURATION)),
      cursor.getString(c(MediaColumns.DATA)))
    if (cursor.moveToFirst) {
      val songs = Iterator(Some(loadSong)) ++
        Iterator.continually(if (cursor.moveToNext()) Some(loadSong) else None)
        val s = songs.takeWhile(x => !x.isEmpty)
        s.flatten.toList
    } else List.empty
  }
}