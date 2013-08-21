package com.illiichi.musicplayer

import android.graphics.drawable.Drawable
import android.content.Context
import android.content.res.Resources
import android.util.Log

object UiUtil {

  val packageName = "com.illiichi.musicplayer"
  val TAG = "UiUtil"
  /**
   * Used to return a drawable from the theme resources.
   *
   * @param resourceName The name of the drawable to return. i.e.
   *            "pager_background".
   * @return A new color from the theme resources.
   */

  def getDrawable(ctx: Context, resourceName: String): Option[Drawable] = {
    val mPackageManager = ctx.getPackageManager()
    val mResources = mPackageManager.getResourcesForApplication(packageName)
    val resourceId = mResources.getIdentifier(resourceName, "drawable", packageName)
    try {
      Option(mResources.getDrawable(resourceId))
    } catch {
      case e: Resources.NotFoundException => Log.e(TAG, "we're screwed!")
      None
    }
    
  }
}