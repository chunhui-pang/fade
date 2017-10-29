package net.floodlightcontroller.applications.fade.rule.manager;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowRemoved;
import org.projectfloodlight.openflow.types.U64;

/**
 * implementation of {@link RuleIndexer}.
 * We use the cookie field to embed index.
 */
public class RuleIndexerImpl implements RuleIndexer {

    @Override
    public OFFlowMod setIndex(OFFlowMod flowMod, long idx) {
        OFFlowMod.Builder builder = flowMod.createBuilder();
        builder.setCookie(U64.of(idx));
        return builder.build();
    }

    @Override
    public long getIndex(OFFlowMod flowMod) {
        return flowMod.getCookie().getValue();
    }

    @Override
    public long getIndex(OFFlowRemoved flowRemoved) {
        return flowRemoved.getCookie().getValue();
    }
}
