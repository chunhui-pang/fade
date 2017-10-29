package net.floodlightcontroller.applications.fade.rule.enforcer;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.types.DatapathId;

import java.util.List;

/**
 * enforce rules to the data plane
 */
public interface RuleEnforcer {
    /**
     * add immediately install task.
     * These rules would be enforced to the data plane immediately.
     * @implNote Actually, it takes time before these rules take effect.
     * @param rules the rules to be enforced
     * @param dpids the switches where to enforce them
     */
    void addTask(List<OFFlowMod> rules, List<DatapathId> dpids);

    /**
     * Add delayed enforcement task.
     *
     * @param rules the rules to be enforced.
     * @param dpids the switches where to enforce them.
     * @param delay the given time after which these rules would be enforced.
     * @implNote It's free to choose the time unit of {@code delay}.
     */
    void addDelayedTask(List<OFFlowMod> rules, List<DatapathId> dpids, long delay);

    /**
     * start enforce rules, the interval before the first enforcement could be customized,
     * and the interval between two enforcement is implementation-dependent.
     * @param initDelay the interval before the first enforcement (in milliseconds)
     */
    void startEnforce(long initDelay);
}
