package net.floodlightcontroller.applications.appmodule.rulegraph;

import com.google.common.collect.*;
import net.floodlightcontroller.applications.appmodule.concretetopology.IConcreteTopologyListener;
import net.floodlightcontroller.applications.appmodule.concretetopology.IConcreteTopologyService;
import net.floodlightcontroller.applications.appmodule.rulegraph.rule.FlowRuleNode;
import net.floodlightcontroller.applications.appmodule.rulegraph.rule.TernaryArrayTrieNodeRetrievable;
import net.floodlightcontroller.applications.fade.FADEController;
import net.floodlightcontroller.applications.fade.util.AutoLock;
import net.floodlightcontroller.applications.util.LockUtil;
import net.floodlightcontroller.applications.util.hsa.HeaderSpace;
import net.floodlightcontroller.applications.util.hsa.TernaryArray;
import net.floodlightcontroller.applications.util.trie.Trie;
import net.floodlightcontroller.core.*;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.protocol.*;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * Implements a rule graph ({@link IRuleGraphService}) as a floodlight module.
 * Note that rule graph should reactively change with topology changes, and it implements itself as an listener of concrete topology.
 *
 * @implNote Rule graph may be accessed concurrently. We use lock to protect it.
 * Actually, a ReadWriteLock is used as for write is far less in our scenario.
 */
public class RuleGraphService implements IFloodlightModule, IRuleGraphService, IConcreteTopologyListener, IOFMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(RuleGraphService.class);
    private static final String APP_NAME = "ruleGraph";
    private IConcreteTopologyService concreteTopologyService;
    private IFloodlightProviderService floodlightProviderService;
    private IThreadPoolService threadPoolService;
    private Set<IRuleGraphListener> listeners;

    /* defines the topology of rule graph */
    private ReadWriteLock ruleLock;
    private Map<DatapathId, Trie<IRuleNode, Object>> rules;
    private ReadWriteLock iterateLock;
    private Map<IRuleNode, Set<IRuleNode>> nextRules;
    private Map<IRuleNode, Set<IRuleNode>> prevRules;

    /* defines iterate logic */
    private ReadWriteLock outputRuleLock;
    private Queue<IRuleNode> outputRules;
    private Map<DatapathId, Set<IRuleNode>> outputMap;

    private ReadWriteLock inputRuleLock;
    private Queue<IRuleNode> inputRules;
    private Map<DatapathId, Set<IRuleNode>> inputMap;

    private ReadWriteLock droppingRuleLock;
    private Queue<IRuleNode> droppingRules;
    private Map<DatapathId, Set<IRuleNode>> droppingMap;
    /* use concurrent map here */
    private ConcurrentMap<IRuleNode, Long> selectPriority;

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return Collections.singletonList(IRuleGraphService.class);
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return Collections.singletonMap(IRuleGraphService.class, this);
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        return Lists.newArrayList(IFloodlightProviderService.class, IConcreteTopologyService.class, IThreadPoolService.class);
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        this.floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
        this.concreteTopologyService = context.getServiceImpl(IConcreteTopologyService.class);
        this.threadPoolService = context.getServiceImpl(IThreadPoolService.class);

        this.ruleLock = new ReentrantReadWriteLock();
        this.iterateLock = new ReentrantReadWriteLock();
        this.rules = Maps.newHashMap();
        this.nextRules = Maps.newHashMap();
        this.prevRules = Maps.newHashMap();
        this.outputRuleLock = new ReentrantReadWriteLock();
        this.outputRules = new PriorityQueue<>(new RuleNodeComparator());
        this.outputMap = Maps.newHashMap();
        this.inputRuleLock = new ReentrantReadWriteLock();
        this.inputRules = new PriorityQueue<>(new RuleNodeComparator());
        this.inputMap = Maps.newHashMap();
        this.droppingRuleLock = new ReentrantReadWriteLock();
        this.droppingRules = new PriorityQueue<>(new RuleNodeComparator());
        this.droppingMap = Maps.newHashMap();
        this.selectPriority = Maps.newConcurrentMap();
        this.listeners = Sets.newCopyOnWriteArraySet();
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        this.floodlightProviderService.addOFMessageListener(OFType.FLOW_MOD, this);
        this.concreteTopologyService.addListener(this);
        logger.info("module ruleGraphService started :)");
    }

    @Override
    public String getName() {
        return APP_NAME;
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cntx) {
        logger.debug("receive OpenFlow message {} on switch {}", msg, sw.getId());
        try {
            switch (msg.getType()) {
                case FLOW_MOD:
                    OFFlowMod flowMod = (OFFlowMod) msg;
                    if (AppCookie.extractApp(flowMod.getCookie()) != FADEController.getAppId()) { // only hijack flow rules installed by other applications
                        if (EthType.IPv4.equals(flowMod.getMatch().get(MatchField.ETH_TYPE))) { // only hijack ipv4 traffic
                            IRuleNode ruleNode = new FlowRuleNode(flowMod, sw.getId());
                            // leverage threadpool to cut down rule graph maintain latency
                            this.threadPoolService.getScheduledExecutor().execute(new Runnable() {
                                @Override
                                public void run() {
                                    long cur_time = System.nanoTime();
                                    if (flowMod instanceof OFFlowAdd) {
                                        addRuleNode(ruleNode);
                                    } else if (flowMod instanceof OFFlowDelete) {
                                        removeRuleNode(ruleNode, (flowMod instanceof OFFlowDeleteStrict));
                                    } else if (flowMod instanceof OFFlowModify) {
                                        modifyRuleNode(ruleNode, (flowMod instanceof OFFlowModifyStrict));
                                    } else {
                                        logger.error("receive messages that could not be handled by FADEController. message {}", flowMod);
                                    }
                                    logger.info("rule graph update for message {}, latency: {}ns", flowMod.getClass().getSimpleName(), System.nanoTime() - cur_time);
                                }
                            });
                        }
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e){
            logger.error("handling FlowMod message {} fails, exception: {}", msg, e);
        }
        return Command.CONTINUE;
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return false;
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    @Override
    public void switchAdded(DatapathId dpid) {
        try {
            this.ruleLock.writeLock().lock();
            try {
                this.rules.put(dpid, new Trie<>());
            } finally {
                this.ruleLock.writeLock().unlock();
            }

            this.inputRuleLock.writeLock().lock();
            try {
                this.inputMap.put(dpid, Sets.newHashSet());
            } finally {
                this.inputRuleLock.writeLock().unlock();
            }

            this.outputRuleLock.writeLock().lock();
            try {
                this.outputMap.put(dpid, Sets.newHashSet());
            } finally {
                this.outputRuleLock.writeLock().unlock();
            }

            this.droppingRuleLock.writeLock().lock();
            try {
                this.droppingMap.put(dpid, Sets.newHashSet());
            } finally {
                this.droppingRuleLock.writeLock().unlock();
            }
        } catch (Exception e) {
            logger.error("handle switchAdded message fail, switch: {}, exception: {}", dpid, e);
        }
    }

    @Override
    public void switchRemoved(DatapathId dpid) {
        try {
            this.ruleLock.writeLock().lock();
            try {
                Trie<IRuleNode, Object> ruleNodes = this.rules.remove(dpid);
                if (ruleNodes == null) {
                    logger.warn("datapath {} hasn't been detected", dpid);
                    return;
                }
                List<IRuleNode> swRules = ruleNodes.getNodes();
                this.iterateLock.writeLock().lock();
                try {
                    for (IRuleNode node : swRules) {
                    /* remove forward (->) links */
                        List<IRuleNode> nexts = Lists.newArrayList(this.nextRules.getOrDefault(node, Collections.emptySet()));
                        for (IRuleNode next : nexts) {
                            this.removeLink(node, next);
                        }
                    /* remove backward (<-) links */
                        List<IRuleNode> prevs = Lists.newArrayList(this.prevRules.getOrDefault(node, Collections.emptySet()));
                        for (IRuleNode prev : prevs) {
                            this.removeLink(prev, node);
                        }
                    }
                } finally {
                    this.iterateLock.writeLock().unlock();
                }

                this.outputRuleLock.writeLock().lock();
                try {
            /* remove output rules */
                    List<IRuleNode> outputs = Lists.newArrayList(this.outputMap.get(dpid));
                    if (outputs != null) {
                        for (IRuleNode output : outputs) {
                            this.removeOutputRule(output);
                        }
                    }
                } finally {
                    this.outputRuleLock.writeLock().unlock();
                }

                this.inputRuleLock.writeLock().lock();
                try {
            /* remove input rules */
                    List<IRuleNode> inputs = Lists.newArrayList(this.inputMap.get(dpid));
                    if (inputs != null) {
                        for (IRuleNode input : inputs) {
                            this.removeInputRule(input);
                        }
                    }
                } finally {
                    this.inputRuleLock.writeLock().unlock();
                }

                this.droppingRuleLock.writeLock().lock();
                try {
                /* remove dropping rules */
                    List<IRuleNode> droppings = Lists.newArrayList(this.droppingMap.get(dpid));
                    if (droppings != null) {
                        for (IRuleNode drop : droppings) {
                            this.removeDroppingRule(drop);
                        }
                    }
                } finally {
                    this.droppingRuleLock.writeLock().unlock();
                }
            } finally {
                this.ruleLock.writeLock().unlock();
            }
        } catch (Exception e){
            logger.error("handling switchRemoved message fails, switch: {}, exception: {}", dpid, e);
        }
    }

    @Override
    public void portAdded(Pair<DatapathId, OFPort> ofPort) {
        // noop
    }

    @Override
    public void portRemoved(Pair<DatapathId, OFPort> ofPort) {
        // noop
    }

    @Override
    public void linkAdded(Pair<DatapathId, OFPort> portSrc, Pair<DatapathId, OFPort> portDst) {
        try {
        /* re-calculate all rule links in which the rule output traffic to portSrc.getRight() */
            List<IRuleNode> affects = Lists.newArrayList();
            Multimap<IRuleNode, IRuleNode> newLinks = ArrayListMultimap.create();
            this.ruleLock.readLock().lock();
            try {
                if (!this.rules.containsKey(portSrc.getLeft())) {
                    return;
                }
                for (IRuleNode node : this.rules.get(portSrc.getLeft()).getNodes()) {
                    if (node.getOutPort() != null && node.getOutPort().equals(portSrc.getRight())) {
                        affects.add(node);
                    }
                }
                for (IRuleNode node : affects) {
                    Pair<DatapathId, OFPort> target = this.concreteTopologyService.linkTransfer(new ImmutablePair<>(node.getDatapathId(), node.getOutPort()));
                    if (target != null) {
                        Iterable<IRuleNode> nexts = this.getMatchedRuleNodes(node.getReallyOutputHS(), target, node.getMatchHS());
                        for (IRuleNode next : nexts) {
                            newLinks.put(node, next);
                        }
                    }
                }
            } finally {
                this.ruleLock.readLock().unlock();
            }
            this.iterateLock.writeLock().lock();
            try {
                for (Map.Entry<IRuleNode, IRuleNode> entry : newLinks.entries()) {
                    this.addLink(entry.getKey(), entry.getValue());
                }
            } finally {
                this.iterateLock.writeLock().unlock();
            }
        /* input rule */
            this.inputRuleLock.writeLock().lock();
            try {
                for (Map.Entry<IRuleNode, IRuleNode> entry : newLinks.entries()) {
                    this.removeInputRule(entry.getValue());
                }
            } finally {
                this.inputRuleLock.writeLock().unlock();
            }
        /* output rule */
            this.outputRuleLock.writeLock().lock();
            try {
                for (Map.Entry<IRuleNode, IRuleNode> entry : newLinks.entries()) {
                    this.removeOutputRule(entry.getKey());
                }
            } finally {
                this.outputRuleLock.writeLock().unlock();
            }
            logger.debug("link between {} -- {} added ", ToStringBuilder.reflectionToString(portSrc), ToStringBuilder.reflectionToString(portDst));
        } catch (Exception e) {
            logger.error("handling linkAdded fails, link {} -- {}, exception: {}",
                    new Object[]{ReflectionToStringBuilder.reflectionToString(portSrc), ReflectionToStringBuilder.reflectionToString(portDst), e});
        }
    }

    @Override
    public void linkRemoved(Pair<DatapathId, OFPort> portSrc, Pair<DatapathId, OFPort> portDst) {
        try {
        /* remove any rule link in which the out port of the first rule is equals to the portSrc.getRight() */
            List<IRuleNode> affects = Lists.newArrayList();
            Multimap<IRuleNode, IRuleNode> erasedLinks = ArrayListMultimap.create();
            try(AutoLock l = new AutoLock(this.ruleLock.readLock())){
                if (!this.rules.containsKey(portSrc.getLeft())) {
                    return;
                }
                for (IRuleNode node : this.rules.get(portSrc.getLeft()).getNodes()) {
                    if (node.getOutPort() != null && node.getOutPort().equals(portSrc.getRight())) {
                        affects.add(node);
                    }
                }
            }
            try(AutoLock l = new AutoLock(this.iterateLock.writeLock())){
                for (IRuleNode node : affects) {
                    if (this.nextRules.containsKey(node)) {
                        ArrayList<IRuleNode> nexts = Lists.newArrayList(this.nextRules.get(node));
                        for (IRuleNode next : nexts) {
                            this.removeLink(node, next);
                            erasedLinks.put(node, next);
                        }
                    }
                }
            }
            /* input rule */
            try(AutoLock l = new AutoLock(this.inputRuleLock.writeLock())){
                for (Map.Entry<IRuleNode, IRuleNode> entry : erasedLinks.entries()) {
                    if (!this.prevRules.containsKey(entry.getValue()) || this.prevRules.get(entry.getValue()).size() == 0) {
                        this.addInputRule(entry.getValue());
                    }
                }
            }
            /* output rule */
            try(AutoLock l = new AutoLock(this.outputRuleLock.writeLock())){
                for (Map.Entry<IRuleNode, IRuleNode> entry : erasedLinks.entries()) {
                    if (!this.nextRules.containsKey(entry.getKey()) || this.nextRules.get(entry.getKey()).size() == 0) {
                        this.addOutputRule(entry.getKey());
                    }
                }
            }
            logger.debug("link between {} -- {} is removed", ToStringBuilder.reflectionToString(portSrc), ToStringBuilder.reflectionToString(portDst));
        } catch (Exception e){
            logger.error("handling linkRemoved message fails, link {} -- {}, exception: {}",
                    new Object[]{ReflectionToStringBuilder.reflectionToString(portSrc), ReflectionToStringBuilder.reflectionToString(portDst), e});
        }
    }

    @Override
    public boolean addRuleNode(IRuleNode ruleNode) {
        if(!this.insertNodeToSwitch(ruleNode)){
            logger.debug("cannot install flow rule: {}", ruleNode);
            return false;
        }
        logger.debug("rule node added, rule node = {}", ruleNode);
        /* notify rule add */
        this.listeners.stream().forEach(listener -> listener.ruleAdded(ruleNode));
        this.iterateLock.writeLock().lock();
        try{
            for(IRuleNode node : ruleNode.getAffectedRuleNodes()){
                /* update links from affected node */
                if(this.nextRules.containsKey(node)) {
                    for (IRuleNode next : Lists.newArrayList(this.nextRules.get(node))) {
                        if(node.getReallyOutputHS().copyIntersect(next.getReallyMatchHS()).isEmpty()){
                            this.removeLink(node, next);
                        }
                    }
                }
                /* update links to affected node */
                if (this.prevRules.containsKey(node)) {
                    for(IRuleNode pre : Lists.newArrayList(this.prevRules.get(node))){
                        if(pre.getReallyOutputHS().copyIntersect(node.getReallyMatchHS()).isEmpty()){
                            this.removeLink(pre, node);
                        }
                    }
                }
            }
        } finally {
            this.iterateLock.writeLock().unlock();
        }
        /* create links from current node */
        Pair<DatapathId, OFPort> targetPort = this.concreteTopologyService.linkTransfer(new ImmutablePair<>(ruleNode.getDatapathId(), ruleNode.getOutPort()));
        if(targetPort != null) {
            Iterable<IRuleNode> nexts = this.getMatchedRuleNodes(ruleNode.getReallyMatchHS(), targetPort, ruleNode.getMatchHS());
            this.iterateLock.writeLock().lock();
            try {
                for (IRuleNode next : nexts) {
                    this.addLink(ruleNode, next);
                }
            } finally {
                this.iterateLock.writeLock().unlock();
            }
        }
        /* create links to current node (the complexity is very high, and can be optimized by applying reverse rewrite) */
        Set<DatapathId> upstream = this.concreteTopologyService.getUpstreamSwitch(ruleNode.getDatapathId(), ruleNode.getInPort());
        List<IRuleNode> prevs = Lists.newArrayList();
        for(DatapathId dpid : upstream){
            for(IRuleNode pre : this.getSwitchRules(dpid)){
                Pair<DatapathId, OFPort> target = this.concreteTopologyService.linkTransfer(new ImmutablePair<>(dpid, pre.getOutPort()));
                if(target != null && target.getLeft().equals(ruleNode.getDatapathId())) {
                    if (ruleNode.getInPort() == null || target.getRight().equals(ruleNode.getInPort())) {
                        if (!pre.getReallyMatchHS().copyIntersect(ruleNode.getReallyMatchHS()).isEmpty()) {
                            prevs.add(pre);
                        }
                    }
                }
            }
        }
        /* we add the links together (reduce the interval which write lock is applied)*/
        this.iterateLock.writeLock().lock();
        try{
            for(IRuleNode prev : prevs){
                this.addLink(prev, ruleNode);
            }
        } finally {
            this.iterateLock.writeLock().unlock();
        }
        /* initialize rule priority */
        this.selectPriority.put(ruleNode, 0L);
        /* update output rule (current rule?, prev?) */
        if(!this.nextRules.containsKey(ruleNode) || this.nextRules.get(ruleNode).size() == 0){
            this.outputRuleLock.writeLock().lock();
            try{
                this.addOutputRule(ruleNode);
                for(IRuleNode prev : this.prevRules.getOrDefault(ruleNode, Collections.emptySet())){
                    this.removeOutputRule(prev);
                }
            } finally {
                this.outputRuleLock.writeLock().unlock();
            }
        }
        /* update input rule (current rule?, next? affected?) */
        try{
            this.inputRuleLock.writeLock().lock();
            if(!this.prevRules.containsKey(ruleNode) || this.prevRules.get(ruleNode).size() == 0){
                    this.addInputRule(ruleNode);
            }
            for(IRuleNode node : Lists.newArrayList(this.nextRules.getOrDefault(ruleNode, Collections.emptySet()))){
                this.removeInputRule(node);
            }
            for(IRuleNode node : ruleNode.getAffectedRuleNodes()){
                if(!this.prevRules.containsKey(node) || this.prevRules.get(node).size() == 0){
                    this.addInputRule(node);
                }
            }
        } finally {
            this.inputRuleLock.writeLock().unlock();
        }
        /* update dropping rule (current rule?) */
        if(ruleNode.getOutPort() == null){
            this.droppingRuleLock.writeLock().lock();
            try{
                this.addDroppingRule(ruleNode);
            } finally {
                this.droppingRuleLock.writeLock().unlock();
            }
        }
        this.dumpRuleGraph();
        return true;
    }

    private boolean removeRuleNodeStrict(IRuleNode ruleNode){
        IRuleNode ref = this.removeNodeFromSwitch(ruleNode);
        if(ref == null) {
            logger.error("cannot find rule {} in the switch {}", ruleNode, ruleNode.getDatapathId());
            return false;
        }
        Iterable<IRuleNode> prevs, nexts, affects;
        this.iterateLock.writeLock().lock();
        try {
            /* remove links to/from current rule */
            prevs = Lists.newArrayList(this.prevRules.get(ruleNode));
            if(prevs != null){
                for(IRuleNode prev : prevs){
                    this.removeLink(prev, ref);
                }
            }
            nexts = Lists.newArrayList(this.nextRules.get(ruleNode));
            if(nexts != null){
                for(IRuleNode next : nexts){
                    this.removeLink(ref, next);
                }
            }
            /* re-calculate links for "affected set" */
            affects = ref.getAffectedRuleNodes();
            for(IRuleNode affected : affects){
                if(prevs != null){
                    for(IRuleNode prev : prevs){
                        if(!prev.getReallyOutputHS().copyIntersect(affected.getReallyMatchHS()).isEmpty()){
                            if(!isLinkExist(prev, affected)) {
                                this.addLink(prev, affected);
                            }
                        }
                    }
                }
                Pair<DatapathId, OFPort> target = this.concreteTopologyService.linkTransfer(new ImmutablePair<>(affected.getDatapathId(), affected.getOutPort()));
                if(target != null){
                    List<IRuleNode> newNexts = this.getMatchedRuleNodes(affected.getReallyOutputHS(), target, affected.getMatchHS());
                    for(IRuleNode newNext : newNexts){
                        if(!this.isLinkExist(affected, newNext)) {
                            this.addLink(affected, newNext);
                        }
                    }
                }
            }
        } finally {
            this.iterateLock.writeLock().unlock();
        }
        logger.debug("rule node removed, rule node = {}", ruleNode);
        /* notify rule delete */
        this.listeners.stream().forEach(listener -> listener.ruleRemoved(ref));
        /* priority */
        this.selectPriority.remove(ref);
        /* output rule (current? prev?) */
        this.outputRuleLock.writeLock().lock();
        try {
            this.removeOutputRule(ref);
            if(prevs != null) {
                for (IRuleNode prev : prevs) {
                    if (!this.nextRules.containsKey(prev) || this.nextRules.get(prev).size() == 0) {
                        this.addOutputRule(prev);
                    }
                }
            }
        } finally {
            this.outputRuleLock.writeLock().unlock();
        }
        /* input rule (current? nexts? affected?) */
        this.inputRuleLock.writeLock().lock();
        try {
            this.removeInputRule(ref);
            if(nexts != null) {
                for (IRuleNode next : nexts) {
                    if(!this.prevRules.containsKey(next) || this.prevRules.get(next).size() == 0){
                        this.addInputRule(next);
                    }
                }
            }
            for(IRuleNode affect : affects){
                if(!this.prevRules.containsKey(affect) || this.prevRules.get(affect).size() == 0){
                    this.addInputRule(affect);
                }
            }
        } finally {
            this.inputRuleLock.writeLock().unlock();
        }
        /* dropping rule */
        this.removeDroppingRule(ref);
        return true;
    }

    @Override
    public boolean removeRuleNode(IRuleNode ruleNode, boolean strict) {
        List<IRuleNode> refs = null;
        if(!strict) {
            Trie<IRuleNode, Object> rules = this.rules.get(ruleNode.getDatapathId());
            if(rules != null) {
                refs = this.rules.get(ruleNode.getDatapathId()).getIntersectedRules(ruleNode);
            } else {
                return false;
            }
        } else {
            refs = Collections.singletonList(ruleNode);
        }
        boolean result = true;
        for(IRuleNode ref : refs){
            result = (result && this.removeRuleNodeStrict(ref));
        }
        this.dumpRuleGraph();
        return result;
    }

    @Override
    public boolean modifyRuleNode(IRuleNode ruleNode, boolean strict) {
        // Note that the hash value of the object hasn't been changed, we can use it to index object still
        List<IRuleNode> refs;
        this.ruleLock.readLock().lock();
        try {
            if (!this.rules.containsKey(ruleNode.getDatapathId())) {
                return false;
            }
            refs = this.rules.get(ruleNode.getDatapathId()).getIntersectedRules(ruleNode);
        } finally {
            this.ruleLock.readLock().unlock();
        }

        if(strict && refs.size() != 1) {
            logger.warn("cannot find exactly reference rule on switch {}. However, we find {} references.", ruleNode.getDatapathId(), refs.size());
            return false;
        }
        // remove old rules and add new rules (in parallel)
        refs.stream().forEach(this::removeRuleNodeStrict);
        this.addRuleNode(ruleNode);
        /* we should have notified the rule modification. However, as the implementation, we don't notify this event */
        return false;
    }

    @Override
    public boolean isLinkExist(IRuleNode pre, IRuleNode nxt) {
        this.iterateLock.readLock().lock();
        try {
            return this.nextRules.containsKey(pre) && this.nextRules.get(pre).contains(nxt);
        } finally {
            this.iterateLock.readLock().unlock();
        }
    }

    @Override
    public List<IRuleNode> getNextHops(IRuleNode ruleNode) {
        this.iterateLock.readLock().lock();
        try{
            if(this.nextRules.containsKey(ruleNode)){
                return Lists.newArrayList(this.nextRules.get(ruleNode));
            } else {
                return Collections.emptyList();
            }
        } finally {
            this.iterateLock.readLock().unlock();
        }
    }

    @Override
    public List<IRuleNode> getPrevHops(IRuleNode ruleNode) {
        this.iterateLock.readLock().lock();
        try{
            if(this.prevRules.containsKey(ruleNode)){
                return Lists.newArrayList(this.prevRules.get(ruleNode));
            } else {
                return Collections.emptyList();
            }
        } finally {
            this.iterateLock.readLock().unlock();
        }
    }

    @Override
    public List<IRuleNode> getSwitchRules(DatapathId dpid) {
        this.ruleLock.readLock().lock();
        try {
            if (this.rules.containsKey(dpid)) {
                return this.rules.get(dpid).getNodes();
            } else {
                return Collections.emptyList();
            }
        } finally {
            this.ruleLock.readLock().unlock();
        }
    }

    /**
     * Retrieving input rules from rule graph
     * @implNote you SHOULD reset the priorities of input rules if you have used them
     * @param limit the largest number of input nodes that this function returns, -1 means infinity
     * @return a list of input rules
     */
    @Override
    public List<IRuleNode> getInputRuleNodes(int limit) {
        this.inputRuleLock.readLock().lock();
        try {
            if(-1 == limit){
                return Lists.newArrayList(this.inputRules);
            }
            int maxSize = this.inputRules.size();
            int fetchSize = maxSize >= limit ? limit : maxSize;
            List<IRuleNode> result = new ArrayList<>(fetchSize);
            Iterator<IRuleNode> it = this.inputRules.iterator();
            while(fetchSize-- > 0 && it.hasNext()){
                result.add(it.next());
            }
            return result;
        } finally {
            this.inputRuleLock.readLock().unlock();
        }
    }

    /**
     * Retrieving output rule nodes from rule graph
     * @implNote you SHOULD reset the priorities of rule nodes if you have used them
     * @param limit the largest number of output nodes that this function returns, -1 means infinity
     * @return a list of output rule nodes
     */
    @Override
    public List<IRuleNode> getOutputRuleNodes(int limit) {
        this.outputRuleLock.readLock().lock();
        try{
            if(-1 == limit){
                return Lists.newArrayList(this.outputRules);
            }
            int fetchSize = this.outputRules.size() >= limit ? limit : this.outputRules.size();
            List<IRuleNode> result = Lists.newArrayList();
            Iterator<IRuleNode> it = this.outputRules.iterator();
            while(fetchSize-- > 0 && it.hasNext()){
                result.add(it.next());
            }
            return result;
        } finally {
            this.outputRuleLock.readLock().unlock();
        }
    }

    @Override
    public List<IRuleNode> getDroppingRuleNodes(int limit) {
        this.droppingRuleLock.readLock().unlock();
        try{
            if(-1 == limit){
                return Lists.newArrayList(this.droppingRules);
            }
            int fetchSize = this.droppingRules.size() >= limit ? limit : this.droppingRules.size();
            List<IRuleNode> result = Lists.newArrayList();
            Iterator<IRuleNode> it = this.droppingRules.iterator();
            while(fetchSize-- > 0 && it.hasNext() ){
                result.add(it.next());
            }
            return result;
        } finally {
            this.droppingRuleLock.readLock().unlock();
        }
    }

    @Override
    public Long getRuleNodePriority(IRuleNode ruleNode) {
        if(!this.selectPriority.containsKey(ruleNode)) {
            logger.warn("cannot find priority for rule node {}, initializing to 0", ruleNode);
            this.selectPriority.put(ruleNode, 0L);
        }
        return this.selectPriority.getOrDefault(ruleNode, 0L);
    }

    @Override
    public Long setRuleNodePriority(IRuleNode ruleNode, long priority) {
        Long oldVal = this.selectPriority.put(ruleNode, priority);
        /* invalidate priority queues */
        this.inputRuleLock.writeLock().lock();
        try {
            if (this.inputRules.contains(ruleNode)) { /* priority queue has an O( log(n) ) search complexity */
                this.inputRules.remove(ruleNode);
                this.inputRules.add(ruleNode);
                return oldVal;
            }
        } finally {
            this.inputRuleLock.writeLock().unlock();
        }

        this.outputRuleLock.writeLock().lock();
        try {
            if (this.outputRules.contains(ruleNode)) {
                this.outputRules.remove(ruleNode);
                this.outputRules.add(ruleNode);
                return oldVal;
            }
        } finally {
            this.outputRuleLock.writeLock().unlock();
        }

        this.droppingRuleLock.writeLock().lock();
        try{
            if(this.droppingRules.contains(ruleNode)){
                this.droppingRules.remove(ruleNode);
                this.droppingRules.add(ruleNode);
                return oldVal;
            }
        } finally {
            this.droppingRuleLock.writeLock().unlock();
        }
        return oldVal;
    }

    @Override
    public boolean isDroppingRuleNode(IRuleNode ruleNode) {
        this.droppingRuleLock.readLock().lock();
        try{
            return this.droppingRules.contains(ruleNode);
        } finally {
            this.droppingRuleLock.writeLock().unlock();
        }
    }

    @Override
    public boolean addListener(IRuleGraphListener listener) {
        return this.listeners.add(listener);
    }

    @Override
    public boolean removeListener(IRuleGraphListener listener) {
        return this.listeners.remove(listener);
    }

    /**
     * Insert a rule node to its given switch
     * @param node the given rule node
     * @return true if successfully inserted
     */
    protected boolean insertNodeToSwitch(IRuleNode node){
        this.ruleLock.writeLock().lock();
        try{
            if(!this.rules.containsKey(node.getDatapathId())){
                this.rules.put(node.getDatapathId(), new Trie<>());
            }
            Trie<IRuleNode, Object> rs = this.rules.get(node.getDatapathId());
            List<IRuleNode> intersects = rs.getIntersectedRules(node);
            if(!rs.insert(node)){
                return false;
            }
            for(IRuleNode ref : intersects){
                if(ref.getPriority() > node.getPriority()){
                    ref.addAffectedRuleNode(node);
                    node.addDependOnRuleNode(ref);
                } else if(ref.getPriority() < node.getPriority()) {
                    ref.addDependOnRuleNode(node);
                    node.addAffectedRuleNode(ref);
                } else { /* unspecified action... */
                    rs.remove(node); /* undo insertion */
                    logger.warn("Insertion failed: two rule nodes with the same priority have intersection: {}, {}", node, ref);
                    return false;
                }
            }
        } finally {
            this.ruleLock.writeLock().unlock();
        }
        return true;
    }

    /**
     * Removing a node from a switch
     * @param node the given rule node. The node itself may not included in the switch. However, there must be a same node in the switch (equals function)
     * @return the removed from the switch. It could be another object different with <code>node</code>
     */
    protected IRuleNode removeNodeFromSwitch(IRuleNode node){
        IRuleNode ref; /* find the original rule */
        this.ruleLock.writeLock().lock();
        try{
            if(!this.rules.containsKey(node.getDatapathId())){
                return null;
            }
            Trie<IRuleNode, Object> swRules = this.rules.get(node.getDatapathId());
            ref = swRules.getReference(node);
            if(ref == null) {
                return null;
            }
            if(!swRules.remove(node)){
                return null;
            }
            for(IRuleNode depend : ref.getDependOnRuleNodes()){
                depend.removeAffectedRuleNode(node);
            }
            for(IRuleNode affect : ref.getAffectedRuleNodes()){
                affect.removeDependOnRuleNode(node);
            }
        } finally {
            this.ruleLock.writeLock().unlock();
        }
        return ref;
    }

    /**
     * Retrieve all rule nodes that can match an input header space on a given switch.
     * As an optimization, a ternary array that specifies the the minimum superset of {@code hs} can be provided
     * (e.g., {@code canMatch} field of {@link net.floodlightcontroller.applications.appmodule.rulegraph.rule.FlowRuleNode}),
     * which provides quick location of a small set of rules that may intersect of the provided header space.
     * @param hs the header space
     * @param inPort the input port and switch
     * @param maxMatch optional, providing the minimum superset of {@code hs}
     * @return a list contains all rule nodes that would match the header space on the given switch
     */
    protected List<IRuleNode> getMatchedRuleNodes(HeaderSpace hs, Pair<DatapathId, OFPort> inPort, TernaryArray maxMatch){
        this.ruleLock.readLock().lock();
        try {
            Trie<IRuleNode, Object> rules = this.rules.get(inPort.getLeft());
            List<IRuleNode> possibleMatch = null;
            if (maxMatch != null) {
                possibleMatch = rules.getIntersectedRules(new TernaryArrayTrieNodeRetrievable(inPort.getRight(), maxMatch));
            } else {
                possibleMatch = rules.getNodes();
            }
            List<IRuleNode> result = Lists.newArrayList();
            for (IRuleNode node : possibleMatch) {
                if (node.getInPort() == null || inPort.getRight() == null ||
                        node.getInPort().equals(inPort.getRight()) || node.getInPort().equals(OFPort.ANY) ||
                        inPort.getRight().equals(OFPort.ANY)) { /* port match */
                    if (!node.getReallyMatchHS().copyIntersect(hs).isEmpty()) {
                        result.add(node);
                    }
                }
            }
            return result;
        } finally {
            this.ruleLock.readLock().unlock();
        }
    }

    /**
     * Implements an comparator for rule nodes
     */
    private class RuleNodeComparator implements Comparator<IRuleNode> {
        @Override
        public int compare(IRuleNode left, IRuleNode right) {
            Long prioLeft = selectPriority.getOrDefault(left, 0L);
            Long prioRight = selectPriority.getOrDefault(right, 0L);
            return prioLeft.longValue() > prioRight.longValue() ? 1 : (prioLeft.longValue() < prioRight.longValue() ? -1 : 0);
        }
    }

    /**
     * add new link to the rule graph and update the prevHops and nextHops.
     * Write lock must be acquired first
     * @param prev the previous rule node
     * @param next the next rule node
     * @return true if a new link is added
     */
    protected boolean addLink(IRuleNode prev, IRuleNode next){
        assert LockUtil.checkLocked(this.iterateLock.writeLock());
        if(!this.nextRules.containsKey(prev)){
            this.nextRules.put(prev, Sets.newHashSet());
        }
        if(this.nextRules.get(prev).contains(next)){
            logger.warn("the unidirectional link {} -> {} has been existed", prev, next);
            return false;
        }
        if(!this.prevRules.containsKey(next)){
            this.prevRules.put(next, Sets.newHashSet());
        }
        if(this.prevRules.get(next).contains(prev)){
            logger.warn("the unidirectional link {} <- {} has been existed", prev, next);
            return false;
        }
        this.nextRules.get(prev).add(next);
        this.prevRules.get(next).add(prev);
        logger.debug("the link {} -- {} has been added", prev, next);
        this.listeners.forEach(listener -> listener.linkAdded(prev, next));
        return true;
    }

    /**
     * remove link between two rule nodes
     * @param prev the previous node
     * @param next the next hop
     * @return true if the link is removed
     */
    protected boolean removeLink(IRuleNode prev, IRuleNode next){
        assert LockUtil.checkLocked(this.iterateLock.writeLock());
        if(!this.nextRules.containsKey(prev)){
            logger.warn("the rule {} hasn't any next hop rule", prev);
            return false;
        }
        if(!this.nextRules.get(prev).remove(next)){
            logger.warn("the link {} -> {} doesn't exist", prev, next);
            return false;
        }
        if(!this.prevRules.containsKey(next)){
            logger.warn("the rule {} doesn't have any previous hop rules", next);
            return false;
        }
        if(!this.prevRules.get(next).remove(prev)){
            logger.warn("the link {} <- {} doesn't exist", prev, next);
            return false;
        }
        logger.debug("the link {} -- {} has been removed", prev, next);
        this.listeners.forEach(listener -> listener.linkRemoved(prev, next));
        return true;
    }

    private void addInputRule(IRuleNode node){
        assert LockUtil.checkLocked(this.inputRuleLock.writeLock());
        if(!this.inputMap.containsKey(node.getDatapathId())){
            this.inputMap.put(node.getDatapathId(), Sets.newHashSet());
        }
        this.inputMap.get(node.getDatapathId()).add(node);
        this.inputRules.add(node);
    }

    private void removeInputRule(IRuleNode node){
        assert LockUtil.checkLocked(this.inputRuleLock.writeLock());
        if(!this.inputRules.remove(node)){
            return;
        }
        if(this.inputMap.containsKey(node.getDatapathId())){
            this.inputMap.get(node.getDatapathId()).remove(node);
        }
    }

    private void addOutputRule(IRuleNode node){
        assert LockUtil.checkLocked(this.outputRuleLock.writeLock());
        if(!this.outputMap.containsKey(node.getDatapathId())){
            this.outputMap.put(node.getDatapathId(), Sets.newHashSet());
        }
        this.outputMap.get(node.getDatapathId()).add(node);
        this.outputRules.add(node);
    }

    private void removeOutputRule(IRuleNode node){
        assert LockUtil.checkLocked(this.outputRuleLock.writeLock());
        if(!this.outputRules.remove(node)){
            return;
        }
        if(this.outputMap.containsKey(node.getDatapathId())){
           this.outputMap.get(node.getDatapathId()).remove(node);
        }
    }

    private void addDroppingRule(IRuleNode node){
        assert LockUtil.checkLocked(this.droppingRuleLock.writeLock());
        if(!this.droppingMap.containsKey(node.getDatapathId())){
            this.droppingMap.put(node.getDatapathId(), Sets.newHashSet());
        }
        this.droppingMap.get(node.getDatapathId()).add(node);
        this.droppingRules.add(node);
    }

    private void removeDroppingRule(IRuleNode node){
        assert LockUtil.checkLocked(this.droppingRuleLock.writeLock());
        if(!this.droppingRules.remove(node)){
            return;
        }
        if(this.droppingMap.containsKey(node.getDatapathId())){
            this.droppingMap.get(node.getDatapathId()).remove(node);
        }
    }

    private void dumpRuleGraph( ) {
        logger.debug("----- RULE GRAPH INFO -----");
        logger.debug(this.toString());
        logger.debug("----- RULE GRAPH INFO END -----");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        this.nextRules.forEach((rn, nxts) -> {
            sb.append(rn);
            sb.append(" -> { ");
            nxts.forEach( nxt -> sb.append(nxt).append(", "));
            sb.append(" }").append('\n');
        });
        return sb.toString();
    }
}
