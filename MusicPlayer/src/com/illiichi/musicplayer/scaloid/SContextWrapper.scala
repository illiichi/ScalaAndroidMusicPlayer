package com.illiichi.musicplayer.scaloid

trait TraitContextWrapper[V <: android.content.ContextWrapper] extends TraitContext[V] {
  @inline def baseContext = basis.getBaseContext
}

class SContextWrapper()(implicit base: android.content.Context)
  extends android.content.ContextWrapper(base) with TraitContextWrapper[SContextWrapper] {
  def basis = this
}

object SContextWrapper {
  def apply()(implicit base: android.content.Context): SContextWrapper = {
    val v = new SContextWrapper
    v
  }
}
