package net.floodlightcontroller.applications.util;

import java.util.concurrent.locks.Lock;

/**
 * A lock utility
 */
public class LockUtil {
    public static boolean checkLocked(Lock lock){
        if(lock.tryLock()){
            lock.unlock();
            return false;
        } else {
            return true;
        }
    }
}
