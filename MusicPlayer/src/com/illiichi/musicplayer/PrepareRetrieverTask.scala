package com.illiichi.musicplayer

import scala.concurrent._
import scala.util.{ Success, Failure }
import ExecutionContext.Implicits.global
import android.util.Log
import android.app.ActivityManager
import android.app.ActivityManager.RunningServiceInfo
import android.content.Context
import com.illiichi.musicplayer.data.Song

class PrepareRetrieverTask(retriever: Retriever, listener: RetrieverCompletedListener) {
  retriever.songs onComplete {
    case Success(s) => listener.onCompleted(Option(s))
    case Failure(t) => listener.onCompleted(None)
  }

}

trait RetrieverCompletedListener {
  def onCompleted(items: Option[List[Song]])
}