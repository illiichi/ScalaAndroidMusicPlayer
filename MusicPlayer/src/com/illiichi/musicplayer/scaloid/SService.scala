package com.illiichi.musicplayer.scaloid

import android.app.Service

trait SService extends Service with SContext with Destroyable with Creatable with Registerable {
  override def basis = this
  override implicit val ctx = this

  def onRegister(body: => Any) = onCreate(body)
  def onUnregister(body: => Any) = onDestroy(body)

  override def onCreate() {
    super.onCreate()
    onCreateBodies.foreach(_ ())
  }

  override def onDestroy() {
    onDestroyBodies.foreach(_ ())
    super.onDestroy()
  }
}