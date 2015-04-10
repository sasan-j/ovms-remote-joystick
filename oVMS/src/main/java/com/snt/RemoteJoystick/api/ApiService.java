package com.snt.RemoteJoystick.api;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.snt.RemoteJoystick.R;
import com.snt.RemoteJoystick.api.ApiTask.OnUpdateStatusListener;
import com.snt.RemoteJoystick.entities.CarData;
import com.snt.RemoteJoystick.utils.CarsStorage;

public class ApiService extends Service implements OnUpdateStatusListener {
	private static final String TAG = "MainActivity";
    private final IBinder mBinder = new ApiBinder();
	private volatile CarData mCarData;
    private ApiTask mApiTask;
	private OnResultCommandListenner mOnResultCommandListenner;
	
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
	
	@Override
	public void onCreate() {
		super.onCreate();
		changeCar(CarsStorage.get().getSelectedCarData());
	}
	
	@Override
	public void onDestroy() {
		try {
			if (mApiTask != null) {
				Log.v(TAG, "Shutting down TCP connection");
				mApiTask.connClose();
				mApiTask.cancel(true);
				mApiTask = null;
			}
		} catch (Exception e) {
			Log.e(TAG, "ERROR stop ApiTask", e);
		}
		super.onDestroy();
	}
	
	public void changeCar(CarData pCarData) {
		Log.d(TAG, "Changed car to: " + pCarData.sel_vehicleid);
		mCarData = pCarData;

		// kill previous connection
		if (mApiTask != null) {
			Log.v("TCP", "Shutting down previous TCP connection (ChangeCar())");
			mApiTask.connClose();
			mApiTask.cancel(true);
		}

		// start new connection
		// reset the paranoid mode flag in car data
		// it will be set again when the TCP task detects paranoid mode messages
		mCarData.sel_paranoid = false;
		mApiTask = new ApiTask(mCarData, this);
		
		Log.v(TAG, "Starting TCP Connection (changeCar())");
		mApiTask.execute();
	}
	
	public void sendCommand(int pResIdMessage, String pCommand, OnResultCommandListenner pOnResultCommandListenner) {
		sendCommand(getString(pResIdMessage), pCommand, pOnResultCommandListenner);
	}

	public void sendCommand(String pMessage, String pCommand, OnResultCommandListenner pOnResultCommandListenner) {
		if (mApiTask == null) return;

		mOnResultCommandListenner = pOnResultCommandListenner;
		mApiTask.sendCommand(String.format("MP-0 C%s", pCommand));
//		Toast.makeText(this, pMessage, Toast.LENGTH_SHORT).show();
	}
	
	public boolean sendCommand(String pCommand, OnResultCommandListenner pOnResultCommandListenner) {
		if (mApiTask == null || TextUtils.isEmpty(pCommand)) return false;
		
		mOnResultCommandListenner = pOnResultCommandListenner;
		return mApiTask.sendCommand(pCommand.startsWith("MP-0") ? pCommand : String.format("MP-0 C%s", pCommand));
	}
	
	public void cancelCommand() {
		mOnResultCommandListenner = null;
	}

	@Override
	public void onUpdateStatus() {
		ApiObservable.get().notifyUpdate(mCarData);		
	}

	@Override
	public void onServerSocketError(Throwable e) {
		Intent intent = new Intent(getPackageName() + ".ApiEvent");
		intent.putExtra("onServerSocketError", e);
		intent.putExtra("message", getString(mApiTask.isLoggedIn() ? 
				R.string.err_connection_lost : R.string.err_check_following));
		sendBroadcast(intent);
	}

	@Override
	public void onResultCommand(String pCmd) {
		if (TextUtils.isEmpty(pCmd)) return;
		String[] data = pCmd.split(",\\s*");

		
		if (mOnResultCommandListenner != null) {
//            Toast.makeText(this, mOnResultCommandListenner.toString(), Toast.LENGTH_SHORT).show();
			mOnResultCommandListenner.onResultCommand(data);
			return;
		}
	}

	@Override
	public void onLoginBegin() {
		Log.d(TAG, "onLoginBegin");
		
		Intent intent = new Intent(getPackageName() + ".ApiEvent");
		intent.putExtra("onLoginBegin", true);
		sendBroadcast(intent);
	}

	@Override
	public void onLoginComplete() {
		Log.d(TAG, "onLoginComplete");
		
//		Intent intent = new Intent(getPackageName() + ".ApiEvent");
//		intent.putExtra("onLoginComplete", true);
//		sendBroadcast(intent);
	}
	
	public boolean isLoggined() {
		return mApiTask.isLoggedIn();
	}
	
	public CarData getCarData() {
		return mCarData;
	}
	
	public CarData getLogginedCarData() {
		return mApiTask.isLoggedIn() ? mCarData : null;
	}
	
    public class ApiBinder extends Binder {
    	
    	public ApiService getService() {
            return ApiService.this;
        }
    	
    }

}
