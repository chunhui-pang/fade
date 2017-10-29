package net.floodlightcontroller.applications.fade.flow.aggregatedflow;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.exception.CannotSplitException;
import net.floodlightcontroller.applications.fade.exception.InvalidArgumentException;
import net.floodlightcontroller.applications.fade.exception.LogicError;
import net.floodlightcontroller.applications.fade.exception.OperationNotSupportedException;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Aggregated flow
 */
public class AggregatedFlow implements Flow {
    private static AtomicLong nextFlowId = new AtomicLong(1);

    private long flowId;
    private List<AggregatedFlowNode> flowNodes;


    private void fetchFlowId(){
        this.flowId = nextFlowId.getAndIncrement();
    }

    /**
     * constructor is only packet level accessable
     */
    AggregatedFlow(){
        this.fetchFlowId();
        this.flowNodes = Lists.newArrayList();
    }

    /**
     * constructor. (packet level accessible)
     * <b>copy</b> flow nodes to current object
     * @param flowNodes the specified flow nodes
     */
    AggregatedFlow(List<AggregatedFlowNode> flowNodes){
        this.fetchFlowId();
        this.flowNodes = Lists.newArrayList(flowNodes);
    }

    protected AggregatedFlow(long flowId){
        this.flowId = flowId;
        this.flowNodes = Lists.newArrayList();
    }

    protected AggregatedFlow(long flowId, Iterable<AggregatedFlowNode> flowNodes){
        this.flowId = flowId;
        this.flowNodes = Lists.newArrayList(flowNodes);
    }

    @Override
    public long getId() {
        return this.flowId;
    }

    @Override
    public int size() {
        if(this.flowNodes.size() == 0){
            return 0;
        } else {
            /* return the size of nodes */
            return this.flowNodes.get(0).size();
        }
    }

    @Override
    public int length() {
        return this.flowNodes.size();
    }

    @Override
    public List<AggregatedFlowNode> getFlowNode() {
        return this.flowNodes;
    }

    @Override
    public boolean addNode(FlowNode node) {
        if(null == node || !(node instanceof AggregatedFlowNode)){
            throw new LogicError("AggregatedFlow only contains AggregatedFlowNode!");
        } else {
            return this.flowNodes.add( (AggregatedFlowNode) node );
        }
    }

    @Override
    public boolean removeNode(FlowNode node) {
        if(null == node){
            return false;
        } else {
            /* the parameter's type is checked here */
            return this.flowNodes.remove(node);
        }
    }

    /**
     * Add every aggregated flow nodes with a new rule (package level accessible)
     * @param nodes the new rules
     * @return if is added
     */
    boolean mergeFlow(List<IRuleNode> nodes){
        if(null == nodes || this.flowNodes.size() != nodes.size()){
            throw new InvalidArgumentException("the length of parameter should be equal to the length of the flow");
        }
        Iterator<IRuleNode> it = nodes.iterator();
        Iterator<AggregatedFlowNode> nodeIt = this.flowNodes.iterator();
        while(nodeIt.hasNext()){
            AggregatedFlowNode afNode = nodeIt.next();
            IRuleNode ruleNode = it.next();
            afNode.addRuleNode(ruleNode);
        }
        return true;
    }

    @Override
    public List<AggregatedFlow> slice(int number) {
        if(number >= this.size()){
            return Collections.singletonList(new AggregatedFlow(this.flowId, this.flowNodes));
        } else {
            AggregatedFlow newAf1 = new AggregatedFlow(this.getId());
            AggregatedFlow newAf2 = new AggregatedFlow(this.getId());
            this.flowNodes.forEach(flowNode -> {
                List<IRuleNode> ruleNodes = flowNode.getRuleNodes();
                // copy rules
                AggregatedFlowNode newNode1 = new AggregatedFlowNode();
                for(int i = 0; i < number; i++){
                    newNode1.addRuleNode(ruleNodes.get(i));
                }
                AggregatedFlowNode newNode2 = new AggregatedFlowNode();
                for(int i = number; i < ruleNodes.size(); i++){
                    newNode2.addRuleNode(ruleNodes.get(i));
                }
                newAf1.addNode(newNode1);
                newAf2.addNode(newNode2);
            });
            List<AggregatedFlow> afs = Lists.newArrayListWithCapacity(2);
            afs.add(newAf1);
            afs.add(newAf2);
            return afs;
        }
    }

    @Override
    public List<AggregatedFlow> slice(IRuleNode[] ruleNodes) {
        if(0 == this.length()){
            return Collections.emptyList();
        } else {
            AggregatedFlowNode firstNode = this.flowNodes.get(0);
            List<IRuleNode> firstRuleNodes = firstNode.getRuleNodes();
            // build indices, and search for invalid parameters
            List<Integer> indices = Lists.newArrayListWithCapacity(ruleNodes.length);
            for(IRuleNode ruleNode : ruleNodes){
                int index = firstRuleNodes.indexOf(ruleNode);
                if(-1 == index){
                    throw new InvalidArgumentException(String.format("cannot find rule node %s in the first flow node", ruleNode));
                }
                indices.add(index);
            }
            Set<Integer> flow1Indices = Sets.newHashSet(indices);
            // construct new aggregated flows
            AggregatedFlow newAf1 = new AggregatedFlow(this.getId());
            AggregatedFlow newAf2 = new AggregatedFlow(this.getId());
            for(AggregatedFlowNode flowNode : this.flowNodes){
                List<IRuleNode> oldContents = flowNode.getRuleNodes();
                AggregatedFlowNode newNode1 = new AggregatedFlowNode();
                AggregatedFlowNode newNode2 = new AggregatedFlowNode();
                for(int idx = 0; idx < oldContents.size(); idx++){
                    if(flow1Indices.contains(idx)){
                        newNode1.addRuleNode(oldContents.get(idx));
                    } else {
                        newNode2.addRuleNode(oldContents.get(idx));
                    }
                }
                newAf1.addNode(newNode1);
                newAf2.addNode(newNode2);
            }
            List<AggregatedFlow> afs = Lists.newArrayListWithCapacity(2);
            afs.add(newAf1);
            afs.add(newAf2);
            return afs;
        }
    }

    @Override
    public List<AggregatedFlow> split(FlowNode splitPoint) {
        int idx = this.flowNodes.indexOf(splitPoint);
        if(-1 == idx){
            throw new InvalidArgumentException("cannot find the flow node in the flow");
        }
        AggregatedFlow af1 = new AggregatedFlow(this.getId(), this.flowNodes.subList(0, idx+1));
        AggregatedFlow af2 = new AggregatedFlow(this.getId(), this.flowNodes.subList(idx+1, this.flowNodes.size()));
        List<AggregatedFlow> afs = Lists.newArrayListWithCapacity(2);
        afs.add(af1);
        afs.add(af2);
        return afs;
    }

    @Override
    public List<AggregatedFlow> split(IRuleNode ruleNode) {
        throw new OperationNotSupportedException("split(IRuleNode) is not supported in AggregatedFlow");
    }

    @Override
    public AggregatedFlow split(FlowNode from, FlowNode to) {
        int idx0 = this.flowNodes.indexOf(from);
        int idx1 = this.flowNodes.indexOf(to);
        if(-1 == idx0 || -1 == idx1){
            throw new InvalidArgumentException("cannot find the flow nodes in the flow");
        }
        return new AggregatedFlow(this.getId(), this.flowNodes.subList(idx0, idx1+1));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        AggregatedFlow that = (AggregatedFlow) o;

        return new EqualsBuilder()
                .append(flowId, that.flowId)
                .append(flowNodes, that.flowNodes)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(flowId)
                .append(flowNodes)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "AggregatedFlow{" + "flowId=" + flowId + ", flowNodes=" + flowNodes + '}';
    }
}
