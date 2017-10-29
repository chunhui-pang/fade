package net.floodlightcontroller.applications.fade.util;

import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;

/**
 * An utility to output blocks of logs, it would add a assign a unique block id to it.
 * It support auto close feature.
 * <code>
 * try (BlockLogger logger = new BlockLogger()){
 *     // blabla
 * }
 * </code>
 */
public interface BlockLogger extends Logger, Closeable {

    @Override
    void close() throws IOException;
}
