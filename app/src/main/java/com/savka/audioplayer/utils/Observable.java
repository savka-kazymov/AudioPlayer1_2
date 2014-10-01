package com.savka.audioplayer.utils;


/**
 * Created by vlad-pc on 17.09.2014.
 */
public interface Observable {
    void registerObserver(Observer o);

    void removeObserver(Observer o);

    void notifyObservers();
}
