package net.floodlightcontroller.applications.fade.constraint.evaluator;

import net.floodlightcontroller.applications.fade.constraint.Constraint;
import net.floodlightcontroller.applications.fade.flow.Flow;

import java.util.List;
import java.util.concurrent.ScheduledExecutorService;

/**
 * constraint evaluator, evaluating constraint periodically, and generate suspicious flows
 */
public interface ConstraintEvaluator {
    /**
     * add a new constraint to evaluate in future.
     * @param constraint the constraint
     * @param evalDelay the time after which it would be evaluated.
     */
    void addConstraint(Constraint constraint, long evalDelay);

    /**
     * poll suspicous flows
     * @param limit the number of maximum flows to be polled.
     * @return a list of flows
     */
    List<List<Flow>> pollSuspiciousFlows(int limit);

    /**
     * poll passed suspicious flows.
     * Unlike suspicious flows, we cannot identify any information from these flows by passing it to {@link net.floodlightcontroller.applications.fade.identifier.AnomalyIdentifier}
     * @param limit the number of flows to be retrieved
     * @return the list of passed suspicious flows.
     */
    List<List<Flow>> pollPassedSuspiciousFlows(int limit);
    /**
     * evaluating start.
     * The time before the first evaluation could be customized,
     * and the evaluation interval is automatically determined by the implementation
     * @param initDelay the time before first evaluation (in milliseconds)
     */
    void startEvaluating(long initDelay);
}
