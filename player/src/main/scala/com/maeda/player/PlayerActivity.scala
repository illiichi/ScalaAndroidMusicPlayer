package com.maeda.player

import org.scaloid.common._
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton
import android.media.AudioManager

class PlayerActivity extends SActivity {

  onCreate {
    val controller = new LocalServiceConnection[Controller]
    
    setContentView(R.layout.activity_player)
    setVolumeControlStream(AudioManager.STREAM_MUSIC)
    val btnPlayPause = find[ImageButton](R.id.btn_play)
    btnPlayPause.onClick(
      if (controller.connected) {
        if (controller.service.togglePlayPause) 
          btnPlayPause.setImageResource(R.drawable.btn_pause)
        else btnPlayPause.setImageResource(R.drawable.btn_play)
      })
    
    val btnNext = find[ImageButton](R.id.btn_next)
  }
  
  
}