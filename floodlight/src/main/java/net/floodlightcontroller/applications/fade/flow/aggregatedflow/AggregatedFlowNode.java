package net.floodlightcontroller.applications.fade.flow.aggregatedflow;

import com.google.common.collect.Lists;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.List;

/**
 * node of aggregated flow, each of it contains several flow rules ({@link net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode}).
 */
public class AggregatedFlowNode implements FlowNode {
    private static long nextNodeId = 1;

    private long nodeId;
    private List<IRuleNode> ruleNodes;

    protected void fetchNodeId(){
        this.nodeId = nextNodeId++;
    }

    public AggregatedFlowNode(){
        this.fetchNodeId();
        this.ruleNodes = Lists.newArrayList();
    }

    public AggregatedFlowNode(List<IRuleNode> ruleNodes){
        this.fetchNodeId();
        this.ruleNodes = Lists.newArrayList(ruleNodes);
    }

    @Override
    public long getId() {
        return this.nodeId;
    }

    public boolean addRuleNode(IRuleNode ruleNode){
        return this.ruleNodes.add(ruleNode);
    }

    public boolean removeRuleNode(IRuleNode ruleNode){
        return this.ruleNodes.remove(ruleNode);
    }

    public List<IRuleNode> getRuleNodes(){
        return this.ruleNodes;
    }

    public int size(){
        return this.ruleNodes.size();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AggregatedFlowNode that = (AggregatedFlowNode) o;

        return new EqualsBuilder()
                .append(nodeId, that.nodeId)
                .append(ruleNodes, that.ruleNodes)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(nodeId)
                .append(ruleNodes)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "AggregatedFlowNode{" + "ruleNodes=" + ruleNodes + '}';
    }
}
