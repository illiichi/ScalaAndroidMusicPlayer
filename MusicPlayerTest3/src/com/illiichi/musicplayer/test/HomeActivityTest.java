package com.illiichi.musicplayer.test;

import com.illiichi.musicplayer.R;
import com.illiichi.musicplayer.ui.HomeActivity;
import com.illiichi.musicplayer.ui.PlayListArrayAdapter;

import android.test.ActivityInstrumentationTestCase2;
import android.widget.ListView;

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

	}

	public void test() {
		assertEquals(mActivity.contentView(), R.layout.activity_base);
		assertTrue(mActivity.getTitle() != null);
		// assertTrue(mPlAdapter != null);
		// assertTrue(mDrawerList.getOnItemClickListener() != null);
	}
}
