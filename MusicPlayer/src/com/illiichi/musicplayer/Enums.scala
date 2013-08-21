package com.illiichi.musicplayer

object State extends Enumeration {
  type State = Value
  val Retrieving = Value("Retrieving")
  val Stopped = Value("Stopped")
  val Preparing = Value("Preparing")
  val Playing = Value("Playing")
  val Paused = Value("Paused")
}

object PauseReason extends Enumeration {
  type PauseReason = Value
  val UserRequest = Value("UserRequest")
  val FocusLoss = Value("FocusLoss")
}

object AudioFocus extends Enumeration {
  type AudioFocus = Value
  val NoFocusNoDuck = Value("NoFocusNoDuck")
  val NoFocusCanDuck = Value("NoFocusCanDuck")
  val Focused = Value("Focused")
}
