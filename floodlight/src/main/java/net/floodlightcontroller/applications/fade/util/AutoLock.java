package net.floodlightcontroller.applications.fade.util;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A wrapper for lock. It can free the lock automatically.
 */
public class AutoLock implements AutoCloseable {
    private Lock lock;

    public AutoLock(Lock lock){
        this.lock = lock;
        this.lock.lock();
    }

    @Override
    public void close() {
        this.lock.unlock();
    }
}
