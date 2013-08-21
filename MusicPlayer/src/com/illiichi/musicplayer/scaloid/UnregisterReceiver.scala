package com.illiichi.musicplayer.scaloid

import android.content.BroadcastReceiver
import android.content.ContextWrapper
import android.content.IntentFilter

trait UnregisterReceiver extends ContextWrapper with Destroyable {
  /**
    * Internal implementation for (un)registering the receiver. You do not need to call this method.
    */
  override def registerReceiver(receiver: BroadcastReceiver, filter: IntentFilter): android.content.Intent = {
    onDestroy {
      try {
        unregisterReceiver(receiver)
      } catch {
        // Suppress "Receiver not registered" exception
        // Refer to http://stackoverflow.com/questions/2682043/how-to-check-if-receiver-is-registered-in-android
        case e: IllegalArgumentException => e.printStackTrace()
      }
    }
    super.registerReceiver(receiver, filter)
  }
}