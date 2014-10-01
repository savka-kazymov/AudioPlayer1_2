package com.savka.audioplayer.entity;

/**
 * Created by naz on 9/23/2014.
 */
public class ObjectDrawerItem {
    private int icon;
    private String name;

    // Constructor.
    public ObjectDrawerItem(int icon, String name) {

        this.icon = icon;
        this.name = name;
    }

    public int getIcon() {
        return icon;
    }

    public void setIcon(int icon) {
        this.icon = icon;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
