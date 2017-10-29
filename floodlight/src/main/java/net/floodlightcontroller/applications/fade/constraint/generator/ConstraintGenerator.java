package net.floodlightcontroller.applications.fade.constraint.generator;

import net.floodlightcontroller.applications.fade.constraint.Constraint;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.rule.generator.RuleGenerateResult;

import java.util.List;

/**
 * The constraint generator.
 * It generates constraint from flow, r1 (the first group of dedicated rules) and r2 (the second group of dedicated rules) rules.
 */
public interface ConstraintGenerator {
    /**
     * generate constraints
     * @param flow the flow
     * @param ruleGenerateResult the rule generation result
     * @param hasAnomaly is the flow has any anomaly (or in anomaly identification phase) ?
     * @return the generated constraints
     */
    List<Constraint> generateConstraint(Flow flow, RuleGenerateResult ruleGenerateResult, boolean hasAnomaly);
}
