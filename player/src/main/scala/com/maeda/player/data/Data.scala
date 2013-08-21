package com.maeda.player.data

import android.net.Uri
import android.database.Cursor
import scala.concurrent._
import android.content.Context
import ExecutionContext.Implicits.global
import android.util.Log
import android.provider.MediaStore.MediaColumns
import android.graphics.BitmapFactory
import android.content.ContentUris
import android.graphics.Bitmap
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.MediaStore

class Music(ctx: Context, uri: Uri) {
  val songs: Future[List[Song]] =
    future {
      //      val cursor = ctx.getContentResolver.query(uri, null, null, null, null)
      val cursor = ctx.getContentResolver.query(uri,
        null, MediaStore.Audio.AudioColumns.IS_MUSIC + "=1",
        null, null);
      Song.cursorToStream(cursor).toList filter (_.is_music)
    }
}

case class Album(title: String, id: Long, key: String) {
  def getCover(context: Context): Option[Bitmap] = {
    try {
      val uri = ContentUris.withAppendedId(
        Uri.parse("content://media/external/audio/albumart"), id)
      val in = context.getContentResolver.openInputStream(uri)
      Option(BitmapFactory.decodeStream(in))
    } catch {
      case e: java.io.FileNotFoundException => {
        error(s"Album#getCover ${e.getMessage}")
        None
      }
      case e: java.lang.SecurityException => {
        error(s"Album#getCover ${e.getMessage}")
        None
      }
    }
  }
}

case class Artist(name: String, id: Long, key: String)

case class Song(
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

  def cursorToStream(cursor: Cursor): Stream[Song] = {
    val c = List(
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
      .map(s => (s, cursor.getColumnIndexOrThrow(s)))
      .toMap

    cursor.moveToFirst

    Stream.continually((
      Song(
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
        cursor.getString(c(MediaColumns.DATA))),
        cursor.moveToNext))
      .takeWhile(_._2)
      .map(_._1)
  }
}