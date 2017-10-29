package net.floodlightcontroller.applications.fade.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Protecting threading pool from uncatched exceptions in Runnable.
 * When we use the {@link java.util.concurrent.ScheduledThreadPoolExecutor} to schedule tasks,
 * the exceptions in tasks cannot be catched and even noticed.
 * Thus, we crate a wrapper for these tasks so that exceptions could be traced.
 *
 */
public class ThreadPoolExcutionExceptionProtector implements Runnable {
    private static final Logger logger = LoggerFactory.getLogger(ThreadPoolExcutionExceptionProtector.class);
    private Runnable runnable;

    public ThreadPoolExcutionExceptionProtector(Runnable runnable) {
        this.runnable = runnable;
    }

    @Override
    public void run() {
        try {
            runnable.run();
        } catch (Throwable e){
            logger.error("Find a BUG in the code, an unexpected exception is catched", e);
        }
    }
}
