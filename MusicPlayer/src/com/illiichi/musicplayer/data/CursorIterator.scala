package com.illiichi.musicplayer.data

import android.database.Cursor

case class CursorIterator(c: Cursor) extends Iterator[Cursor]{
  var started = false

  def hasNext = {
    if (!started) {
      c.moveToFirst()
    }
    else {
      val result = c.moveToNext()
      c.moveToPrevious()
      result
    }
  }

  def next = {
    if (!started) {
      c.moveToFirst()
      started = true
      c
    }
    else {
      c.moveToNext()
      c
    }
  }
}