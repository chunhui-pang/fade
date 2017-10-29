package net.floodlightcontroller.applications.fade.flow;

/**
 * The node in flows
 */
public interface FlowNode {
    /**
     * Each node in a flow has an unique id, this function returns this id
     */
    long getId();
}
