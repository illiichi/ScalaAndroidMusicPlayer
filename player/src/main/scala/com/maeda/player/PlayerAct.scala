package com.maeda.player

import org.scaloid.common._
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageButton

class PlayerAct extends SActivity {

  onCreate {
    val controller = new LocalServiceConnection[Controller]
    
    setContentView(R.layout.activity_player)
    val btnPlay = find[ImageButton](R.id.btnPlay)
    btnPlay.onClick(
      if (controller.connected) {
        if (controller.service.togglePlayPause) btnPlay.setImageResource(R.drawable.btn_pause)
        else btnPlay.setImageResource(R.drawable.btn_play)
      })
  }
  
  override def onSaveInstanceState(savedInstanceState: Bundle) {
    
  }
}