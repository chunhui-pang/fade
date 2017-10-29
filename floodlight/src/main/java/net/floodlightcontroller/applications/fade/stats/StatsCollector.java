package net.floodlightcontroller.applications.fade.stats;

import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;

/**
 * The statistics collector of all dedicated rules.
 * It collects and maintains the flow statistics of all dedicated rules.
 * When the controller receives rule expire message, it should retrieve the flow statistics and report it to this interface.
 */
public interface StatsCollector {
    /**
     * add new statistics to the collector when a rule expires
     * @param index the index of the installed dedicated rule
     * @param pktStats the packet counter of the rule
     * @param byteStats the byte counter of the rule
     */
    void addStatsRecord(long index, long pktStats, long byteStats);

    /**
     * invalidate the statistics record of a given rule node
     * @param index the index of the dedicated rule
     */
    void invalidateStatsRecord(long index);

    /**
     * Retrieve the statistics context to read statistics.
     * @return the statistics context
     */
    StatsContext getStatsContext();
}
