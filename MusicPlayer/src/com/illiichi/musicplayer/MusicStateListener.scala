package com.illiichi.musicplayer

trait MusicStateListener {
	def onSongChanged(pos:Int):Unit
}