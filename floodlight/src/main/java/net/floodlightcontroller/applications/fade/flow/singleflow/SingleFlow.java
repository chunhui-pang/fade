package net.floodlightcontroller.applications.fade.flow.singleflow;

import com.google.common.collect.Lists;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.exception.InvalidArgumentException;
import net.floodlightcontroller.applications.fade.exception.LogicError;
import net.floodlightcontroller.applications.fade.exception.OperationNotSupportedException;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * A single flow.
 * A single flow is a set of packets which are forwarded by the same sequence of flow rules.
 * In the implementation of SingleFlow, each flow rule is implemented with a {@link net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode},
 * and in turn is implemented with {@link SingleFlowNode}.
 */
public class SingleFlow implements Flow {
    /* the size of single flow is always 1 */
    private static final int SINGLE_FLOW_LENGTH_SIZE = 1;
    private static AtomicLong nextFlowId = new AtomicLong(1);

    private long flowId;
    private List<SingleFlowNode> flowNodes;

    protected void setFlowId(){
        this.flowId = nextFlowId.getAndIncrement();
    }

    /**
     * constructor is only packet level accessible
     */
    SingleFlow(){
        this.setFlowId();
        this.flowNodes = new ArrayList<>();
    }

    /**
     * constructor.
     * <b>Copy</b> flow nodes to this object
     * @param flowNodes
     */
    SingleFlow(Iterable<SingleFlowNode> flowNodes){
        this.setFlowId();
        this.flowNodes = Lists.newArrayList(flowNodes);
    }

    protected SingleFlow(long flowId, Iterable<SingleFlowNode> flowNodes){
        this.flowId = flowId;
        this.flowNodes = Lists.newArrayList(flowNodes);
    }

    @Override
    public long getId() {
        return this.flowId;
    }

    @Override
    public int size() {
        return SINGLE_FLOW_LENGTH_SIZE;
    }

    @Override
    public int length() {
        return this.flowNodes.size();
    }

    @Override
    public List<SingleFlowNode> getFlowNode() {
        return this.flowNodes;
    }

    @Override
    public boolean addNode(FlowNode node) {
        if(null != node && node instanceof SingleFlowNode){
            return this.flowNodes.add((SingleFlowNode) node);
        } else {
            throw new LogicError("SingleFlow only accept SingleFlowNode as its nodes!");
        }
    }

    @Override
    public boolean removeNode(FlowNode node) {
        if(null == node || !(node instanceof SingleFlowNode)) {
            return false;
        }
        Iterator<SingleFlowNode> it = this.flowNodes.iterator();
        while(it.hasNext()){
            SingleFlowNode current = it.next();
            if(current.equals(node)){
                it.remove();
                return true;
            }
        }
        return false;
    }

    @Override
    public List<SingleFlow> slice(int number) {
        throw new OperationNotSupportedException("split operation is not supported by SingleFlow yet!");
    }

    @Override
    public List<SingleFlow> slice(IRuleNode[] ruleNodes) {
        throw new OperationNotSupportedException("split operation is not supported by SingleFlow yet!");
    }

    @Override
    public List<SingleFlow> split(FlowNode splitPoint) {
        List<SingleFlow> sfs = Lists.newArrayListWithCapacity(2);
        int idx = this.flowNodes.indexOf(splitPoint);
        if(-1 == idx){
            throw new InvalidArgumentException("cannot find the node in the flow");
        }

        SingleFlow subflow1 = new SingleFlow(this.getId(), this.flowNodes.subList(0, idx+1));
        SingleFlow subflow2 = new SingleFlow(this.getId(), this.flowNodes.subList(idx+1, this.flowNodes.size()));
        sfs.add(subflow1);
        sfs.add(subflow2);
        return sfs;
    }

    @Override
    public List<SingleFlow> split(IRuleNode ruleNode) {
        int idx = 0;
        while(idx < this.flowNodes.size() && !this.flowNodes.get(idx).getRuleNode().equals(ruleNode)){
            idx++;
        }
        if(idx == this.flowNodes.size()){
            throw new InvalidArgumentException("cannot find the node in the flow");
        }
        SingleFlow sf1 = new SingleFlow(this.getId(), this.flowNodes.subList(0, idx+1));
        SingleFlow sf2 = new SingleFlow(this.getId(), this.flowNodes.subList(idx+1, this.flowNodes.size()));
        List<SingleFlow> sfs = Lists.newArrayListWithCapacity(2);
        sfs.add(sf1);
        sfs.add(sf2);
        return sfs;
    }

    @Override
    public SingleFlow split(FlowNode from, FlowNode to) {
        // search for pos
        int idx0 = this.flowNodes.indexOf(from);
        int idx1 = this.flowNodes.indexOf(to);
        if(-1 == idx0 || -1 == idx1){
            throw new InvalidArgumentException("cannot find these node in the flow.");
        }
        return new SingleFlow(this.getId(), this.flowNodes.subList(idx0, idx1+1));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        SingleFlow that = (SingleFlow) o;

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
        return "SingleFlow{" + "flowId=" + flowId + ", flowNodes=" + flowNodes + '}';
    }
}
