package com.illiichi.musicplayer.scaloid

import android.app.Activity
import android.view.View
import scala.collection.mutable.ArrayBuffer
import android.os.Bundle

trait TraitActivity[V <: Activity] {
  def contentView_=(p: View) = {
    basis.setContentView(p)
    basis
  }

  def contentView(p: View) = contentView_=(p)
  def basis: Activity

  def find[V <: View](id: Int): V = basis.findViewById(id).asInstanceOf[V]
}

trait SActivity extends Activity with SContext with TraitActivity[SActivity] with Destroyable with Creatable with Registerable {

  override def basis = this
  override implicit val ctx = this

  def onRegister(body: => Any) = onResume(body)
  def onUnregister(body: => Any) = onPause(body)

  val onStartStop = new Registerable {
    def onRegister(body: => Any) = onStart(body)
    def onUnregister(body: => Any) = onStop(body)
  }

  val onCreateDestroy = new Registerable {
    def onRegister(body: => Any) = onCreate(body)
    def onUnregister(body: => Any) = onDestroy(body)
  }

  protected override def onCreate(b: Bundle) {
    super.onCreate(b)
    onCreateBodies.foreach(_())
  }

  override def onStart {
    super.onStart()
    onStartBodies.foreach(_())
  }

  protected val onStartBodies = new ArrayBuffer[() => Any]

  def onStart(body: => Any) = {
    val el = (() => body)
    onStartBodies += el
    el
  }

  override def onResume {
    super.onResume()
    onResumeBodies.foreach(_())
  }

  protected val onResumeBodies = new ArrayBuffer[() => Any]

  def onResume(body: => Any) = {
    val el = (() => body)
    onResumeBodies += el
    el
  }

  override def onPause {
    onPauseBodies.foreach(_())
    super.onPause()
  }

  protected val onPauseBodies = new ArrayBuffer[() => Any]

  def onPause(body: => Any) = {
    val el = (() => body)
    onPauseBodies += el
    el
  }

  override def onStop {
    onStopBodies.foreach(_())
    super.onStop()
  }

  protected val onStopBodies = new ArrayBuffer[() => Any]

  def onStop(body: => Any) = {
    val el = (() => body)
    onStopBodies += el
    el
  }

  override def onDestroy {
    onDestroyBodies.foreach(_())
    super.onDestroy()
  }
}