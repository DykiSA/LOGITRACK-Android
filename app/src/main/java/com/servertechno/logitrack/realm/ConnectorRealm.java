package com.servertechno.logitrack.realm;

import android.content.Context;
import android.util.Log;

import com.servertechno.logitrack.realm.models.BrokeLevel;
import com.servertechno.logitrack.realm.models.BrokenStreet;

import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmConfiguration;
import io.realm.RealmObject;
import io.realm.RealmResults;
import io.realm.annotations.RealmModule;

import static android.content.ContentValues.TAG;

/**
 * Created by Acing on 1/28/18 00:11.
 */

public class ConnectorRealm {

    Realm realm;

    public ConnectorRealm() {
        this.realmInit();
    }

    private void realmInit() {
        RealmConfiguration config = new RealmConfiguration.Builder()
                .name("default.realm")
                .schemaVersion(1)
                .build();
        this.realm = Realm.getInstance(config);
        Log.d(TAG, "realmPath: " + this.realm.getPath());
    }

    public RealmResults<BrokenStreet> getBrokenStreets() {
        return this.realm.where(BrokenStreet.class).findAll();
    }

    public BrokenStreet getBrokenStreetByUuid(String uuid) {
        return this.realm.where(BrokenStreet.class).equalTo("uuid", uuid).findFirst();
    }

    public BrokenStreet createBrokenStreet(double latitude, double longitude, BrokeLevel level) {
        this.realm.beginTransaction();
        BrokenStreet record = this.realm.createObject(BrokenStreet.class);
        record.setUuid(UUID.randomUUID().toString());
        record.setLatitude(latitude);
        record.setLongitude(longitude);
        record.setLevel(level.getValue());
        this.realm.commitTransaction();
        return record;
    }

    public void removeBrokenSteet(BrokenStreet street) {
        street.deleteFromRealm();
    }
}