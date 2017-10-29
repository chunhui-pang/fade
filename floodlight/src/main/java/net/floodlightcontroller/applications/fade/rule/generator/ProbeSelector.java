package net.floodlightcontroller.applications.fade.rule.generator;

import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.FlowNode;

import java.util.List;

/**
 * Specify how to select probes from flow
 */
public interface ProbeSelector {
    /**
     * selecting probes
     * @param flow the given flow
     * @return the selected probe nodes that on the flow
     */
    List<FlowNode> selectProbes(Flow flow);
}
