package com.jebware.weardemo;

import com.google.android.gms.wearable.DataItem;

import java.util.UUID;

/**
 * Created by jware on 6/11/15.
 * (c) 2015
 */
public class Grocery {

    public int id;
    public String value;
    public DataItem dataItem;

    public Grocery(String value) {
        this.value = value;
        id = UUID.randomUUID().hashCode();
    }

    public Grocery(int id, String value, DataItem dataItem) {
        this.id = id;
        this.value = value;
        this.dataItem = dataItem;
    }

}
