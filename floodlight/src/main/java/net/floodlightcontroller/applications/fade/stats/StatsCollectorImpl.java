package net.floodlightcontroller.applications.fade.stats;

import com.google.common.collect.Maps;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * the implementation of statistics collector
 */
public class StatsCollectorImpl implements StatsCollector {
    private static final Logger logger = LoggerFactory.getLogger(StatsCollectorImpl.class);
    // data storage
    private StatsContextImpl statsContext;

    public StatsCollectorImpl( ){
        this.statsContext = new StatsContextImpl();
    }

    @Override
    public void addStatsRecord(long index, long pktStats, long byteStats) {
        this.statsContext.addStats(index, pktStats, byteStats);
    }

    @Override
    public void invalidateStatsRecord(long index) {
        this.statsContext.releaseStats(index);
    }

    @Override
    public StatsContext getStatsContext() {
        return this.statsContext;
    }
}



