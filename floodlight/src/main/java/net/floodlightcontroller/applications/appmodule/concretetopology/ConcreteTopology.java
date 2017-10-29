package net.floodlightcontroller.applications.appmodule.concretetopology;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery;
import net.floodlightcontroller.linkdiscovery.ILinkDiscovery.*;
import net.floodlightcontroller.topology.ITopologyListener;
import net.floodlightcontroller.topology.ITopologyService;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.U64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * This class implements a more concrete "TopologyService".
 * Compare with the one comes with Floodlight, it
 * <ol>
 * <li>masks redundant events</li>
 * <li>provides more application specific utilities</li>
 * </ol>
 *
 * event dependency:
 *      link up --> port up --> switch up
 *      link down <-- port down <-- switch down
 *      xx down --> xx up
 * @author chunhui (chunhui.pang@outlook.com)
 * @see net.floodlightcontroller.topology.TopologyInstance
 */
public class ConcreteTopology implements IConcreteTopologyService, IFloodlightModule, ITopologyListener {

    private static final Logger logger = LoggerFactory.getLogger(ConcreteTopology.class);
    /* data, concurrently accessible */
    private Set<DatapathId> dpids;
    private Map<DatapathId, Set<OFPort>> swPorts;
    private Map<Pair<DatapathId, OFPort>, Pair<DatapathId, OFPort>> links;  /* unidirectional */
    private Map<Pair<DatapathId, OFPort>, Pair<DatapathId, OFPort>> reverseLinks; /* unidirectional */

    /* caches, concurrently accessible */
    private Set<Pair<DatapathId, OFPort>> outPorts;
    private Map<DatapathId, Set<DatapathId>> physicalLinks;   /* unidirectional */

    private class EventInfo {
        private static final long EXPIRE_DURATION = 3000; // 3000ms
        private LDUpdate update;
        private long happenTime;
        private int numOfDependency;
        public EventInfo(LDUpdate update, int numOfDependency) {
            this.update = update;
            this.happenTime = Calendar.getInstance().getTimeInMillis();
            this.numOfDependency = numOfDependency;
        }
        public EventInfo(LDUpdate update){ this(update, 1); }
        public boolean hasExpired() { return Calendar.getInstance().getTimeInMillis() - happenTime > EXPIRE_DURATION; }
        public boolean couldApply() { return this.numOfDependency == 1; }
        public LDUpdate getUpdate() { return this.update; }
        public void decreaseDependency() { this.numOfDependency--; }

        @Override
        public String toString() {
            return "EventInfo{" + "update=" + update + ", happenTime=" + happenTime + ", numOfDependency=" + numOfDependency + '}';
        }
    }
    /* rewrite equals and hashCode function */
    private class LDUpdateWrapper extends LDUpdate {
        public LDUpdateWrapper(LDUpdate old) {
            super(old);
        }
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            LDUpdateWrapper that = (LDUpdateWrapper) o;
            return new EqualsBuilder().append(src, that.src).append(srcPort, that.srcPort).append(dst, that.dst)
                    .append(dstPort, that.dstPort).append(srcType, that.srcType)
                    .append(latency, that.latency).append(type, that.type).append(operation, that.operation).isEquals();
        }
        @Override
        public int hashCode() {
            return new HashCodeBuilder(17, 37).append(src).append(srcPort).append(dst)
                    .append(dstPort).append(srcType).append(latency).append(type)
                    .append(operation).toHashCode();
        }
    }
    private Map<LDUpdateWrapper, Set<EventInfo>> eventDependency;
    /* listeners, concurrently accessible */
    private Set<IConcreteTopologyListener> listeners;

    /* the raw topology */
    private ITopologyService rawTopologyService;
    private ReadWriteLock rwLock;

    @Override
    public Set<OFPort> getPorts(DatapathId dpid) {
        this.rwLock.readLock().lock();
        try {
            if (this.swPorts.containsKey(dpid)) {
                return swPorts.get(dpid);
            } else {
                return Collections.emptySet();
            }
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean isOuterPort(Pair<DatapathId, OFPort> ofPort) {
        try {
            this.rwLock.readLock().lock();
            return outPorts.contains(ofPort);
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    @Override
    public Set<Pair<DatapathId, OFPort>> getOuterPorts() {
        this.rwLock.readLock().lock();
        try {
            return this.outPorts;
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    @Override
    public Pair<DatapathId, OFPort> linkTransfer(Pair<DatapathId, OFPort> ofPort) {
        this.rwLock.readLock().lock();
        try {
            /* our link is maintained "unidirectional" */
            if (this.links.containsKey(ofPort)) {
                return this.links.get(ofPort);
            } else {
                return null;
            }
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    @Override
    public Set<DatapathId> getDownstreamSwitch(DatapathId dpid) {
        this.rwLock.readLock().lock();
        try {
            if (this.dpids.contains(dpid)) {
                if (this.physicalLinks.containsKey(dpid)) {
                    return this.physicalLinks.get(dpid);
                } else {
                    return Collections.emptySet();
                }
            } else {
                return null;
            }
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    @Override
    public Set<DatapathId> getUpstreamSwitch(DatapathId dpid, OFPort ofPort) {
        this.rwLock.readLock().lock();
        try {
            if (!this.swPorts.containsKey(dpid)) {
                return Collections.emptySet();
            }
            if (ofPort != null && !this.swPorts.get(dpid).contains(ofPort)) {
                return Collections.emptySet();
            }
            if (ofPort != null) {
                if (this.reverseLinks.containsKey(new ImmutablePair<>(dpid, ofPort))) {
                    return Collections.singleton(this.reverseLinks.get(new ImmutablePair<>(dpid, ofPort)).getKey());
                } else {
                    return Collections.emptySet();
                }
            }
            Set<OFPort> ports = this.swPorts.get(ofPort);
            if(ports == null || ports.size() == 0){
                return Collections.emptySet();
            }
            Set<DatapathId> result = Sets.newHashSet();
            for (OFPort port : ports) {
                if (this.reverseLinks.containsKey(new ImmutablePair<>(dpid, port))) {
                    result.add(this.reverseLinks.get(new ImmutablePair<>(dpid, port)).getKey());
                }
            }
            return result;
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    @Override
    public Pair<OFPort, OFPort> getLink(DatapathId left, DatapathId right) {
        this.rwLock.readLock().lock();
        try{
            if(this.physicalLinks.getOrDefault(left, Collections.emptySet()).contains(right)) {
                Set<OFPort> ports = this.swPorts.get(left);
                for (OFPort port : ports) {
                    Pair<DatapathId, OFPort> nxtPort = this.links.get(new ImmutablePair<>(left, port));
                    if(nxtPort != null && nxtPort.getLeft().equals(right)) {
                        return new ImmutablePair<>(port, nxtPort.getRight());
                    }
                }
            }
            return null;
        } finally {
            this.rwLock.readLock().unlock();
        }
    }

    @Override
    public boolean addListener(IConcreteTopologyListener listener) {
        return this.listeners.add(listener);
    }

    @Override
    public boolean removeListener(IConcreteTopologyListener listener) {
        return this.listeners.remove(listener);
    }

    @Override
    public void dump() {
        logger.debug("----- TOPOLOGY INFO -----");
        StringBuilder sb = new StringBuilder();
        links.forEach((src, dst) -> {
            sb.append(src.getLeft()).append(':').append(src.getRight());
            sb.append(" --- ");
            sb.append(dst.getLeft()).append(':').append(dst.getRight());
            sb.append('\n');
        });
        logger.debug(sb.toString());
        logger.debug("----- TOPOLOGY INFO END -----");
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        /* implement IConcreteTopologyService */
        return Collections.singletonList(IConcreteTopologyService.class);
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return Collections.singletonMap(IConcreteTopologyService.class, this);
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        return Collections.singletonList(ITopologyService.class);
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        this.rwLock = new ReentrantReadWriteLock();
        this.dpids = Sets.newHashSet();
        this.swPorts = Maps.newHashMap();
        this.links = Maps.newHashMap();
        this.reverseLinks = Maps.newHashMap();
        this.outPorts = Sets.newHashSet();
        this.physicalLinks = Maps.newHashMap();
        this.eventDependency = Maps.newHashMap();
        /* read > write */
        this.listeners = Sets.newCopyOnWriteArraySet();

        this.rawTopologyService = context.getServiceImpl(ITopologyService.class);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        this.rawTopologyService.addListener(this);
        logger.info("concrete topology module started :)");
    }

    @Override
    public void topologyChanged(List<ILinkDiscovery.LDUpdate> linkUpdates) {
        this.rwLock.writeLock().lock();
        LDUpdate current = null;
        try {
            for(LDUpdate update : linkUpdates) {
                current = update;
                this.topologyChanged(update);
            }
        } catch(Exception e){
            logger.error("handling topology change failure. message: {}, exception: {}", current, e);
        } finally {
            this.rwLock.writeLock().unlock();
        }
    }

    /**
     * do none for network down messages
     */
    private void topologyChanged(ILinkDiscovery.LDUpdate update) {
        /* here, we ignore the link type */
        logger.debug("recv topology event {}", update);
        switch (update.getOperation()) {
            case SWITCH_UPDATED:
                this.switchAdded(update.getSrc(), update);
                break;
            case PORT_UP:
                this.portAdded(Pair.of(update.getSrc(), update.getSrcPort()), update);
                break;
            case LINK_UPDATED:
                this.linkAdded(Pair.of(update.getSrc(), update.getSrcPort()), Pair.of(update.getDst(), update.getDstPort()), update);
                break;
            case SWITCH_REMOVED:
                // this.switchRemoved(update.getSrc(), update);
                break;
            case PORT_DOWN:
                // this.portRemoved(Pair.of(update.getSrc(), update.getSrcPort()), update);
                break;
            case LINK_REMOVED:
                // this.linkRemoved(Pair.of(update.getSrc(), update.getSrcPort()), Pair.of(update.getDst(), update.getDstPort()), update);
                break;
            default:
                logger.warn("receive unexpected type of topology update message: {}", ReflectionToStringBuilder.toString(update));
                break;
        }
        Set<EventInfo> eventInfos = this.eventDependency.remove(new LDUpdateWrapper(update));
        if(eventInfos != null) {
            for (EventInfo eventInfo : eventInfos) {
                if (!eventInfo.hasExpired()) {
                    if (eventInfo.couldApply()) {
                        this.topologyChanged(eventInfo.getUpdate());
                    } else {
                        eventInfo.decreaseDependency();
                    }
                }
            }
        }
        logger.debug("event dependencies: {}", this.eventDependency);
        this.dump();
    }

    private void switchAdded(DatapathId dpid, LDUpdate update) {
        if (this.dpids.add(dpid)) {
            for (IConcreteTopologyListener listener : this.listeners) {
                listener.switchAdded(dpid);
            }
            logger.debug("switch added, dpid={}", dpid);
        } else {
            logger.debug("switch {} has been discovered previously.", dpid);
        }
    }

    /**
     * refuse to remove switches which has any ports or links.
     * Dependency: port down
     * @param dpid the switch to be removed
     */
    private void switchRemoved(DatapathId dpid, LDUpdate update) {
        if(!this.dpids.contains(dpid)){
            logger.warn("the switch {} has been removed.", dpid);
            return;
        }

        List<LDUpdate> expectedUpdates = Lists.newArrayList();
        if (this.swPorts.containsKey(dpid) && this.swPorts.get(dpid).size() != 0) {
            for (OFPort port : this.swPorts.get(dpid)) {
                expectedUpdates.add(new LDUpdate(dpid, port, ILinkDiscovery.UpdateOperation.PORT_DOWN));
            }
            logger.debug("there are {} ports left, delaying the remove operation of switch {}", this.swPorts.get(dpid).size(), dpid);
        }
        if(expectedUpdates.size() != 0){
            this.addDependency(update, expectedUpdates);
            return;
        }
        logger.debug("switch removed, dpid = {}", dpid);
        this.dpids.remove(dpid);
        this.swPorts.remove(dpid);
        for (IConcreteTopologyListener listener : this.listeners) {
            listener.switchRemoved(dpid);
        }
    }

    /**
     * event dependency: switch added
     */
    private void portAdded(Pair<DatapathId, OFPort> port, LDUpdate update) {
        if (this.swPorts.containsKey(port.getLeft()) && this.swPorts.get(port.getLeft()).contains(port.getRight())) {
            logger.warn("port {} has been discovered previously", ReflectionToStringBuilder.toString(port));
            return;
        }

        List<LDUpdate> expectedUpdates = Lists.newArrayList();
        if(!this.dpids.contains(port.getLeft())){
            expectedUpdates.add(new LDUpdate(port.getLeft(), SwitchType.BASIC_SWITCH, UpdateOperation.SWITCH_UPDATED));
        }
        if(expectedUpdates.size() != 0){
            this.addDependency(update, expectedUpdates);
            return;
        }
        if (!this.swPorts.containsKey(port.getLeft())) {
            this.swPorts.put(port.getLeft(), Sets.newConcurrentHashSet());
        }
        this.swPorts.get(port.getLeft()).add(port.getRight());
        /* new ports are all outer ports */
        this.outPorts.add(port);

        for (IConcreteTopologyListener listener : this.listeners) {
            listener.portAdded(port);
        }
        logger.debug("port added, port={}", ReflectionToStringBuilder.toString(port));
    }

    /**
     * event dependency: link down
     */
    private void portRemoved(Pair<DatapathId, OFPort> port, LDUpdate update) {
        if(!this.swPorts.containsKey(port.getLeft()) || !this.swPorts.get(port.getLeft()).contains(port.getRight())){
            logger.warn("the port {} on switch {} has been removed.", port.getLeft(), port.getRight());
            return;
        }

        List<LDUpdate> expectedEvents = Lists.newArrayList();
        if (this.links.containsKey(port)) {
            Pair<DatapathId, OFPort> link = this.links.get(port);
            expectedEvents.add(new LDUpdate(port.getLeft(), port.getRight(), link.getLeft(), link.getRight(), U64.ZERO, LinkType.DIRECT_LINK, UpdateOperation.LINK_REMOVED));
            logger.debug("expect link {} -- {} down, delaying it", ToStringBuilder.reflectionToString(port), ToStringBuilder.reflectionToString(link));
        }
        if(expectedEvents.size() != 0){
            this.addDependency(update, expectedEvents);
            return;
        }
        /* remove port from the entry */
        this.swPorts.get(port.getLeft()).remove(port.getRight());
        if (this.swPorts.get(port.getLeft()).size() == 0) {
            this.swPorts.remove(port.getLeft());
        }
        /* tries to remove outer port */
        this.outPorts.remove(port);

        for (IConcreteTopologyListener listener : listeners) {
            listener.portRemoved(port);
        }
        logger.debug("port removed, port={}", ReflectionToStringBuilder.toString(port));
    }

    /**
     * event dependency: port added
     */
    private void linkAdded(Pair<DatapathId, OFPort> src, Pair<DatapathId, OFPort> dst, LDUpdate update) {
        if(this.links.containsKey(src) && this.links.get(src).equals(dst)){
            logger.warn("cannot add link {}--{} as it has been existed", ReflectionToStringBuilder.toString(src), ReflectionToStringBuilder.toString(dst));
            return;
        }
        if( this.links.containsKey(src) && !this.links.get(src).equals(dst)) {
            logger.error("detect two different link on the same port, src={}, oldDst={}, newDst={}, we use the final one",
                    new Object[]{ReflectionToStringBuilder.toString(src), ReflectionToStringBuilder.toString(this.links.get(src)), ReflectionToStringBuilder.toString(dst)});
            return;
        }

        List<LDUpdate> expectedEvents = Lists.newArrayList();
        if(!this.swPorts.containsKey(src.getLeft()) || !this.swPorts.get(src.getLeft()).contains(src.getRight())){
            expectedEvents.add(new LDUpdate(src.getLeft(), src.getRight(), UpdateOperation.PORT_UP));
            logger.debug("expect port {} on switch {} up, delaying event", src.getRight(), src.getLeft());
        }
        if(!this.swPorts.containsKey(dst.getLeft()) || !this.swPorts.get(dst.getLeft()).contains(dst.getRight())){
            expectedEvents.add(new LDUpdate(dst.getLeft(), dst.getRight(), UpdateOperation.PORT_UP));
            logger.debug("expect port {} on switch {} up, delaying event", dst.getRight(), dst.getLeft());
        }
        if(expectedEvents.size() != 0){
            this.addDependency(update, expectedEvents);
            return;
        }

        logger.debug("link added, {} -- {}",  ToStringBuilder.reflectionToString(src), ToStringBuilder.reflectionToString(dst));
        this.links.put(src, dst);
        this.reverseLinks.put(dst, src);
        /* maintain physical links */
        if (!this.physicalLinks.containsKey(src.getLeft())) {
            this.physicalLinks.put(src.getLeft(), Sets.newConcurrentHashSet());
        }
        this.physicalLinks.get(src.getLeft()).add(dst.getLeft());
        /* maintain out ports */
        if (this.outPorts.contains(src)) {
            this.outPorts.remove(src);
        }
        if (!this.links.containsKey(dst)) {
            this.outPorts.add(dst);
        }
        /* listen mechanics */
        for (IConcreteTopologyListener listener : this.listeners) {
            listener.linkAdded(src, dst);
        }
    }

    /**
     * event dependency:
     */
    private void linkRemoved(Pair<DatapathId, OFPort> src, Pair<DatapathId, OFPort> dst, LDUpdate update) {
        if(!this.links.containsKey(src) || !this.links.get(src).equals(dst)){
            logger.warn("the link {} -- {} has been removed.", ToStringBuilder.reflectionToString(src), ToStringBuilder.reflectionToString(dst));
            return;
        }
        List<LDUpdate> expectedEvents = Lists.newArrayList();
        if(expectedEvents.size() != 0){
            this.addDependency(update, expectedEvents);
            return;
        }
        this.links.remove(src);
        this.reverseLinks.remove(dst);
        /* maintain physical links */
        if (this.physicalLinks.containsKey(src.getLeft())) {
            this.physicalLinks.get(src.getLeft()).remove(dst.getLeft());
            if (this.physicalLinks.get(src.getLeft()).size() == 0) {
                this.physicalLinks.remove(src.getLeft());
            }
        }
        /* maintain out ports */
        this.outPorts.add(src);
        /* listener mechanics */
        for (IConcreteTopologyListener listener : this.listeners) {
            listener.linkRemoved(src, dst);
        }
        logger.debug("link removed, {} -- {}", ToStringBuilder.reflectionToString(src), ToStringBuilder.reflectionToString(dst));
    }

    private void addDependency(LDUpdate event, List<LDUpdate> depends) {
        EventInfo eventInfo = new EventInfo(event, depends.size());
        for(LDUpdate depend : depends){
            LDUpdateWrapper wrapper = new LDUpdateWrapper(depend);
            if(!this.eventDependency.containsKey(wrapper)) {
                this.eventDependency.put(wrapper, Sets.newHashSet());
            }
            this.eventDependency.get(wrapper).add(eventInfo);
        }
    }
}
