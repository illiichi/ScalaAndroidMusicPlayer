package com.illiichi.musicplayer

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.widget.Button
import org.junit.Before
import org.junit.Test
import org.junit.Assert.assertThat
import org.hamcrest.CoreMatchers.equalTo
import com.illiichi.musicplayer.ui.HomeActivity
import org.robolectric.shadows.ShadowHandler
import org.robolectric.shadows.ShadowToast

@RunWith(classOf[RobolectricTestRunner])
class HomeActivityTest2 {

  lazy val activity: HomeActivity = new HomeActivity()
  //  def button = activity.findViewById(R.id.action_add_songs).asInstanceOf[Button]

  @Before def setup {
    //http://stackoverflow.com/questions/17687173/in-robolectric-how-do-i-get-around-drawerlayout-must-be-measured-with-measuresp
//        activity.onCreate(null)
  }

  @Test def test {
    val name = activity.getResources().getString(R.string.app_name)
    assertThat(name, equalTo("SPlayer"))
    
    
    
    //    button.performClick
    //    ShadowHandler.idleMainLooper() 
    //    println(ShadowToast.getTextOfLatestToast())
    //    assertThat( ShadowToast.getTextOfLatestToast(), equalTo("Can't add songs to the 'All' playlist.") )
  }
}