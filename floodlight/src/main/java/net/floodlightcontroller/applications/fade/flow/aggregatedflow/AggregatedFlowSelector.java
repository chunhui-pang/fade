package net.floodlightcontroller.applications.fade.flow.aggregatedflow;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleGraphListener;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleGraphService;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.FlowSelector;
import net.floodlightcontroller.applications.fade.util.AutoLock;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * The flow selector of aggregated flow
 */
public class AggregatedFlowSelector implements FlowSelector, IRuleGraphListener {
    private static final Logger logger = LoggerFactory.getLogger(AggregatedFlowSelector.class);
    private IRuleGraphService ruleGraphService;
    private PriorityQueue<AggregatedFlow> flows;
    private Map<AggregatedFlow, Long> flowPriority;
    private Map<IRuleNode, Set<AggregatedFlow>> ruleMap;
    private Map<RuleNodeActions, Set<AggregatedFlow>> actionFlowMap;
    private ReadWriteLock rwFlowLock;
    private ReentrantLock dumpLock = new ReentrantLock();

    private class RuleNodeActions {
        private DatapathId dpid;
        private OFPort ofPort;
        private List<OFAction> actions;
        public RuleNodeActions(IRuleNode ruleNode){
            this.dpid = ruleNode.getDatapathId();
            this.ofPort = ruleNode.getOutPort();
            this.actions = ruleNode.getActions();
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            RuleNodeActions that = (RuleNodeActions) o;
            return new EqualsBuilder().append(dpid, that.dpid).append(ofPort, that.ofPort).append(actions, that.actions).isEquals();
        }
        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(dpid).append(ofPort).append(actions).toHashCode();
        }
    }

    public AggregatedFlowSelector(IRuleGraphService ruleGraphService){
        this.ruleGraphService = ruleGraphService;
        this.rwFlowLock = new ReentrantReadWriteLock();
        this.flowPriority = Maps.newHashMap();
        this.flows = new PriorityQueue<>(Comparator.comparingLong(flow -> this.flowPriority.getOrDefault(flow, 0L)));
        this.ruleMap = Maps.newConcurrentMap();
        this.actionFlowMap = Maps.newHashMap();
        this.ruleGraphService.addListener(this);
    }

    @Override
    public void ruleAdded(IRuleNode node) {
        // ruleAdded -> createFlowNode -> merge local flow nodes
        if(this.ruleMap.containsKey(node)){
            logger.warn("current flow selector has contained ths node {}", node);
            return;
        }
        List<AggregatedFlowNode> afn = Lists.newArrayList(new AggregatedFlowNode(Lists.newArrayList(node)));
        AggregatedFlow af = new AggregatedFlow(afn);
        this.rwFlowLock.writeLock().lock();
        try{
            this.addFlow(af);
            this.tryMerge(af);
        } finally {
            this.rwFlowLock.writeLock().unlock();
        }
        this.dumpFlows();
    }

    @Override
    public void ruleRemoved(IRuleNode node) {
        // ruleRemoved -> slice -> split -> try merge
        this.rwFlowLock.writeLock().lock();
        try{
            this.ruleMap.getOrDefault(node, Collections.emptySet()).forEach( af -> {
                Pair<Integer, Integer> idx = this.identifyRuleNode(af, node);
                if(null == idx){
                    logger.error("cannot find rule node {} in flow {}", node, af);
                } else {
                    Pair<AggregatedFlow, AggregatedFlow> flows = this.split(af,
                            af.getFlowNode().get(0).getRuleNodes().get(idx.getRight()));
                    if(flows.getLeft().size() == 1) { // yes, the flow that contains only one SingleFlow
                        AggregatedFlow sf = flows.getLeft();
                        List<AggregatedFlowNode> nodes1 = Lists.newArrayList(sf.getFlowNode().subList(0, idx.getLeft()));
                        List<AggregatedFlowNode> nodes2 = Lists.newArrayList(sf.getFlowNode().subList(idx.getLeft()+1, sf.getFlowNode().size()));
                        AggregatedFlow newAf1 = new AggregatedFlow(nodes1);
                        AggregatedFlow newAf2 = new AggregatedFlow(nodes2);
                        this.clearFlow(sf);
                        this.addFlow(newAf1);
                        this.tryMerge(newAf1);
                        this.addFlow(newAf2);
                        this.tryMerge(newAf2);
                    }
                }
            });
        } finally {
            this.rwFlowLock.writeLock().unlock();
        }
        this.dumpFlows();
    }

    @Override
    public void ruleModified(IRuleNode node) {
        // leverage a remove and an addition to simulate this event
        this.rwFlowLock.writeLock().lock();
        try {
            this.ruleRemoved(node);
            this.ruleAdded(node);
        } finally {
            this.rwFlowLock.writeLock().unlock();
        }
    }

    @Override
    public void linkAdded(IRuleNode prev, IRuleNode next) {
        // linkAdded -> prevFlows, nextFlows ->
        //     A. prevflow SF, nextflow SF, (copy), link, try to merge
        //     B. prevflow SF, nextflow AF, split/copy, link, try to merge
        //     C. prevflow AF, nextflow SF, split/copy, link, try to merge
        //     D. prevflow AF, nextflow AF, split/copy, link, try to merge
        this.rwFlowLock.writeLock().lock();
        try{
            // default is a new flow with the current rule node
            AggregatedFlow defaultPrev = new AggregatedFlow(Collections.singletonList(new AggregatedFlowNode(Collections.singletonList(prev))));
            AggregatedFlow defaultNext = new AggregatedFlow(Collections.singletonList(new AggregatedFlowNode(Collections.singletonList(next))));
            List<AggregatedFlow> prevFlows = Lists.newArrayList(this.ruleMap.getOrDefault(prev, Collections.singleton(defaultPrev)));
            List<AggregatedFlow> nextFlows = Lists.newArrayList(this.ruleMap.getOrDefault(next, Collections.singleton(defaultNext)));
            Map<AggregatedFlow, Pair<Integer, Integer>> prevIdx = Maps.newHashMap();
            Map<AggregatedFlow, Pair<Integer, Integer>> nextIdx = Maps.newHashMap();
            // we use set to filter out duplicated entries
            Set<List<AggregatedFlowNode>> prevNodeLists = Sets.newHashSet();
            Set<List<AggregatedFlowNode>> nextNodeLists = Sets.newHashSet();
            for(AggregatedFlow pf : prevFlows){
                Pair<Integer, Integer> pos = this.identifyRuleNode(pf, prev);
                if(prevNodeLists.add(pf.getFlowNode().subList(0, pos.getLeft()+1))) {
                    prevIdx.put(pf, pos);
                }
            }
            for(AggregatedFlow nf : nextFlows){
                Pair<Integer, Integer> pos = this.identifyRuleNode(nf, next);
                if(nextNodeLists.add(nf.getFlowNode().subList(pos.getLeft(), nf.getFlowNode().size()))) {
                    nextIdx.put(nf, pos);
                }
            }
            for(AggregatedFlow pf : prevFlows){
                int pfIdxOfFlowNode = prevIdx.get(pf).getLeft(),
                        pfIdxOfRuleNode = prevIdx.get(pf).getRight();
                // construct the first part of flow
                AggregatedFlow firstPart = null;
                if(pfIdxOfFlowNode == pf.getFlowNode().size()-1) { // split
                    Pair<AggregatedFlow, AggregatedFlow> pfSubflows = this.split(pf, pf.getFlowNode().get(0).getRuleNodes().get(pfIdxOfRuleNode));
                    firstPart = pfSubflows.getLeft();
                    // remove from management
                    this.clearFlow(firstPart);
                } else { // copy
                    firstPart = new AggregatedFlow();
                    int idx = 0;
                    Iterator<AggregatedFlowNode> itAfn = pf.getFlowNode().iterator();
                    while(idx++ <= pfIdxOfFlowNode){
                        AggregatedFlowNode afn = itAfn.next();
                        firstPart.addNode(new AggregatedFlowNode(Lists.newArrayList(afn.getRuleNodes().get(pfIdxOfRuleNode))));
                    }
                }
                for(AggregatedFlow nf : nextFlows){
                    int nfIdxofFlowNode = nextIdx.get(nf).getLeft(),
                            nfIdxOfRuleNode = nextIdx.get(nf).getRight();
                    AggregatedFlow lastPart = null;
                    if(0 == nfIdxofFlowNode){ // split
                        Pair<AggregatedFlow, AggregatedFlow> subflows = this.split(nf, nf.getFlowNode().get(0).getRuleNodes().get(nfIdxOfRuleNode));
                        lastPart = subflows.getLeft();
                        // remove from management
                        this.clearFlow(lastPart);
                    } else { // copy
                        lastPart = new AggregatedFlow();
                        ListIterator<AggregatedFlowNode> itAfn = nf.getFlowNode().listIterator(nfIdxofFlowNode);
                        while(itAfn.hasNext()){
                            lastPart.addNode(new AggregatedFlowNode(Lists.newArrayList(itAfn.next().getRuleNodes().get(nfIdxOfRuleNode))));
                        }
                    }
                    // merge flow and try to merge with others
                    AggregatedFlow newAf = new AggregatedFlow(firstPart.getFlowNode());
                    for(AggregatedFlowNode afn : lastPart.getFlowNode()){
                        newAf.addNode(afn);
                    }
                    this.addFlow(newAf);
                    this.tryMerge(newAf);
                }
            }
        } finally {
            this.rwFlowLock.writeLock().unlock();
        }
        this.dumpFlows();
    }

    @Override
    public void linkRemoved(IRuleNode prev, IRuleNode next) {
        // linkRemoved -> slice -> split -> try merge
        this.rwFlowLock.writeLock().lock();
        try{
            // identify flows that contains this link
            Set<AggregatedFlow> candidiateFlows = Sets.newHashSet(this.ruleMap.getOrDefault(prev, Collections.emptySet()));
            Iterator<AggregatedFlow> it = candidiateFlows.iterator();
            List<AggregatedFlow> flows = Lists.newArrayList();
            List<Pair<Integer, Integer>> idxs = Lists.newArrayList();
            while(it.hasNext()){
                AggregatedFlow af = it.next();
                Pair<Integer, Integer> idx = this.identifyRuleNode(af, prev);
                if(null == idx){// impossible
                    it.remove();
                    continue;
                }
                if( (idx.getLeft() + 1 >= af.getFlowNode().size()) ||
                        (idx.getRight() >= af.getFlowNode().get(idx.getLeft()+1).getRuleNodes().size())){
                    it.remove();
                    continue;
                }
                if(!next.equals(af.getFlowNode().get(idx.getLeft()+1).getRuleNodes().get(idx.getRight()))){
                    it.remove();
                    continue;
                }
                flows.add(af);
                idxs.add(idx);
            }
            it = flows.iterator();
            Iterator<Pair<Integer, Integer>> itIdx = idxs.iterator();
            int cur = 0, all = flows.size();
            while(it.hasNext()){
                AggregatedFlow af = it.next();
                Pair<Integer, Integer> idx = itIdx.next();
                Pair<AggregatedFlow, AggregatedFlow> subflows = this.split(af, af.getFlowNode().get(idx.getLeft()),
                        af.getFlowNode().get(idx.getLeft()).getRuleNodes().get(idx.getRight()));
                if(subflows.getLeft() != null) {
                    this.tryMerge(subflows.getLeft());
                }
                if(subflows.getRight() != null){
                    this.tryMerge(subflows.getRight());
                }
            }
        } finally {
            this.rwFlowLock.writeLock().unlock();
        }
    }

    @Override
    public List<Flow> getFlows(int limit) {
        int retrieveSize = (limit <= 0) ? Integer.MAX_VALUE : limit;
        List<Flow> result = Lists.newArrayList();
        try(AutoLock l = new AutoLock(this.rwFlowLock.writeLock())){
            if(retrieveSize >= this.flows.size()){
                return this.getAllFlows();
            }
            for(; limit > 0; limit--){
                AggregatedFlow af = this.flows.poll();
                this.resetPriority(af);
                this.flows.add(af);
                result.add(af);
            }
            this.dumpFlows();
        }
        return result;
    }

    @Override
    public List<Flow> getAllFlows() {
        try (AutoLock l = new AutoLock(this.rwFlowLock.readLock())){
            this.dumpFlows();
            return Lists.newArrayList(this.flows);
        }
    }

    private void resetPriority(AggregatedFlow af){
        this.flowPriority.put(af, Calendar.getInstance().getTimeInMillis());
    }

    private void clearFlow(AggregatedFlow af){
        this.flowPriority.remove(af);
        this.flows.remove(af);
        af.getFlowNode().forEach( fn -> {
            // update action flow map
            if(fn.getRuleNodes().size() != 0) {
                IRuleNode example = fn.getRuleNodes().get(0);
                RuleNodeActions actionIdx = new RuleNodeActions(example);
                // getOrDefault disable the function returns null
                this.actionFlowMap.getOrDefault(actionIdx, Collections.emptySet()).remove(af);
            }
            // update rule flow map
            fn.getRuleNodes().forEach( rn -> {
                this.ruleMap.getOrDefault(rn, Collections.emptySet()).remove(af);
            });
        });
    }

    private void addFlow(AggregatedFlow af){
        if(af.size() == 0 || af.length() == 0){
            return;
        }
        this.resetPriority(af);
        this.flows.add(af);
        af.getFlowNode().forEach(fn -> {
            // update action flow map
            if(fn.getRuleNodes().size() != 0) {
                IRuleNode example = fn.getRuleNodes().get(0);
                RuleNodeActions actionIdx = new RuleNodeActions(example);
                if(!this.actionFlowMap.containsKey(actionIdx)){
                    this.actionFlowMap.put(actionIdx, Sets.newHashSet());
                }
                this.actionFlowMap.get(actionIdx).add(af);
            }
            // update rule map
            fn.getRuleNodes().forEach( rn -> {
                if(!this.ruleMap.containsKey(rn)){
                    this.ruleMap.put(rn, Sets.newHashSet());
                }
                this.ruleMap.get(rn).add(af);
            });
        });
    }

    private void tryMerge(AggregatedFlow af) {
        // try to find flows that could merge with af
        if (af.size() == 0 || af.length() == 0) {
            return;
        }
        IRuleNode exemplar = af.getFlowNode().get(0).getRuleNodes().get(0);
        RuleNodeActions actionIdx = new RuleNodeActions(exemplar);
        Set<AggregatedFlow> possibleMerges = this.actionFlowMap.getOrDefault(actionIdx, Collections.emptySet());
        for(AggregatedFlow possibleMerge : possibleMerges){
            if(possibleMerge != af && null != this.tryMerge(af, possibleMerge)){
                break; // only one merge could be possible
            }
        }
    }


    /**
     * try to merge the two aggregated flow. If flows are merged, old flows are erased.
     * @param af1 the first aggregated flow
     * @param af2 the second aggregated flow
     * @return null if fails to merge, otherwise return the new aggregated flow
     */
    private AggregatedFlow tryMerge(AggregatedFlow af1, AggregatedFlow af2) {
        // how to merge flows:
        //     1. check if they can be merged (length), and handle specific scenarios
        //     2. get example rules
        //     3. check if they could be merged by checking the actions and out ports of all example rules
        if( af1.length() != af2.length() ) {
            return null;
        }
        if(af1.size() == 0 || af1.length() == 0) {
            // return af2
            return new AggregatedFlow(af2.getFlowNode());
        }
        if(af2.size() == 0 || af2.length() == 0) {
            return new AggregatedFlow(af1.getFlowNode());
        }
        if(logger.isDebugEnabled()) {
            logger.debug("try to merge flow with same length: {}, {}", af1, af2);
        }
        for(int idx = 0; idx < af1.length(); idx++){
            IRuleNode exemplar1 = af1.getFlowNode().get(idx).getRuleNodes().get(0);
            IRuleNode exemplar2 = af2.getFlowNode().get(idx).getRuleNodes().get(0);
            if(!this.canMerge(exemplar1, exemplar2)){
                return null;
            }
        }
        // merge
        List<AggregatedFlowNode> newFlowNodes = Lists.newArrayListWithCapacity(af1.length());
        Iterator<AggregatedFlowNode> itLeft = af1.getFlowNode().iterator();
        Iterator<AggregatedFlowNode> itRight = af2.getFlowNode().iterator();
        while(itLeft.hasNext()){
            AggregatedFlowNode afn1 = itLeft.next(), afn2 = itRight.next();
            List<IRuleNode> ruleNodes = Lists.newArrayListWithCapacity(afn1.size() + afn2.size());
            ruleNodes.addAll(afn1.getRuleNodes());
            ruleNodes.addAll(afn2.getRuleNodes());
            newFlowNodes.add(new AggregatedFlowNode(ruleNodes));
        }
        AggregatedFlow newAf = new AggregatedFlow(newFlowNodes);
        // update peripheral configurations
        this.clearFlow(af1);
        this.clearFlow(af2);
        this.addFlow(newAf);
        if(logger.isDebugEnabled()) {
            logger.debug("merge flow {}, {}", af1, af2);
        }
        return newAf;
    }

    private boolean canMerge(IRuleNode rn1, IRuleNode rn2) {
        if (rn1 == rn2) {
            return true;
        }
        if ((rn1 == null || rn2 == null) && rn1 != rn2){
            return false;
        }
        return new RuleNodeActions(rn1).equals(new RuleNodeActions(rn2));
    }

    /**
     * split flow according to rule on the first node.
     * The flow that contains the rule becomes single flow, and others are become another one.
     */
    private Pair<AggregatedFlow, AggregatedFlow> split(AggregatedFlow af, IRuleNode splitNode){
        if(af.length() == 0 || af.size() == 0){
            return null;
        }
        int idxOfSplit = af.getFlowNode().get(0).getRuleNodes().indexOf(splitNode);
        List<AggregatedFlowNode> newNode1 = Lists.newArrayListWithCapacity(af.length());
        List<AggregatedFlowNode> newNode2 = Lists.newArrayListWithCapacity(af.length());
        af.getFlowNode().forEach( fn -> {
            IRuleNode singleSplit = fn.getRuleNodes().get(idxOfSplit);
            AggregatedFlowNode afn1 = new AggregatedFlowNode(Lists.newArrayList(singleSplit));
            List<IRuleNode> ruleNodes = Lists.newArrayListWithCapacity(fn.getRuleNodes().size()-1);
            fn.getRuleNodes().forEach( rn -> {
                if(rn != singleSplit){
                    ruleNodes.add(rn);
                }
            });
            AggregatedFlowNode afn2 = new AggregatedFlowNode(ruleNodes);
            newNode1.add(afn1);
            newNode2.add(afn2);
        });
        AggregatedFlow newAf1 = new AggregatedFlow(newNode1);
        AggregatedFlow newAf2 = new AggregatedFlow(newNode2);
        this.clearFlow(af);
        this.addFlow(newAf1);
        this.addFlow(newAf2);
        return new ImmutablePair<>(newAf1, newAf2);
    }

    /**
     * split the flow according to a rule node on a flow node
     */
    private Pair<AggregatedFlow, AggregatedFlow> split(AggregatedFlow af, AggregatedFlowNode fn, IRuleNode splitNode){
        int idxOfSplit = fn.getRuleNodes().indexOf(splitNode);
        if(idxOfSplit < 0){
            return null;
        }
        IRuleNode splitNodeOnFirstFn = af.getFlowNode().get(0).getRuleNodes().get(idxOfSplit);
        return this.split(af, splitNodeOnFirstFn);
    }

    /**
     * split the flow into two sections according to split node
     */
    private Pair<AggregatedFlow, AggregatedFlow> split(AggregatedFlow af, AggregatedFlowNode splitNode){
        int idxOfSplit = af.getFlowNode().indexOf(splitNode);
        if(idxOfSplit < 0){
            return null;
        }
        if(idxOfSplit == af.getFlowNode().size()-1){
            return new ImmutablePair<>(af, null);
        }
        AggregatedFlow newAf1 = new AggregatedFlow(Lists.newArrayList(af.getFlowNode().subList(0, idxOfSplit+1)));
        AggregatedFlow newAf2 = new AggregatedFlow(Lists.newArrayList(af.getFlowNode().subList(idxOfSplit+1, af.getFlowNode().size())));
        this.clearFlow(af);
        this.addFlow(newAf1);
        this.addFlow(newAf2);
        return new ImmutablePair<>(newAf1, newAf2);
    }

    private Pair<Integer, Integer> identifyRuleNode(AggregatedFlow af, IRuleNode ruleNode) {
        int idxOfFlowNode = 0;
        int idxOfRuleNode = -1;
        Iterator<AggregatedFlowNode> itAfn = af.getFlowNode().iterator();
        while(itAfn.hasNext()){
            AggregatedFlowNode afn = itAfn.next();
            if ( (idxOfRuleNode = afn.getRuleNodes().indexOf(ruleNode)) >= 0){
                break;
            }
            idxOfFlowNode++;
        }
        if(-1 == idxOfRuleNode){
            return null;
        }
        return new ImmutablePair<>(idxOfFlowNode, idxOfRuleNode);
    }

    public void dumpFlows () {
        if(logger.isDebugEnabled()) {
            try (AutoLock l1 = new AutoLock(this.rwFlowLock.readLock())) {
                try(AutoLock l2 = new AutoLock(this.dumpLock)) {
                    // lock the output so that message would not overlap
                    logger.debug("----- AGGREGATED FLOW SELECTOR INFO -----");
                    StringBuffer sb = new StringBuffer();
                    for (AggregatedFlow af : this.flows)
                        sb.append(af.toString()).append(", ");
                    logger.debug(sb.toString());
                    logger.debug("----- AGGREGATED FLOW SELECTOR INFO END -----");
                }
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("AggregatedFlowSelector[");
        sb.append("flowSize=").append(this.flows.size());
        sb.append(", flowIds=[");
        for(AggregatedFlow af : this.flows)
            sb.append(af.getId()).append(", ");
        sb.append("]");
        sb.append("]");
        return sb.toString();
    }
}
