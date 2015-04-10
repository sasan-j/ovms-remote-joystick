package com.snt.RemoteJoystick.ui;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;

//import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.snt.RemoteJoystick.api.ApiObservable;
import com.snt.RemoteJoystick.api.ApiService;
import com.snt.RemoteJoystick.api.ApiService.ApiBinder;
import android.support.v7.app.ActionBarActivity;

public class ApiActivity extends ActionBarActivity {
	private ApiService mApiService;
	
	@Override
	protected void onStart() {
		super.onStart();
        bindService(new Intent(this, ApiService.class), mConnection, Context.BIND_AUTO_CREATE);
	}
	
	@Override
	protected void onStop() {
		if (mApiService != null) {
        	unbindService(mConnection);
        	mApiService = null;
        }
		super.onStop();
	}
	
	public ApiService getService() {
		return mApiService;
	}
	
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            ApiBinder binder = (ApiBinder) service;
            mApiService = binder.getService();
            ApiObservable.get().notifyOnBind(mApiService);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
        	mApiService = null;
        }
    };
	
}
