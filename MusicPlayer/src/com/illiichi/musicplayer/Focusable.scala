package com.illiichi.musicplayer

/**
 * Represents something that can react to audio focus events. 
 */
trait Focusable {

  /** Signals that audio focus was gained. */
  def onGainedAudioFocus
  /**
   * Signals that audio focus was lost.
   *
   * @param canDuck If true, audio can continue in "ducked" mode (low volume). Otherwise, all
   * audio must stop.
   */
  def onLostAudioFocus(canDuck: Boolean)
}