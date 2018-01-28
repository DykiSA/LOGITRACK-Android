package com.servertechno.logitrack;

import android.app.Application;

import io.realm.Realm;

/**
 * Created by Acing on 1/28/18 15:18.
 */

public class LogiTrack extends Application {

    @Override
    public void onCreate() {
        super.onCreate();

        Realm.init(this);
    }
}
