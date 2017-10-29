package net.floodlightcontroller.applications.fade.rule.manager;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;

/**
 * set and get index for {@link OFFlowMod} and {@link OFFlowRemoved} message.
 * The indexer should return the same index for a {@link OFFlowMod} and its corresponding {@link OFFlowRemoved} messages.
 */
public interface RuleIndexer {
    /**
     * set index for a flow rule
     * @param flowMod the flow rule
     * @param idx the index
     * @return the new flow rule
     */
    OFFlowMod setIndex(OFFlowMod flowMod, long idx);

    /**
     * get index from flow rule
     * @param flowMod the flow rule
     * @return the index
     */
    long getIndex(OFFlowMod flowMod);

    /**
     * get index from flow removed message
     * @param flowRemoved the flow removed message
     * @return the index
     */
    long getIndex(OFFlowRemoved flowRemoved);
}
