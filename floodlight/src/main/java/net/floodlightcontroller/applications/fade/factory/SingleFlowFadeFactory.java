package net.floodlightcontroller.applications.fade.factory;

import net.floodlightcontroller.applications.appmodule.concretetopology.IConcreteTopologyService;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleGraphService;
import net.floodlightcontroller.applications.fade.constraint.generator.ConstraintGenerator;
import net.floodlightcontroller.applications.fade.constraint.generator.SingleFlowConstraintGenerator;
import net.floodlightcontroller.applications.fade.flow.FlowSelector;
import net.floodlightcontroller.applications.fade.flow.singleflow.SingleFlowSelector;
import net.floodlightcontroller.applications.fade.identifier.AnomalyIdentifier;
import net.floodlightcontroller.applications.fade.identifier.SingleFlowAnomalyIdentifier;
import net.floodlightcontroller.applications.fade.rule.generator.ProbeSelector;
import net.floodlightcontroller.applications.fade.rule.generator.RuleGenerator;
import net.floodlightcontroller.applications.fade.rule.generator.SingleFlowRuleGeneratorImpl;
import net.floodlightcontroller.applications.fade.util.TagManager;
import net.floodlightcontroller.applications.fade.util.AbstractTagManager;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.projectfloodlight.openflow.protocol.OFFactory;

/**
 * factory method for SingleFlow (not aggregated)
 *
 */
public class SingleFlowFadeFactory extends CommonFadeFactory {
    public SingleFlowFadeFactory(int appId, String appName, double acceptedStatsDeviation, double detectionTagRatio, boolean ignoreTimeoutIssues,
                                 OFFactory ofFactory, IOFSwitchService ofSwitchService, IRuleGraphService ruleGraphService, IConcreteTopologyService concreteTopologyService,
                                 IThreadPoolService threadPoolService, ProbeSelector probeSelector) {
        super(appId, appName, acceptedStatsDeviation, detectionTagRatio, ignoreTimeoutIssues, ofFactory, concreteTopologyService, ofSwitchService, ruleGraphService, threadPoolService, probeSelector);
    }

    @Override
    protected FlowSelector getFlowSelector(FadeContext fadeContext) {
        return new SingleFlowSelector(this.ruleGraphService);
    }

    @Override
    protected RuleGenerator getRuleGenerator(FadeContext fadeContext) {
        return new SingleFlowRuleGeneratorImpl(this.ofFactory,
                fadeContext.getTagManager(),
                fadeContext.getCookieManager(),
                fadeContext.getDedicatedRuleManager(),
                fadeContext.getRuleIndexer(),
                fadeContext.getStatsCollector().getStatsContext(),
                fadeContext.getDetectingFlowManager(),
                fadeContext.getProbeSelector(),
                this.concreteTopologyService);
    }

    @Override
    protected ConstraintGenerator getConstraintGenerator(FadeContext fadeContext) {
        return new SingleFlowConstraintGenerator(fadeContext.getDedicatedRuleManager(), fadeContext.getRuleIndexer(), this.acceptedStatsDeviation, this.ignoreTimeoutIssues);
    }

    @Override
    protected AnomalyIdentifier getAnomalyIdentifier(FadeContext fadeContext) {
        return new SingleFlowAnomalyIdentifier();
    }
}
