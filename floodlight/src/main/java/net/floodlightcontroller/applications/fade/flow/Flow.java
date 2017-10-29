package net.floodlightcontroller.applications.fade.flow;

import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;

import java.util.List;

/**
 * flows in the network
 */
public interface Flow {
    /**
     * Every flow should have an unique id
     * @return the flow's unique id
     */
    long getId();

    /**
     * the size of the flow
     * @return the size of the flow
     */
    int size();

    /**
     * the length of the flow
     * @return the length
     */
    int length();

    /**
     * get all flow nodes
     * @return the flow nodes
     */
    List<? extends FlowNode> getFlowNode();

    /**
     * add a new node to the flow
     * @param node the new node
     * @return if the node is added
     */
    boolean addNode(FlowNode node);

    /**
     * remove a flow node from the flow
     * @param node the removed node
     * @return if the node is removed
     */
    boolean removeNode(FlowNode node);

    /**
     * slice current flow into subflows, and the size of the subflow is {@code number} and {@code size-number}.
     * Note, current flow should not modified by the slice operation, and the ID of subflows is the same as the parent flow.
     * If the size of current flow is smaller than <code>number</code>, it just a copy of the current flow.
     * @param number the size of the subflow
     * @return the subflow
     */
    List<? extends Flow> slice(int number);

    /**
     * slice current flow to subflows by specified rule nodes.
     * @param ruleNodes the slice point
     * @return a new subflow
     * @see #slice(int)
     */
    List<? extends Flow> slice(IRuleNode[] ruleNodes);

    /**
     * split the flow into two subflows by a specified point. The size of the subflow is equal to the original one's,
     * and the length of smaller than the original one's.
     * The return value is the all subflows, and current flow is not modified
     * The difference between {@link #slice(int)} and {@link #slice(IRuleNode[])}  is that
     * the "slice" splits flows by its size, and "split" splits flows by its length.
     * @param splitPoint the split point
     * @return the first part of subflow.
     */
    List<? extends Flow> split(FlowNode splitPoint);

    /**
     * split current flow according to a rule node, and current flow is not modified
     * @param ruleNode the rule node
     * @return the subflow
     */
    List<? extends Flow> split(IRuleNode ruleNode);

    /**
     * split current flow to several subflows, the length of these subflows are smaller than it of the original flow.
     * The orignal flow is not modified.
     * @param from the first slice point
     * @param to the second slice point
     * @return the subflow between {@code from} and {@code to}
     */
    Flow split(FlowNode from, FlowNode to);
}
