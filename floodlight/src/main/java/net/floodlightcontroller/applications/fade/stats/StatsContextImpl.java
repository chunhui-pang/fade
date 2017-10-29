package net.floodlightcontroller.applications.fade.stats;

import com.google.common.collect.Maps;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Implementation of {@link StatsContext}.
 * Concurrent access is supported.
 * Only public to current package.
 */
class StatsContextImpl implements StatsContext {
    private static final Logger logger = LoggerFactory.getLogger(StatsContextImpl.class);
    private Map<Long, Long> packetCount;
    private Map<Long, Long> byteCount;

    // for note that hasn't reported any flow statistics, we use a default value (Long.MIN_VALUE).
    // This very tiny value assures that every equality constraint on it would fail
    private static final long DEFAULT_STATS = Long.MIN_VALUE;

    public StatsContextImpl( ){
        this.packetCount = Maps.newConcurrentMap();
        this.byteCount = Maps.newConcurrentMap();
    }

    @Override
    public long getPacketCount(long index) {
        return this.packetCount.getOrDefault(index, DEFAULT_STATS);
    }

    @Override
    public long getByteCount(long index) {
        return this.byteCount.getOrDefault(index, DEFAULT_STATS);
    }

    @Override
    public void releaseStats(long index) {
        if(this.packetCount.containsKey(index)) {
            this.packetCount.remove(index);
            this.byteCount.remove(index);
        } else {
            logger.warn("The rule with the index {} hasn't reported any statistics yet.", index);
        }
    }

    void addStats(long index, long pkts, long bytes) {
        this.packetCount.put(index, pkts);
        this.byteCount.put(index, bytes);
    }
}

