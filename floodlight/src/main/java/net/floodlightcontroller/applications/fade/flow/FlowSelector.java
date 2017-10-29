package net.floodlightcontroller.applications.fade.flow;

import net.floodlightcontroller.applications.fade.flow.Flow;

import java.util.List;

/**
 * The flow selector.
 * Selecting flows from rule graph
 */
public interface FlowSelector {
    /**
     * select several flows from rule graph
     * @param limit the maximum number of selected flows
     * @return the iterator of the selected flows
     */
    List<Flow> getFlows(int limit);

    /**
     * get all flows in the rule graph
     * @return a list contains all flows
     */
    List<Flow> getAllFlows();
}
