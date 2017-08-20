package de.wico.fatburner.logging;

import android.util.Log;

import java.util.HashMap;
import java.util.Map;

public class AndroidLogger {

    private static final Map<Class, AndroidLogger> LOGGERS = new HashMap<>();

    private final Class clazz;

    private AndroidLogger(Class clazz) {
        this.clazz = clazz;
    }

    public static AndroidLogger get(final Class clazz) {
        if (!LOGGERS.containsKey(clazz)) {
            LOGGERS.put(clazz, new AndroidLogger(clazz));
        }
        return LOGGERS.get(clazz);
    }

    public void debug(String message) {
        Log.d(getTag(), message);
    }

    public void info(String message) {
        Log.i(getTag(), message);
    }

    public void warn(String message) {
        Log.w(getTag(), message);
    }
    // public void error(String message) {
    //     Log.e(getTag(), message);
    // }

    public void error(String message, Throwable e) {
        Log.e(getTag(), message, e);
    }

    private String getTag() {
        return clazz.getName();
    }
}