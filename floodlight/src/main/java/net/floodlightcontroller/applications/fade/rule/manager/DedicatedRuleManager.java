package net.floodlightcontroller.applications.fade.rule.manager;

import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;

/**
 * Manage all dedicated rules that are installed to the network.
 * When we install a new dedicated rule to the network, we save it here, and set a key to index it.
 * When it expires, we could leverage the key to index the original dedicated rule.
 * This class fills the gap among {@link IRuleNode}, {@link org.projectfloodlight.openflow.protocol.OFFlowRemoved} and {@link org.projectfloodlight.openflow.protocol.OFFlowMod} messages.
 * We provide an interface ({@link RuleIndexer} to set and get the index of a {@link org.projectfloodlight.openflow.protocol.OFFlowMod} and {@link org.projectfloodlight.openflow.protocol.OFFlowRemoved}
 *
 * @implSpec the index could be implemented with various of fields in rule, however, it should be a long value.
 */
public interface DedicatedRuleManager {

    /**
     * add a new dedicated rule to manage
     * @param ruleNode the new dedicated rule
     * @param idx the index of the related real rule ({@link org.projectfloodlight.openflow.protocol.OFFlowMod}
     */
    void addDedicatedRule(IRuleNode ruleNode, long idx);

    /**
     * When a dedicated rule expires, it should be deleted.
     * @param ruleNode the expired dedicated rule
     */
    void removeDedicatedRule(IRuleNode ruleNode);

    /**
     * remove dedicated rule by its index
     * @param idx the index
     */
    void removeDedicatedRule(long idx);

    /**
     * get dedicated rule by its index
     * @param index the index
     * @return the dedicated rule
     */
    IRuleNode getDedicatedRule(long index);

    /**
     * start to dump dedicated flow rules' usage.
     * The caller could customize the initial waiting time for the first dump,
     * and the dump interval is determined by the implementation automatically.
     * @param initWaiting the time before the first dump (in milliseconds)
     */
    void dumpUsage(long initWaiting);
}
