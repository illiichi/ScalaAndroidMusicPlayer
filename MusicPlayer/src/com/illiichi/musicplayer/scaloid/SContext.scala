package com.illiichi.musicplayer.scaloid

import android.content.Intent
import scala.reflect.ClassTag
import android.content.Context

trait TraitContext[V <: android.content.Context] {

  def basis: V

  implicit val ctx = basis

  def startActivity[T: ClassTag] {
    basis.startActivity(SIntent[T])
  }

  def startService[T: ClassTag] {
    basis.startService(SIntent[T])
  }

  def stopService[T: ClassTag] {
    basis.stopService(SIntent[T])
  }

  def applicationContext = basis.getApplicationContext

  def applicationInfo = basis.getApplicationInfo

  def assets = basis.getAssets

  def cacheDir = basis.getCacheDir

  def classLoader = basis.getClassLoader

  def contentResolver = basis.getContentResolver

  def externalCacheDir = basis.getExternalCacheDir

  def filesDir = basis.getFilesDir

  def mainLooper = basis.getMainLooper

  def packageCodePath = basis.getPackageCodePath

  def packageManager = basis.getPackageManager

  def packageName = basis.getPackageName

  def packageResourcePath = basis.getPackageResourcePath

  def resources = basis.getResources

  def theme = basis.getTheme
  def theme(p: Int) = theme_=(p)
  def theme_=(p: Int) = { basis.setTheme(p); basis }

  def wallpaper = basis.getWallpaper
  def wallpaper(p: android.graphics.Bitmap) = wallpaper_=(p)
  def wallpaper_=(p: android.graphics.Bitmap) = { basis.setWallpaper(p); basis }
  def wallpaper(p: java.io.InputStream) = wallpaper_=(p)
  def wallpaper_=(p: java.io.InputStream) = { basis.setWallpaper(p); basis }

  def wallpaperDesiredMinimumHeight = basis.getWallpaperDesiredMinimumHeight

  def wallpaperDesiredMinimumWidth = basis.getWallpaperDesiredMinimumWidth

}

trait SContext extends Context with TraitContext[SContext] with TagUtil {
  def basis: SContext = this
}

object SIntent {
  def apply[T](implicit context: Context, mt: ClassTag[T]) = new Intent(context, mt.runtimeClass)

  def apply[T](action: String)(implicit context: Context, mt: ClassTag[T]): Intent = SIntent[T].setAction(action)
}