package net.floodlightcontroller.applications.fade.rule.generator;

import net.floodlightcontroller.applications.fade.flow.Flow;

/**
 * the dedicated rule generator
 */
public interface RuleGenerator {

    /**
     * generate dedicated flow rules for a given flow.
     * @param flow the flow
     * @param detectionDuration the detection duration, in seconds
     * @param maxNetworkDelay the maximum network delay, in milliseconds
     * @return the generated result.
     */
    RuleGenerateResult generateDedicatedRules(Flow flow, int detectionDuration, long maxNetworkDelay);
}
