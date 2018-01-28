package com.servertechno.logitrack.realm.models;

public enum BrokeLevel {
    LOW(15),
    HIGH(50);

    private int value;

    BrokeLevel(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }
}
