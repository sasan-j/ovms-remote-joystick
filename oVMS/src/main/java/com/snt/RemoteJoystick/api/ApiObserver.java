package com.snt.RemoteJoystick.api;

import com.snt.RemoteJoystick.entities.CarData;

public interface ApiObserver {
    void update(CarData pCarData);
    void onServiceAvailable(ApiService pService);
}
