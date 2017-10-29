package net.floodlightcontroller.applications.appmodule.concretetopology;

import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.IFloodlightService;
import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.List;
import java.util.Set;

/**
 * This interface declares an concrete implementation of "TopologyService",
 * which is very similar with {@link net.floodlightcontroller.topology.ITopologyService}. <br />
 * However, it can mask applications with redundant events. <br />
 * As another convenience, we also declare more application-specific function to retrieve useful information
 * for our "forwarding anomaly detection application"(fad)
 *
 * @author chunhui (chunhui.pang@outlook.com)
 * @see net.floodlightcontroller.topology.ITopologyService
 */
public interface IConcreteTopologyService extends IFloodlightService {

    /**
     * Retrieving ports of a given switch
     * @param dpid the datapath id of the given switch
     * @return all port of this switch
     */
    Set<OFPort> getPorts(DatapathId dpid);

    /**
     * Checking if the given port is an outer port which connects to outer network (or, switch not in current network)
     * @param ofPort the specified port
     * @return true if it is outer port
     */
    boolean isOuterPort(Pair<DatapathId, OFPort> ofPort);

    /**
     * Retrieving all outer port from current switch. <br />
     * Outer port is such a port with which no switch in current network connects
     * @return all outer ports
     */
    Set< Pair<DatapathId, OFPort> > getOuterPorts();

    /**
     * Retrieving the port connected to the given port. Actually, those two port is connected by a physical link
     * @param ofPort the given port
     * @return the another port connect with the port given in parameter
     */
    Pair<DatapathId, OFPort> linkTransfer(Pair<DatapathId, OFPort> ofPort);

    /**
     * Retrieving downstream switches
     * @param dpid current switch
     * @return all downstream switches
     */
    Set<DatapathId> getDownstreamSwitch(DatapathId dpid);

    /**
     * Retrieving upstream switches, which can output traffic to current switch.
     * If parameter <code>ofPort</code> is specified, these switches can only forward traffic to the given switch
     * @param dpid the datapath id
     * @param ofPort the input port. Optional
     * @return the set of upstream switches
     */
    Set<DatapathId> getUpstreamSwitch(DatapathId dpid, OFPort ofPort);

    /**
     * retrieving the link between two switches
     * @param left the upstream switch
     * @param right the downstream switch
     * @return the two ports related to the two switch
     */
    Pair<OFPort, OFPort> getLink(DatapathId left, DatapathId right);

    /**
     * add listener
     * @param listener listener class
     * @return if successful
     * @see IConcreteTopologyListener
     */
    boolean addListener(IConcreteTopologyListener listener);

    /**
     * remove listener
     * @param listener the listener class
     * @return if successful
     * @see IConcreteTopologyListener
     */
    boolean removeListener(IConcreteTopologyListener listener);

    /**
     * debug only
     */
    void dump();
}
