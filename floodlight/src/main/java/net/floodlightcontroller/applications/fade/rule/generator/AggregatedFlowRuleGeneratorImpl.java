package net.floodlightcontroller.applications.fade.rule.generator;

import com.google.common.collect.Lists;
import net.floodlightcontroller.applications.appmodule.concretetopology.IConcreteTopologyService;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.constraint.AllValidatingConstraint;
import net.floodlightcontroller.applications.fade.constraint.SequentialConstraint;
import net.floodlightcontroller.applications.fade.constraint.postvalidateaction.*;
import net.floodlightcontroller.applications.fade.exception.InvalidArgumentException;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import net.floodlightcontroller.applications.fade.flow.aggregatedflow.AggregatedFlow;
import net.floodlightcontroller.applications.fade.flow.aggregatedflow.AggregatedFlowNode;
import net.floodlightcontroller.applications.fade.flow.manager.DetectingFlowManager;
import net.floodlightcontroller.applications.fade.rule.manager.DedicatedRuleManager;
import net.floodlightcontroller.applications.fade.rule.manager.RuleIndexer;
import net.floodlightcontroller.applications.fade.stats.StatsContext;
import net.floodlightcontroller.applications.fade.util.AbstractTagManager;
import net.floodlightcontroller.applications.fade.util.CookieManager;
import net.floodlightcontroller.applications.fade.util.TagManager;
import net.floodlightcontroller.applications.util.hsa.HeaderSpace;
import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.protocol.OFFactory;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFFlowModFlags;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

/**
 * Rule generator for aggregated flows
 */
public class AggregatedFlowRuleGeneratorImpl extends SingleFlowRuleGeneratorImpl {
    private static final Logger logger = LoggerFactory.getLogger(AggregatedFlowRuleGeneratorImpl.class);

    public AggregatedFlowRuleGeneratorImpl(OFFactory ofFactory, TagManager tagManager, CookieManager cookieManager,
                                           DedicatedRuleManager dedicatedRuleManager, RuleIndexer ruleIndexer, StatsContext statsContext, DetectingFlowManager detectingFlowManager, ProbeSelector probeSelector,
                                           IConcreteTopologyService concreteTopologyService) {
        super(ofFactory, tagManager, cookieManager, dedicatedRuleManager, ruleIndexer, statsContext, detectingFlowManager, probeSelector, concreteTopologyService);
    }

    @Override
    protected void checkValidity(Flow flow) {
        if(!(flow instanceof AggregatedFlow)){
            throw new InvalidArgumentException("This class only generate dedicated rules for AggregatedFlow.");
        }
    }


    @Override
    protected void generateR1Rules(Flow flow, int tag, int detectionDuration, List<FlowNode> probes,
                                 List<OFFlowMod> r1Rules, List<DatapathId> r1Dpids, List<PostValidateAction> postValidateActions){
        AggregatedFlowNode first = (AggregatedFlowNode)probes.get(0);
        AggregatedFlow af = (AggregatedFlow) flow;
        for(int i = 0; i < af.size(); i++){
            // FIXME: we hasn't handle field rewrite yet
            // Note: these flowMods only contains match
            IRuleNode frn = af.getFlowNode().get(0).getRuleNodes().get(i);
            HeaderSpace hs = frn.getReallyMatchHS().clone();
            for(int j = 1; j < af.length(); j++) {
                HeaderSpace mhs = af.getFlowNode().get(j).getRuleNodes().get(i).getReallyMatchHS();
                hs.intersect(mhs);
            }
            List<OFFlowMod> flowMods = this.flowModConverter.read(hs, this.ofFactory);
            for(OFFlowMod finalMod : flowMods){
                OFFlowMod.Builder builder = finalMod.createBuilder();
                // build matches, only match none vlanVid (preventing disturbing detecting tasks)
                // say we are detecting 1 -> 2 -> 3 -> 4, however, we are locating 2 -> 3 ->4
                Match.Builder mb = finalMod.getMatch().createBuilder();
                this.tagManager.createTagMatch(this.tagManager.getBlankTag(), mb);
                builder.setMatch(mb.build());
                // build actions: set Vlan Tag, copy others
                List<OFAction> actions = Lists.newArrayList(this.tagManager.createAttachTagAction(tag, this.ofFactory));
                actions.addAll(frn.getActions());
                if(frn.getOutPort() != null){
                    builder.setOutPort(frn.getOutPort());
                }
                builder.setActions(actions);
                // set timeout, priority and SEND_FLOW_REM flag
                builder.setHardTimeout(detectionDuration);
                builder.setPriority(DEDICATED_RULE_PRIORITY);
                builder.setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM));
                // set cookie
                U64 cookie = cookieManager.requestCookie();
                builder.setCookie(cookie);
                // build and update results
                OFFlowMod fm = builder.build();
                r1Rules.add(fm);
                r1Dpids.add(frn.getDatapathId());
                this.addDedicatedRuleToManage(frn, fm);
                postValidateActions.add(new CookieReleasePostValidateAction(this.cookieManager, cookie));
                postValidateActions.add(new StatsReleasePostValidationAction(statsContext, this.ruleIndexer.getIndex(fm)));
                postValidateActions.add(new DedicatedRuleReleasePostValidateAction(this.dedicatedRuleManager, this.ruleIndexer.getIndex(fm)));
            }
        }
    }

    @Override
    protected void generateR2Rules(Flow flow, int tag, int duration, List<FlowNode> probes,
                                 List<OFFlowMod> r2Rules, List<DatapathId> r2Dpids, List<PostValidateAction> postValidateActions){
        for(int i = 1; i < probes.size()-1; i++) {
            AggregatedFlowNode afn = (AggregatedFlowNode) probes.get(i);
            IRuleNode exampleRn = afn.getRuleNodes().get(0);
            IRuleNode preExampleRn = ((AggregatedFlowNode)flow.getFlowNode().get(flow.getFlowNode().indexOf(probes.get(i))-1)).getRuleNodes().get(0);
            OFFlowMod.Builder builder = ofFactory.buildFlowAdd();
            // build match
            Match.Builder mb = ofFactory.buildMatch();
            this.tagManager.createTagMatch(tag, mb);
            Pair<OFPort, OFPort> link = this.concreteTopologyService.getLink(preExampleRn.getDatapathId(), exampleRn.getDatapathId());
            if(link != null){
                mb.setExact(MatchField.IN_PORT, link.getRight());
            } else {
                logger.warn("cannot get the link between switch {} and {}", preExampleRn.getDatapathId(), exampleRn.getDatapathId());
            }
            builder.setMatch(mb.build());
            // build actions
            List<OFAction> actions = Lists.newArrayList(exampleRn.getActions());
            if (exampleRn.getOutPort() != null) {
                builder.setOutPort(exampleRn.getOutPort());
            }
            builder.setActions(actions);
            // set timeout, priority and SEND_FLOW_REM flag
            builder.setHardTimeout(duration);
            builder.setPriority(DEDICATED_RULE_PRIORITY);
            builder.setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM));
            // set cookie
            U64 cookie = cookieManager.requestCookie();
            builder.setCookie(cookie);
            // build and update result
            OFFlowMod finalMod = builder.build();
            r2Rules.add(finalMod);
            r2Dpids.add(exampleRn.getDatapathId());
            this.addDedicatedRuleToManage(exampleRn, finalMod);
            postValidateActions.add(new CookieReleasePostValidateAction(cookieManager, cookie));
            postValidateActions.add(new StatsReleasePostValidationAction(statsContext, this.ruleIndexer.getIndex(finalMod)));
            postValidateActions.add(new DedicatedRuleReleasePostValidateAction(this.dedicatedRuleManager, this.ruleIndexer.getIndex(finalMod)));
        }
    }

    @Override
    protected void generateTailRule(Flow flow, int tag, int duration, List<FlowNode> probes,
                                         List<OFFlowMod> r2Rules, List<DatapathId> r2Dpids, List<PostValidateAction> postValidateActions){
        AggregatedFlowNode tail = (AggregatedFlowNode)probes.get(probes.size()-1);
        IRuleNode preExampleRn = ((AggregatedFlowNode)flow.getFlowNode().get(flow.getFlowNode().indexOf(tail)-1)).getRuleNodes().get(0);
        IRuleNode exampleRn = tail.getRuleNodes().get(0);
        // match with the original match and the tag
        OFFlowMod.Builder builder = ofFactory.buildFlowAdd();
        // build match
        Match.Builder mb = ofFactory.buildMatch();
        this.tagManager.createTagMatch(tag, mb);
        Pair<OFPort, OFPort> link = this.concreteTopologyService.getLink(preExampleRn.getDatapathId(), exampleRn.getDatapathId());
        if(link != null){
            mb.setExact(MatchField.IN_PORT, link.getRight());
        } else {
            logger.warn("cannot get the link between switch {} and {}", preExampleRn.getDatapathId(), exampleRn.getDatapathId());
        }
        builder.setMatch(mb.build());
        // build actions
        List<OFAction> actions = Lists.newArrayList(this.tagManager.createStripTagAction(tag, this.ofFactory));
        actions.addAll(exampleRn.getActions());
        if(exampleRn.getOutPort() != null){
            builder.setOutPort(exampleRn.getOutPort());
        }
        builder.setActions(actions);
        // set timeout, priority and SEND_FLOW_REM flag
        builder.setHardTimeout(duration);
        builder.setPriority(DEDICATED_RULE_PRIORITY);
        builder.setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM));
        // set cookie
        U64 cookie = cookieManager.requestCookie();
        builder.setCookie(cookie);
        // build and update results
        OFFlowMod flowMod = builder.build();
        r2Rules.add(flowMod);
        r2Dpids.add(exampleRn.getDatapathId());
        this.addDedicatedRuleToManage(exampleRn, flowMod);
        postValidateActions.add(new CookieReleasePostValidateAction(cookieManager, cookie));
        postValidateActions.add(new StatsReleasePostValidationAction(statsContext, this.ruleIndexer.getIndex(flowMod)));
        postValidateActions.add(new DedicatedRuleReleasePostValidateAction(this.dedicatedRuleManager, this.ruleIndexer.getIndex(flowMod)));
    }
}
