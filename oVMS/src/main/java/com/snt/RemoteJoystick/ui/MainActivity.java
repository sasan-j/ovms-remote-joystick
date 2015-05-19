package com.snt.RemoteJoystick.ui;

import java.util.UUID;

import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
//import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.support.v7.app.ActionBar;
import android.view.Window;

//import com.actionbarsherlock.app.ActionBar;
//import com.actionbarsherlock.view.Window;
import com.luttu.AppPrefes;
import com.snt.RemoteJoystick.R;
import com.snt.RemoteJoystick.api.ApiObservable;
import com.snt.RemoteJoystick.api.ApiObserver;
import com.snt.RemoteJoystick.api.ApiService;
import com.snt.RemoteJoystick.entities.CarData;
//import com.snt.RemoteJoystick.ui.FragMap.UpdateLocation;
//import com.snt.RemoteJoystick.ui.GetMapDetails.afterasytask;
import com.snt.RemoteJoystick.ui.utils.Database;
//import com.snt.RemoteJoystick.utils.ConnectionList.Con;

public class MainActivity extends ApiActivity implements
        NavigationDrawerFragment.NavigationDrawerCallbacks/*,
		ActionBar.OnNavigationListener */{
	// private static final String TAG = "MainActivity";

	private final Handler mC2dmHandler = new Handler();

//	private ViewPager mViewPager;
//	private MainPagerAdapter mPagerAdapter;

	AppPrefes appPrefes;
	Database database;


    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;



	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		appPrefes = new AppPrefes(this, "ovms");
		database = new Database(this);

		supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
		setSupportProgressBarIndeterminateVisibility(false);

		registerReceiver(mApiEventReceiver, new IntentFilter(getPackageName()
				+ ".ApiEvent"));

        // ------------------------
        // import

        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);
        mTitle = getTitle();

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // --------------------

//		mViewPager = new ViewPager(this);
//		mViewPager.setId(android.R.id.tabhost);
//		setContentView(mViewPager);

//		final ActionBar actionBar = getSupportActionBar();
//		actionBar.setDisplayShowTitleEnabled(false);

//		mPagerAdapter = new MainPagerAdapter(
//                new TabInfo(R.string.Battery,
//				R.drawable.ic_action_battery, InfoFragment.class),
////                new TabInfo(
////				R.string.Car, R.drawable.ic_action_car, CarFragment.class),
////				new TabInfo(R.string.Location,
////						R.drawable.ic_action_location_map, FragMap.class),
////				new TabInfo(R.string.Messages, R.drawable.ic_action_email,
////						NotificationsFragment.class),
//                new TabInfo(
//						R.string.Settings, R.drawable.ic_action_settings,
//						SettingsFragment.class));
//		mViewPager.setAdapter(mPagerAdapter);

		// check for C2DM registration
		// Restore saved registration id
		SharedPreferences settings = getSharedPreferences("C2DM", 0);
		String registrationID = settings.getString("RegID", "");
		if (registrationID.length() == 0) {
			Log.d("C2DM", "Doing first time registration.");

			// No C2DM ID available. Register now.
			// ProgressDialog progress = ProgressDialog.show(this,
			// "Push Notification Network", "Sending one-time registration...");
			Intent intent = new Intent(
					"com.google.android.c2dm.intent.REGISTER");
			intent.putExtra("app",
					PendingIntent.getBroadcast(this, 0, new Intent(), 0)); // boilerplate
			intent.putExtra("sender", "openvehicles@gmail.com");
			startService(intent);
			// progress.dismiss();

			mC2dmHandler.postDelayed(mC2DMRegistrationID, 2000);

			/*
			 * unregister Intent unregIntent = new
			 * Intent("com.google.android.c2dm.intent.UNREGISTER");
			 * unregIntent.putExtra("app", PendingIntent.getBroadcast(this, 0,
			 * new Intent(), 0)); startService(unregIntent);
			 */
		} else {
			Log.d("C2DM", "Loaded Saved C2DM registration ID: "
					+ registrationID);
			mC2dmHandler.postDelayed(mC2DMRegistrationID, 2000);
		}
		onNewIntent(getIntent());
	}

    // import


    @Override
    public void onNavigationDrawerItemSelected(int position) {

        Fragment newFragment = null;

        switch (position+1)
        {
            case 1:
                newFragment = new JoystickFragment();
                break;
            case 2:
                newFragment = new SettingsFragment();
                break;
            default:
                newFragment = new JoystickFragment();
//                newFragment = new InfoFragment();
                break;
        }

        onSectionAttached(position +1);

        // update the main content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        fragmentManager.beginTransaction()
                .replace(R.id.container, newFragment)
                .commit();
    }

    public void onSectionAttached(int number) {
        switch (number) {
            case 1:
                mTitle = getString(R.string.title_section1);
                break;
            case 2:
                mTitle = getString(R.string.title_section2);
                break;
            case 3:
                mTitle = getString(R.string.title_section3);
                break;
        }
    }

    public void restoreActionBar() {
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (!mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
//            getMenuInflater().inflate(R.menu.main, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    //---------


	@Override
	public void onNewIntent(Intent newIntent) {
		super.onNewIntent(newIntent);

//		if (newIntent.getBooleanExtra("onNotification", false)) {
//			onNavigationItemSelected(3, 0);
//		}
	}

	@Override
	protected void onDestroy() {
		unregisterReceiver(mApiEventReceiver);
		super.onDestroy();
	}
//
//	@Override
//	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
//		TabInfo ti = mPagerAdapter.getTabInfoItems()[itemPosition];
//		getSupportActionBar().setIcon(ti.icon_res_id);
//		mViewPager.setCurrentItem(itemPosition, false);
//		return true;
//	}

	private final Runnable mC2DMRegistrationID = new Runnable() {
		public void run() {
			// check if tcp connection is still active (it may be closed as the
			// user leaves the program)
			ApiService service = getService();
			if (service == null || !service.isLoggedIn())
				return;

			SharedPreferences settings = getSharedPreferences("C2DM", 0);
			String registrationID = settings.getString("RegID", "");
			String uuid;

			if (!settings.contains("UUID")) {
				// generate a new UUID
				uuid = UUID.randomUUID().toString();
				Editor editor = getSharedPreferences("C2DM",
						Context.MODE_PRIVATE).edit();
				editor.putString("UUID", uuid);
				editor.commit();

				Log.d("C2DM", "Generated New C2DM UUID: " + uuid);
			} else {
				uuid = settings.getString("UUID", "");
				Log.d("C2DM", "Loaded Saved C2DM UUID: " + uuid);
			}

			// MP-0
			// p<appid>,<pushtype>,<pushkeytype>{,<vehicleid>,<netpass>,<pushkeyvalue>}
			CarData carData = service.getCarData();
			String cmd = String.format("MP-0 p%s,c2dm,production,%s,%s,%s",
					uuid, carData.sel_vehicleid, carData.sel_server_password,
					registrationID);
			service.sendCommand(cmd, null);

			if ((registrationID.length() == 0)
					|| !service.sendCommand(cmd, null)) {
				// command not successful, reschedule reporting after 5 seconds
				Log.d("C2DM", "Reporting C2DM ID failed. Rescheduling.");
				mC2dmHandler.postDelayed(mC2DMRegistrationID, 5000);
			}
		}
	};

	private final BroadcastReceiver mApiEventReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getSerializableExtra("onServerSocketError") != null) {
				setSupportProgressBarIndeterminateVisibility(false);

				new AlertDialog.Builder(MainActivity.this)
						.setTitle(R.string.Error)
						.setMessage(intent.getStringExtra("message"))
						.setPositiveButton(android.R.string.ok, null).show();
			}
			if (intent.getBooleanExtra("onLoginBegin", false)) {
				setSupportProgressBarIndeterminateVisibility(true);
				ApiObservable.get().addObserver(mApiObserver);
			}
		}
	};

	private ApiObserver mApiObserver = new ApiObserver() {
		@Override
		public void update(CarData pCarData) {
			setSupportProgressBarIndeterminateVisibility(false);
			ApiObservable.get().deleteObserver(this);
		}

		@Override
		public void onServiceAvailable(ApiService pService) {
		}
	};































	private class TabInfo {
		public final int title_res_id, icon_res_id;
		public final Class<? extends BaseFragment> fragment_class;

		public String getFragmentName() {
			return fragment_class.getName();
		}

		public TabInfo(int pTitleResId, int pIconResId,
				Class<? extends BaseFragment> pFragmentClass) {
			title_res_id = pTitleResId;
			icon_res_id = pIconResId;
			fragment_class = pFragmentClass;
		}

		@Override
		public String toString() {
			return getString(title_res_id);
		}
	}

	private static class NavAdapter extends ArrayAdapter<TabInfo> {
		public NavAdapter(Context context, TabInfo[] objects) {
			super(context, android.R.layout.simple_spinner_item, objects);
			setDropDownViewResource(R.layout.support_simple_spinner_dropdown_item);
		}

		@Override
		public View getDropDownView(int position, View convertView,
				ViewGroup parent) {
			TextView tv = (TextView) super.getDropDownView(position,
					convertView, parent);
			tv.setCompoundDrawablesWithIntrinsicBounds(
					getItem(position).icon_res_id, 0, 0, 0);
			return tv;
		}
	}

	private class MainPagerAdapter extends FragmentPagerAdapter {
		private final TabInfo[] mTabInfoItems;

		public MainPagerAdapter(TabInfo... pTabInfoItems) {
			super(getSupportFragmentManager());
			mTabInfoItems = pTabInfoItems;
		}

		public TabInfo[] getTabInfoItems() {
			return mTabInfoItems;
		}

		@Override
		public Fragment getItem(int pPosition) {
			return Fragment.instantiate(MainActivity.this,
					mTabInfoItems[pPosition].getFragmentName());
		}

		@Override
		public int getCount() {
			return mTabInfoItems.length;
		}

		@Override
		public CharSequence getPageTitle(int pPosition) {
			return getString(mTabInfoItems[pPosition].title_res_id);
		}
	}

//	@Override
//	public void connections(String al, String name) {
//		// TODO Auto-generated method stub
//
//	}

//	@Override
//	public void after(boolean flBoolean) {
//		// TODO Auto-generated method stub
//
//	}

//	@Override
//	public void updatelocation(String url) {
//		// TODO Auto-generated method stub
//		GetMapDetails getMapDetails = new GetMapDetails(MainActivity.this, url,
//				this);
//		getMapDetails.execute();
//		System.out.println("url" + url);
//	}

}
