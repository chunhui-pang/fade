package net.floodlightcontroller.applications.fade.flow.singleflow;

import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Implementation of a single flow node.
 * A flow node in SingleFlow is just a rule node.
 */
public class SingleFlowNode implements FlowNode {
    private static long nextNodeId = 1;

    private long nodeId;
    private IRuleNode ruleNode;

    public SingleFlowNode(IRuleNode ruleNode){
        this.nodeId = nextNodeId++;
        this.ruleNode = ruleNode;
    }

    @Override
    public long getId() {
        return nodeId;
    }

    public IRuleNode getRuleNode(){
        return this.ruleNode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        SingleFlowNode that = (SingleFlowNode) o;

        return new EqualsBuilder()
                .append(nodeId, that.nodeId)
                .append(ruleNode, that.ruleNode)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(nodeId)
                .append(ruleNode)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "SingleFlowNode{" + "ruleNode=" + ruleNode + '}';
    }
}
