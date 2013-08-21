package com.illiichi.musicplayer.scaloid

import android.util.Log

case class LoggerTag(_tag: String) {
  private val MAX_TAG_LEN = 22
  val tag = if (_tag.length < MAX_TAG_LEN) _tag else ":" + _tag.substring(_tag.length - (MAX_TAG_LEN - 1), _tag.length)
}

trait TagUtil {
  implicit val loggerTag = LoggerTag(this.getClass.getName)
}

trait Logger {
  private def loggingText(str: String, t: Throwable) = str + (if (t == null) "" else "\n" + Log.getStackTraceString(t))

  def verbose(str: => String, t: Throwable = null)(implicit tag: LoggerTag) { if (Log.isLoggable(tag.tag, Log.VERBOSE)) Log.v(tag.tag, loggingText(str, t)) }
  def debug(str: => String, t: Throwable = null)(implicit tag: LoggerTag) { if (Log.isLoggable(tag.tag, Log.DEBUG)) Log.d(tag.tag, loggingText(str, t)) }
  def info(str: => String, t: Throwable = null)(implicit tag: LoggerTag) { if (Log.isLoggable(tag.tag, Log.INFO)) Log.i(tag.tag, loggingText(str, t)) }
  def warn(str: => String, t: Throwable = null)(implicit tag: LoggerTag) { if (Log.isLoggable(tag.tag, Log.WARN)) Log.w(tag.tag, loggingText(str, t)) }
  def error(str: => String, t: Throwable = null)(implicit tag: LoggerTag) { if (Log.isLoggable(tag.tag, Log.ERROR)) Log.e(tag.tag, loggingText(str, t)) }
  def wtf(str: => String, t: Throwable = null)(implicit tag: LoggerTag) { if (Log.isLoggable(tag.tag, Log.ASSERT)) Log.wtf(tag.tag, loggingText(str, t)) }
}