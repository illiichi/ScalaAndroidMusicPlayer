package com.illiichi.musicplayer.data

import android.content.{ ContentValues, Context, ContentUris }
import android.provider.{ MediaStore, BaseColumns }
import android.provider.MediaStore.{ Audio, MediaColumns }
import android.provider.MediaStore.Audio.{ Playlists, AudioColumns }
import android.graphics.{ Bitmap, BitmapFactory }
import android.net.Uri
import android.database.Cursor
import android.util.Log
import android.provider.MediaStore.Audio.PlaylistsColumns
import com.illiichi.musicplayer.Constants

object DbUtil {
  val TAG = this.getClass.getSimpleName

  def createPlaylist(ctx: Context, title: String): Option[PlayList] = {
    val PROJECTION_PLAYLIST = Array(BaseColumns._ID)
    val d = System.currentTimeMillis()
    val mInserts = new ContentValues()
    val selection = MediaStore.Audio.PlaylistsColumns.NAME + "=? AND " +
      MediaStore.Audio.PlaylistsColumns.DATE_ADDED + "=?  AND " + MediaStore.Audio.PlaylistsColumns.DATE_MODIFIED + "=?"
    val values = Array[String](title, d.toString, d.toString)
    mInserts.put(PlaylistsColumns.NAME, title)
    mInserts.put(PlaylistsColumns.DATE_ADDED, d.toString)
    mInserts.put(PlaylistsColumns.DATE_MODIFIED, d.toString)
    val resolver = ctx.getContentResolver()
    val id = for {
      mUri <- Option(resolver.insert(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, mInserts))
      cursor <- Option(resolver.query(mUri, PROJECTION_PLAYLIST, null, null, null))
      id <- Option({
        cursor.moveToFirst()
        val id = cursor.getLong(cursor.getColumnIndex(MediaStore.Audio.Playlists.Members._ID))
        cursor.close()
        id
      })
    } yield id
    id.map(x => PlayList(x, title, d, d))
  }

  def getAllPlaylists(ctx: Context): List[PlayList] = {
    val list: List[PlayList] = List.empty
    val cursor = ctx.getContentResolver().query(
      MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI,
      Array(BaseColumns._ID, MediaStore.Audio.PlaylistsColumns.NAME,
        MediaStore.Audio.PlaylistsColumns.DATE_ADDED, PlaylistsColumns.DATE_MODIFIED),
      null,
      null,
      MediaStore.Audio.Playlists.DEFAULT_SORT_ORDER)
    if (cursor == null) List.empty
    else {
      val l = (new CursorIterator(cursor)).map({ c =>
        val id = c.getInt(c.getColumnIndex(BaseColumns._ID))
        val name = c.getString(c.getColumnIndex(PlaylistsColumns.NAME))
        val dateAdded = c.getLong(c.getColumnIndex(PlaylistsColumns.DATE_ADDED))
        val dateModified = c.getLong(c.getColumnIndex(PlaylistsColumns.DATE_MODIFIED))
        new PlayList(id, name, dateAdded, dateModified)
      }).toList
      if (cursor != null) cursor.close()
      l
    }
  }

  def deletePlaylist(ctx: Context, id: Long): Unit = {
    val where = BaseColumns._ID + "=?"
    val whereVal = Array[String](id.toString)
    ctx.getContentResolver().delete(MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, where, whereVal)
  }

  def addSongToPlaylist(ctx: Context, id: String, plId: Long): Int = {
    val uri = MediaStore.Audio.Playlists.Members.getContentUri(Constants.EXTERNAL, plId)
    val resolver = ctx.getContentResolver()
    val orderCursor = resolver.query(uri,
      Array[String](MediaStore.Audio.Playlists.Members.PLAY_ORDER), null, null,
      MediaStore.Audio.Playlists.Members.PLAY_ORDER + " DESC ")
    val playOrder = if (orderCursor != null && orderCursor.moveToFirst()) {
      val order = orderCursor.getInt(0) + 1
      orderCursor.close()
      order
    } else 0
    val value = new ContentValues()
    value.put(MediaStore.Audio.Playlists.Members.AUDIO_ID, id)
    value.put(MediaStore.Audio.Playlists.Members.PLAY_ORDER, playOrder: java.lang.Integer)
    val u = resolver.insert(uri, value)
    val cols = Array[String]("count(*)")
    val cur = resolver.query(uri, cols, null, null, null)
    cur.moveToFirst()
    val count = cur.getInt(0)
    cur.close()
    count
  }

  def removeSongFromPlaylist(ctx: Context, songId: Long, plId: Long): Unit = {
    try {
      val uri = MediaStore.Audio.Playlists.Members.getContentUri(Constants.EXTERNAL, plId)
      val selection = MediaStore.Audio.Playlists.Members.AUDIO_ID + "=?"
      val args: Array[String] = Array[String](String.valueOf(songId))
      val num = ctx.getContentResolver().delete(uri, selection, args)
    } catch {
      case e: Exception => Log.e(TAG, "DbUtil#removeSongFromPlaylist")
    }
  }

  def getSongForUriStrAndTitle(ctx: Context, uriStr: String, title: String): Option[Song] = {
    val selection = AudioColumns.IS_MUSIC + "=1 AND " +
      MediaStore.MediaColumns.TITLE + "=?  AND " + MediaStore.MediaColumns.DATA + "=?"
    val values = Array[String](title, uriStr)
    val cursor = ctx.getContentResolver().query(
      MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
      null, selection.toString(), values, null)
    if (cursor == null) None
    else {
      val c = List(
        BaseColumns._ID,
        MediaColumns.TITLE, MediaColumns.DATA, AudioColumns.IS_MUSIC,
        AudioColumns.ALBUM, AudioColumns.ALBUM_ID, AudioColumns.ALBUM_KEY,
        AudioColumns.ARTIST, AudioColumns.ARTIST_ID, AudioColumns.ARTIST_KEY,
        AudioColumns.COMPOSER, AudioColumns.DURATION)
        .map(s => (s, cursor.getColumnIndexOrThrow(s)))
        .toMap
      cursor.moveToFirst
      val s = Song(
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
      cursor.close()
      Option(s)
    }
  }

  def getSongListForPlayList(ctx: Context, playlistID: Long): Option[List[Song]] = {
    val selection = AudioColumns.IS_MUSIC + "=1 AND " + MediaStore.MediaColumns.TITLE + " != ''"
    val cursor = ctx.getContentResolver().query(
      MediaStore.Audio.Playlists.Members.getContentUri(Constants.EXTERNAL, playlistID),
      null, selection.toString(), null, MediaStore.Audio.Playlists.Members.DEFAULT_SORT_ORDER)
    if (cursor == null) None
    else {
      val l = Option(Song.cursorToList(cursor))
      l
    }
  }

  def query(ctx: Context, uri: Uri, projection: Array[String], selection: String,
    selectionArgs: Array[String], sortOrder: String, limit: Int): Option[Cursor] = {
    try {
      val resolver = ctx.getContentResolver
      if (limit > 0) {
        uri.buildUpon().appendQueryParameter("limit", "" + limit).build()
      }
      Option(resolver.query(uri, projection, selection, selectionArgs, sortOrder))
    } catch {
      case e: Exception => Log.e("DbUtil", e.toString()); None
    }
  }

  def getCover(ctx: Context, albumId: Long): Option[Bitmap] = {
    try {
      val uri = ContentUris.withAppendedId(
        Uri.parse("content://media/external/audio/albumart"), albumId)
      val in = ctx.getContentResolver.openInputStream(uri)
      val bitmap = Option(BitmapFactory.decodeStream(in))
      in.close()
      bitmap
    } catch {
      case e: java.io.FileNotFoundException => {
        Log.w("Music", s"Exception when reading cover image: ${e.getMessage}")
        None
      }
      case e: java.lang.SecurityException => {
        Log.w("Music", s"Exception when reading cover image: ${e.getMessage}")
        None
      }
    }
  }

}