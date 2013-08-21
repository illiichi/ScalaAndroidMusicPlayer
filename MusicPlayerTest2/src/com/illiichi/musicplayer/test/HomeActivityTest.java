package com.illiichi.musicplayer.test;

import scala.Option;
import scala.collection.immutable.List;
import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;

import com.illiichi.musicplayer.R;
import com.illiichi.musicplayer.data.DbUtil;
import com.illiichi.musicplayer.data.PlayList;
import com.illiichi.musicplayer.ui.HomeActivity;
import com.illiichi.musicplayer.ui.PlayListArrayAdapter;

public class HomeActivityTest extends
		ActivityInstrumentationTestCase2<HomeActivity> {

	public HomeActivityTest() {
		super(HomeActivity.class);
	}

	private HomeActivity mActivity;
	private ListView mDrawerList;
	private PlayListArrayAdapter mPlAdapter;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		setActivityInitialTouchMode(false);
		mActivity = getActivity();
		mDrawerList = (ListView) mActivity.findViewById(R.id.left_drawer);
		mPlAdapter = (PlayListArrayAdapter) mDrawerList.getAdapter();
	}

	public void testPreConditions() {
		assertTrue(mPlAdapter != null);
		assertTrue(mDrawerList.getOnItemClickListener() != null);
	}

	public void test() {
		assertEquals(mActivity.contentView(), R.layout.activity_base);
		assertTrue(mActivity.getTitle() != null);
	}
	
	public void testDb() {
		List<PlayList> plList1 = DbUtil.getAllPlaylists(mActivity
				.getApplicationContext());
		Option<PlayList> pl = DbUtil.createPlaylist(mActivity, "TEST Playlist 01");
		List<PlayList> plList2 = DbUtil.getAllPlaylists(mActivity
				.getApplicationContext());
		assertEquals(plList1.size()+1, plList2.size());
		DbUtil.deletePlaylist(mActivity, pl.get().id());
		assertEquals(DbUtil.getAllPlaylists(mActivity
				.getApplicationContext()).size(), plList1.size());
	}
}
