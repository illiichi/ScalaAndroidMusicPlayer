package com.illiichi.musicplayer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import com.illiichi.musicplayer.ui.HomeActivity;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith(RobolectricTestRunner.class)
public class HomeActivityTest {
	@Test
    public void shouldHaveHappySmiles() throws Exception {
        String hello = new HomeActivity().getResources().getString(R.string.app_name);
        assertThat(hello, equalTo("SPlayer"));
    }
}
