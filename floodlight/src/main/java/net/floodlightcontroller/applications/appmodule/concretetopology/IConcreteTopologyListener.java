package net.floodlightcontroller.applications.appmodule.concretetopology;

import net.floodlightcontroller.core.IOFSwitch;
import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * This interface declares a concrete implementation of "Topology Service". <br />
 * This service masks application from redundant events like "switch added", "link added"...,
 * which is not provided with the {@link net.floodlightcontroller.topology.ITopologyListener} comes with Floodlight
 *
 * @author chunhui (chunhui.pang@outlook.com)
 * @see net.floodlightcontroller.topology.ITopologyListener
 */
public interface IConcreteTopologyListener {
    /**
     * new switch added
     * @param dpid the new switch
     */
    void switchAdded(DatapathId dpid);


    /**
     * An existed switch is detached
     * @param dpid the detached switch
     */
    void switchRemoved(DatapathId dpid);

    /**
     * A new port is detected on a switch
     * @param ofPort the new port and the switch
     */
    void portAdded(Pair<DatapathId, OFPort> ofPort);

    /**
     * An existing port is detached
     * @param ofPort the existing port
     */
    void portRemoved(Pair<DatapathId, OFPort> ofPort);

    /**
     * new link is detected
     * @param portSrc the source device and the corresponding port
     * @param portDst the destination device and the corresponding port
     */
    void linkAdded(Pair<DatapathId, OFPort> portSrc, Pair<DatapathId, OFPort> portDst);

    /**
     * An existing link is crashed
     * @param portSrc the source end of the link
     * @param portDst the destination end of the link
     */
    void linkRemoved(Pair<DatapathId, OFPort> portSrc, Pair<DatapathId, OFPort> portDst);


}
