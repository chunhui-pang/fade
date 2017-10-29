package net.floodlightcontroller.applications.appmodule.rulegraph;

import net.floodlightcontroller.applications.util.hsa.HeaderSpace;
import net.floodlightcontroller.applications.util.hsa.TernaryArray;
import net.floodlightcontroller.applications.util.trie.TrieNodeRetrievable;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.List;

/**
 * Defines an interface which describe the "flow rule node" used in {@link IRuleGraphService}.
 * This interface defines a switch, a match header space, output header space and the output port.
 * Setters only provided on match header space for we think it is the only priority we may directly modify.
 *
 * Note, we organize rules in a switch as an trie, so it must implements {@link TrieNodeRetrievable}
 *
 * @author chunhui (chunhui.pang@outlook.com)
 */
public interface IRuleNode extends TrieNodeRetrievable<Object> {
    /**
     * Return the switch which this node enforced
     * @return the switch's datapath id
     */
    DatapathId getDatapathId();

    /**
     * Retrieve inPort of current node, i.e., this node only matches packets which comes from given port
     * @return the inPort
     */
    OFPort getInPort();

    /**
     * Retrieve the priority of current rule node
     * @return the priority
     */
    int getPriority();

    /**
     * Retrieve header space this node can match.
     * In other words, this function return the header space this node matches
     * when it is the <b>only</b> rule enforced on the switch
     * @return the header space which this node can match
     */
    TernaryArray getMatchHS();

    /**
     * Return the header space this node real match.
     * Compared with {@link #getMatchHS()}, this function returns a subset of the return value of that.
     * This is because the header space of current rule can be "robed" by other rules with high priority
     *
     * @return the header space which this node really matches
     */
    HeaderSpace getReallyMatchHS();

    /**
     * Retrieve the header space this node can output, corresponding to the header space the node can match. <br />
     * Note, some nodes will rewrite their matched header space
     * @see #getMatchHS()
     * @return the header space the node can output
     */
    TernaryArray getOutputHS();

    /**
     * Retrieve the header space which this node will actually output.
     * The header space it "actually" output may be different from the one it "can" output,
     * as the matched header space will be robbed by other nodes
     * @see #getReallyMatchHS()
     * @return the header space this node actually outputs
     */
    HeaderSpace getReallyOutputHS();

    /**
     * get actions of the rule
     * @return the actions
     */
    List<OFAction> getActions();

    /**
     * get the out port of current node
     * @return the out port
     */
    OFPort getOutPort();

    /**
     * Adding a new rule node to the affected set
     * @param affected the affected node
     * @return if the node is added to the affected set
     */
    boolean addAffectedRuleNode(IRuleNode affected);

    /**
     * Removing a rule node from the affected set
     * @param affected the affected rule node
     * @return if the node is removed from affected set
     */
    boolean removeAffectedRuleNode(IRuleNode affected);

    /**
     * Retrieve nodes which are affected by current node.
     * In other words, this function returns nodes whose header space is "robbed" by current node's.
     * @implSpec you shoud return {@code null} or throw exception if this operation is not supported
     * @return list of nodes affected by current one
     */
    Iterable<IRuleNode> getAffectedRuleNodes();

    /**
     * Adding a new rule node that current node depends on, and subtracting its "can match" header space from current node's matched header space.
     * In other words, the rule "rob" header space from current rule
     * @param depends the new rule added to the dependOn set
     * @return whether the rule node is added
     */
    boolean addDependOnRuleNode(IRuleNode depends);

    /**
     * Removing a rule node from the dependOn set, and adding its "really matched" header space to current node's matched header space
     * @param depends the given rule node
     * @return whether the rule node is removed
     */
    boolean removeDependOnRuleNode(IRuleNode depends);

    /**
     * Retrieve nodes which affect current node.
     * In other words, this function returns nodes who "rob" header space from current node
     * @return the list of nodes current node depends on
     */
    Iterable<IRuleNode> getDependOnRuleNodes();

}
