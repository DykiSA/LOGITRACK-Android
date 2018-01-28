package com.servertechno.logitrack.realm.models;

import java.util.UUID;

import io.realm.RealmObject;
import io.realm.annotations.PrimaryKey;
import io.realm.annotations.RealmClass;
import io.realm.annotations.RealmModule;

/**
 * Created by Acing on 1/27/18 23:51.
 */

public class BrokenStreet extends RealmObject {

    @PrimaryKey
    private String uuid = UUID.randomUUID().toString();
    private double latitude = 0;
    private double longitude = 0;
    private int level = BrokeLevel.LOW.getValue();

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getUuid() {
        return uuid;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getLevel() {
        return level;
    }

    public void setLevel(int level) {
        this.level = level;
    }
}

