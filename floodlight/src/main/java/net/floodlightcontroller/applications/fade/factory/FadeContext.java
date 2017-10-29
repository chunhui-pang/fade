package net.floodlightcontroller.applications.fade.factory;

import net.floodlightcontroller.applications.fade.constraint.evaluator.ConstraintEvaluator;
import net.floodlightcontroller.applications.fade.constraint.generator.ConstraintGenerator;
import net.floodlightcontroller.applications.fade.flow.FlowSelector;
import net.floodlightcontroller.applications.fade.flow.manager.DetectingFlowManager;
import net.floodlightcontroller.applications.fade.identifier.AnomalyIdentifier;
import net.floodlightcontroller.applications.fade.rule.generator.ProbeSelector;
import net.floodlightcontroller.applications.fade.rule.manager.DedicatedRuleManager;
import net.floodlightcontroller.applications.fade.rule.enforcer.RuleEnforcer;
import net.floodlightcontroller.applications.fade.rule.generator.RuleGenerator;
import net.floodlightcontroller.applications.fade.rule.manager.RuleIndexer;
import net.floodlightcontroller.applications.fade.stats.StatsCollector;
import net.floodlightcontroller.applications.fade.util.CookieManager;
import net.floodlightcontroller.applications.fade.util.TagManager;

/**
 * the runtime context of FADE.
 * It contains various of managers and modules.
 */
public interface FadeContext {
    /**
     * get the tag manager
     * @return the tag manager
     */
    TagManager getTagManager();

    /**
     * get cookie manager
     * @return the cookie manager
     */
    CookieManager getCookieManager();

    /**
     * get the dedicated rule manager
     * @return the dedicated rule manager
     */
    DedicatedRuleManager getDedicatedRuleManager();

    /**
     * get the rule indexer
     * @return the rule indexer
     */
    RuleIndexer getRuleIndexer();

    /**
     * get the detecting flow manager
     * @return the detecting flow manager
     */
    DetectingFlowManager getDetectingFlowManager();

    ProbeSelector getProbeSelector();

    /**
     * get the flow selector
     * @return the flow selector
     */
    FlowSelector getFlowSelector();

    /**
     * get the rule generator
     * @return the rule generator
     */
    RuleGenerator getRuleGenerator();

    /**
     * get the constraint generator
     * @return constraint generator
     */
    ConstraintGenerator getConstraintGenerator();

    /**
     * get the rule enforcer
     * @return the rule enforcer
     */
    RuleEnforcer getRuleEnforcer();

    /**
     * get the flow statistics collector
     * @return the stats collector
     */
    StatsCollector getStatsCollector();

    /**
     * get the constraint evaluator
     * @return the constraint evaluator
     */
    ConstraintEvaluator getConstraintEvaluator();

    /**
     * get the anomaly identifier
     * @return the anomaly identifier
     */
    AnomalyIdentifier getAnomalyIdentifier();
}
