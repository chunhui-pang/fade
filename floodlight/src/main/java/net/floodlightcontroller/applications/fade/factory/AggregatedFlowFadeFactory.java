package net.floodlightcontroller.applications.fade.factory;

import net.floodlightcontroller.applications.appmodule.concretetopology.IConcreteTopologyService;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleGraphService;
import net.floodlightcontroller.applications.fade.constraint.generator.AggregatedFlowConstraintGenerator;
import net.floodlightcontroller.applications.fade.constraint.generator.ConstraintGenerator;
import net.floodlightcontroller.applications.fade.flow.FlowSelector;
import net.floodlightcontroller.applications.fade.flow.aggregatedflow.AggregatedFlowSelector;
import net.floodlightcontroller.applications.fade.identifier.AggregatedFlowAnomalyIdentifier;
import net.floodlightcontroller.applications.fade.identifier.AnomalyIdentifier;
import net.floodlightcontroller.applications.fade.rule.generator.AggregatedFlowRuleGeneratorImpl;
import net.floodlightcontroller.applications.fade.rule.generator.ProbeSelector;
import net.floodlightcontroller.applications.fade.rule.generator.RuleGenerator;
import net.floodlightcontroller.applications.fade.util.TagManager;
import net.floodlightcontroller.applications.fade.util.AbstractTagManager;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.projectfloodlight.openflow.protocol.OFFactory;

/**
 * factory for aggregated flows
 */
public class AggregatedFlowFadeFactory extends CommonFadeFactory {
    private long acceptedCounterDeviation;
    public AggregatedFlowFadeFactory(int appId, String appName, long acceptedCounterDeviation, double detectionTagRatio, boolean ignoreTimeoutIssues,
                                     OFFactory ofFactory, IOFSwitchService ofSwitchService, IRuleGraphService ruleGraphService, IConcreteTopologyService concreteTopologyService,
                                     IThreadPoolService threadPoolService, ProbeSelector probeSelector) {
        super(appId, appName, 0.0, detectionTagRatio, ignoreTimeoutIssues, ofFactory, concreteTopologyService, ofSwitchService, ruleGraphService, threadPoolService, probeSelector);
        this.acceptedCounterDeviation = acceptedCounterDeviation;
    }

    @Override
    protected FlowSelector getFlowSelector(FadeContext fadeContext) {
        return new AggregatedFlowSelector(this.ruleGraphService);
    }

    @Override
    protected RuleGenerator getRuleGenerator(FadeContext fadeContext) {
        return new AggregatedFlowRuleGeneratorImpl(this.ofFactory, fadeContext.getTagManager(), fadeContext.getCookieManager(),
                fadeContext.getDedicatedRuleManager(), fadeContext.getRuleIndexer(), fadeContext.getStatsCollector().getStatsContext(),
                fadeContext.getDetectingFlowManager(), fadeContext.getProbeSelector(),
                this.concreteTopologyService);
    }

    @Override
    protected ConstraintGenerator getConstraintGenerator(FadeContext fadeContext) {
        return new AggregatedFlowConstraintGenerator(fadeContext.getDedicatedRuleManager(),
                fadeContext.getRuleIndexer(),
                this.acceptedCounterDeviation,
                this.ignoreTimeoutIssues);
    }

    @Override
    protected AnomalyIdentifier getAnomalyIdentifier(FadeContext fadeContext) {
        return new AggregatedFlowAnomalyIdentifier();
    }
}
