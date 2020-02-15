package com.example.juan.tanit.selfadaptation;

import android.os.Build;
import android.support.annotation.RequiresApi;

import java.util.HashMap;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Context {
    HashMap<String, Object> context;

    // locks
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
    private final Lock readLock = readWriteLock.readLock();
    private final Lock writeLock = readWriteLock.writeLock();

    public Context() {
        context = new HashMap<>();
    }

    public void putValue(String key, Object value) {
        writeLock.lock();
        context.put(key, value);
        writeLock.unlock();
    }

    public Object getValue(String key) {
        Object res = null;
        readLock.lock();
        res = context.get(key);
        readLock.unlock();
        return res;
    }

    public Boolean existsKey(String key) {
        readLock.lock();
        Boolean res = context.containsKey(key);
        readLock.unlock();
        return res;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Context)) return false;
        Context context1 = (Context) o;
        return context.equals(context1.context) &&
                readWriteLock.equals(context1.readWriteLock) &&
                readLock.equals(context1.readLock) &&
                writeLock.equals(context1.writeLock);
    }

    @RequiresApi(api = Build.VERSION_CODES.KITKAT)
    @Override
    public int hashCode() {

        return Objects.hash(context, readWriteLock, readLock, writeLock);
    }
}
