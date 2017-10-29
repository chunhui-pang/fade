package net.floodlightcontroller.applications.fade.flow.manager;

import net.floodlightcontroller.applications.fade.flow.Flow;

import java.util.Collection;
import java.util.List;

/**
 * An interface to monitor all flow that are currently being detected.
 * For a given flow, or its subflows,
 * we only permit one instance is being detect at one time.
 */
public interface DetectingFlowManager {
    /**
     * try to add a flow to detect task.
     * @param flow the flow
     * @return true if this flow could be detected.
     */
    boolean tryAddFlow(Flow flow);

    /**
     * determine if a flow is being detected
     * @param flow the flow
     * @return true if it is being detected
     */
    boolean isBeingDetected(Flow flow);

    /**
     * If a flow is identified with suspicious, we would start localization progress.
     * If no anomaly is found in the localization progress, we would start with another localization progress.
     * If the flow has passed several runs of localization progress, we would set it to be passed.
     * @param flow the flow
     * @return the number of runs of localization progress.
     */
    int addLocalizationRun(Flow flow);

    /**
     * finish the detection of a flow
     * @param flow the flow
     */
    void releaseDetectingFlow(Flow flow);

    /**
     * replace the detection of flow with the detection of its subflows
     * @param flow the flow
     * @param numOfNodes the number of nodes to release
     */
    void releaseFlowNodes(Flow flow, int numOfNodes);

}
