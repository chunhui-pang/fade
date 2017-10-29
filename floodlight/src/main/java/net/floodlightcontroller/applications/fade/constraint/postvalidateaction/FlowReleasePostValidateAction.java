package net.floodlightcontroller.applications.fade.constraint.postvalidateaction;

import net.floodlightcontroller.applications.fade.constraint.Constraint;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.manager.DetectingFlowManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * release flow detection.
 * Note, we only release the detection of flows after there is no suspicious flows.
 * Otherwise, we replace the detection with the detection of the subflows of current flow
 */
public class FlowReleasePostValidateAction implements PostValidateAction {
    private static final Logger logger = LoggerFactory.getLogger(FlowReleasePostValidateAction.class);
    private DetectingFlowManager detectingFlowManager;
    private Flow toBeReleased;

    public FlowReleasePostValidateAction(DetectingFlowManager detectingFlowManager, Flow toBeReleased){
        this.detectingFlowManager = detectingFlowManager;
        this.toBeReleased = toBeReleased;
    }

    @Override
    public void execute(Constraint constraint, boolean validateResult) {
        Flow flow = this.toBeReleased == null ? constraint.getFlow() : this.toBeReleased;
        if(constraint.getSuspiciousFlow() == null && constraint.getPassedSuspiciousFlow() == null){
            if(logger.isDebugEnabled()) {
                logger.debug("release detecting flow {}", constraint.getFlow());
            }
            this.detectingFlowManager.releaseDetectingFlow(flow);
        } else {
            int releaseNumOfNodes = flow.size() * flow.length();
            Collection<Flow> remaining = constraint.getSuspiciousFlow() != null ? constraint.getSuspiciousFlow() : constraint.getPassedSuspiciousFlow();
            for(Flow r : remaining){
                releaseNumOfNodes -= (r.size() * r.length());
            }
            if(logger.isDebugEnabled()) {
                logger.debug("release {} flow nodes of detecting flow {}", releaseNumOfNodes, flow);
            }
            this.detectingFlowManager.releaseFlowNodes(flow, releaseNumOfNodes);
        }
    }

    @Override
    public String toString() {
        return "FlowReleasePostValidateAction{" +
                "toBeReleased=" + toBeReleased +
                '}';
    }
}
