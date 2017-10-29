package net.floodlightcontroller.applications.appmodule.rulegraph;

import net.floodlightcontroller.core.module.IFloodlightService;
import org.projectfloodlight.openflow.types.DatapathId;

import java.util.List;

/**
 * Defines the interface which describe an rule graph.
 * Rule graph is a acyclic graph, in which flow rules ({@link IRuleNode}) are nodes and the dependencies among flow rules are edges.
 * For example, a flow rule {@code A} has a directional link to a flow rule {@code B} if the following satisfy:
 * <ol>
 *     <li>{@code A} will forward traffic to the switch which contains {@code B}, and in that switch.<li>
 *     <li>the traffic is process by {@code B} immediately.</li>
 * </ol>
 *
 * In order to iterate over flow rules and flow paths, we also implement several ways to iterate over the object.
 * These iteration are implemented on "input nodes", "output nodes" and "dropping nodes",
 * with an order defined by priorities associated with rule nodes.
 * The priority associated with each rule node can be changed with a member function,
 * and it is initialized with 0.
 *
 * @author chunhui (chunhui.pang@outlook.com)
 */
public interface IRuleGraphService extends IFloodlightService {

    /**
     * Add a new rule node to current rule graph instance
     * @param ruleNode the new rule node
     * @return whether the node is added successfully
     */
    boolean addRuleNode(IRuleNode ruleNode);

    /**
     * Remove a rule node from current rule graph
     * @param ruleNode the node to be removed
     * @param strict is strictly delete
     * @return whether if <b>any</b> rule node is removed
     */
    boolean removeRuleNode(IRuleNode ruleNode, boolean strict);

    /**
     * Update the actions of an rule node, and the match of the rule node selects rule nodes to be updated
     * @param ruleNode the given rule node
     * @param strict if strict, the selected rule nodes would match "ALL" fields of the given rule node;
     *               otherwise, the selected rule nodes could match only a subset of the given rule node's.
     * @return if <b>any</b> rule is updated
     */
    boolean modifyRuleNode(IRuleNode ruleNode, boolean strict);

    /**
     * Determine whether a link exists between to flow rules
     * @param pre the first hop flow rule
     * @param nxt the next hop flow rule
     * @return true if the link exists, otherwise return false
     */
    boolean isLinkExist(IRuleNode pre, IRuleNode nxt);

    /**
     * Retrieve next hops of a rule node.
     * Multiple rule nodes could be returned for some rule matches a big trunk of header space.
     * However, this header space may be splitted into the header spaces of numbers of rules
     * @param ruleNode
     * @return A list, in which contains the following rule nodes
     */
    List<IRuleNode> getNextHops(IRuleNode ruleNode);

    /**
     * Retrieve the previous hops of a rule node.
     * As a flow rule may receive traffic from multiple other switches, there may multiple previous flow rules.
     * @param ruleNode
     * @return A list, in which contains previous rule nodes
     */
    List<IRuleNode> getPrevHops(IRuleNode ruleNode);

    /**
     * Retrieve rule nodes of a given switch
     * @param dpid the switch id
     * @return all rules on the switch
     */
    List<IRuleNode> getSwitchRules(DatapathId dpid);

    /**
     * Retrieve all rule nodes which forward traffic into "current network".
     * These nodes are boundries of current network.
     * @param limit the largest number of input nodes that this function returns, -1 means infinity
     * @return a list in which contains all input nodes
     */
    List<IRuleNode> getInputRuleNodes(int limit);

    /**
     * Retrieve all rule nodes by which traffics are forwarded into "outer" network.
     * Note that "dropping nodes" are also recognized as output nodes
     * @param limit the largest number of output nodes that this function returns, -1 means infinity
     * @see #getDroppingRuleNodes(int limit)
     * @return all output rule nodes
     */
    List<IRuleNode> getOutputRuleNodes(int limit);

    /**
     * Retrieve all rule nodes by which packets are dropped
     * @param limit the largest number of dropping nodes this function returns, -1 means infinity
     * @return all dropping rule nodes
     */
    List<IRuleNode> getDroppingRuleNodes(int limit);

    /**
     * Get the priority of a given rule node
     * @param ruleNode the rule node
     * @return the priority associated with it
     */
    Long getRuleNodePriority(IRuleNode ruleNode);

    /**
     * Change the priority associated with a given rule node
     * @param ruleNode the given rule node
     * @param priority the new priority
     * @return the old priority
     */
    Long setRuleNodePriority(IRuleNode ruleNode, long priority);

    /**
     * Determine whether a node is a dropping node
     * @param ruleNode the given node
     * @return true if the node drops packets
     */
    boolean isDroppingRuleNode(IRuleNode ruleNode);

    /**
     * Add new listener to events of current rule graph
     * @param listener the listener instance
     * @return whether the listener has been added
     */
    boolean addListener(IRuleGraphListener listener);

    /**
     * remove an existed listener from current rule graph
     * @param listener the listener instance
     * @return whether the listener has been removed
     */
    boolean removeListener(IRuleGraphListener listener);
}
