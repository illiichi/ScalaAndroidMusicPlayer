package com.maeda.player

import android.content.ContentResolver
import android.content.ContentUris
import android.database.Cursor
import android.net.Uri
import android.provider.MediaStore
import org.scaloid.common._

/**
 * Retrieves and organizes media to play. Before being used, you must call {@link #prepare()},
 * which will retrieve all of the music on the user's device (by performing a query on a content
 * resolver). After that, it's ready to retrieve a random song, with its title and URI, upon
 * request.
 */
class MusicRetriever(val resolver: ContentResolver) extends TagUtil with Logger {

  def prepare = {
    val uri = android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI
    info(s"Querying media...URI: ${uri.toString}")
    
  }
}