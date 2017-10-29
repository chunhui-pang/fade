package net.floodlightcontroller.applications.fade.stats;

import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;

/**
 * An entity to restore flow statistics.
  */
public interface StatsContext {
    /**
     * retrieves the packet count of a given rule
     * @param index the index of the installed dedicated rule
     * @return the packet count
     */
    long getPacketCount(long index);

    /**
     * retrieves the byte count of a given rule
     * @param index the index of installed dedicated rule
     * @return the packet count
     */
    long getByteCount(long index);

    /**
     * release the statistics of a given rule
     * @param index the index of the dedicated rule
     */
    void releaseStats(long index);
}
