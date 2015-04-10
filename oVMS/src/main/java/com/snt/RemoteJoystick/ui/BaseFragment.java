package com.snt.RemoteJoystick.ui;

import android.app.Activity;
import android.support.v4.app.Fragment;
import android.view.View;

//import com.actionbarsherlock.app.SherlockFragment;
import com.snt.RemoteJoystick.api.ApiObservable;
import com.snt.RemoteJoystick.api.ApiObserver;
import com.snt.RemoteJoystick.api.ApiService;
import com.snt.RemoteJoystick.api.OnResultCommandListenner;
import com.snt.RemoteJoystick.entities.CarData;

public class BaseFragment extends Fragment implements ApiObserver {

	@Override
	public void onStart() {
		super.onStart();
		ApiObservable.get().addObserver(this);
		ApiService service = getService();
		if (service != null) {
			onServiceAvailable(service);
			if (service.isLoggined())
				update(service.getCarData());
		}
	}

	@Override
	public void onStop() {
		super.onStop();
		ApiObservable.get().deleteObserver(this);
	}

	@Override
	public void update(CarData pCarData) {
	}

	@Override
	public void onServiceAvailable(ApiService pService) {
	}

	public void cancelCommand() {
		ApiService service = getService();
		if (service == null)
			return;
		service.cancelCommand();
	}
	public View findViewById(int pResId) {
		return getView().findViewById(pResId);
	}

	public void sendCommand(int pResIdMessage, String pCommand,
			OnResultCommandListenner pOnResultCommandListenner) {
		ApiService service = getService();
		if (service == null)
			return;
		service.sendCommand(pResIdMessage, pCommand, pOnResultCommandListenner);
	}

	public void sendCommand(String pMessage, String pCommand,
			OnResultCommandListenner pOnResultCommandListenner) {
		ApiService service = getService();
		if (service == null)
			return;
		service.sendCommand(pMessage, pCommand, pOnResultCommandListenner);
	}

	public void sendCommand(String pCommand,
			OnResultCommandListenner pOnResultCommandListenner) {
		ApiService service = getService();
		if (service == null)
			return;
		service.sendCommand(pCommand, pOnResultCommandListenner);
	}

	public void changeCar(CarData pCarData) {
		ApiService service = getService();
		if (service == null)
			return;
		service.changeCar(pCarData);
	}

	public ApiService getService() {
		Activity activity = getActivity();
		if (activity instanceof ApiActivity) {
			return ((ApiActivity) activity).getService();
		}
		return null;
	}

}
