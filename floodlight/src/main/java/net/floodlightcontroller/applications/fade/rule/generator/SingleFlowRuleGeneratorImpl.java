package net.floodlightcontroller.applications.fade.rule.generator;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.floodlightcontroller.applications.appmodule.concretetopology.IConcreteTopologyService;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.appmodule.rulegraph.rule.FlowRuleNode;
import net.floodlightcontroller.applications.fade.constraint.postvalidateaction.*;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import net.floodlightcontroller.applications.fade.flow.manager.DetectingFlowManager;
import net.floodlightcontroller.applications.fade.rule.manager.DedicatedRuleManager;
import net.floodlightcontroller.applications.fade.rule.manager.RuleIndexer;
import net.floodlightcontroller.applications.fade.stats.StatsContext;
import net.floodlightcontroller.applications.fade.util.AbstractTagManager;
import net.floodlightcontroller.applications.fade.util.CookieManager;
import net.floodlightcontroller.applications.fade.util.TagManager;
import net.floodlightcontroller.applications.fade.util.TagRunOutException;
import net.floodlightcontroller.applications.util.hsa.FlowModHSConverter;
import net.floodlightcontroller.applications.fade.exception.OperationNotSupportedException;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.singleflow.SingleFlow;
import net.floodlightcontroller.applications.fade.flow.singleflow.SingleFlowNode;
import net.floodlightcontroller.applications.util.hsa.IHSConverter;
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

import java.security.SecureRandom;
import java.util.*;

/**
 * Implementation of {@link RuleGenerator} for singleflow.
 *
 * @implNote ToS field in floodlight is slightly intrigue.
 * In actions, it is a short value from 0 to 255, and it should be times of 4.
 * However, in matches, it could be any value from 0 to 64.
 *
 * !! we use VLAN_VID now, because it's so little IP TOS values.
 */
public class SingleFlowRuleGeneratorImpl implements RuleGenerator {
    private static final Logger logger = LoggerFactory.getLogger(SingleFlowRuleGeneratorImpl.class);

    protected OFFactory ofFactory;
    protected TagManager tagManager;
    protected CookieManager cookieManager;
    protected DedicatedRuleManager dedicatedRuleManager;
    protected RuleIndexer ruleIndexer;
    protected StatsContext statsContext;
    protected DetectingFlowManager detectingFlowManager;
    protected ProbeSelector probeSelector;
    protected IConcreteTopologyService concreteTopologyService;

    protected IHSConverter<OFFlowMod> flowModConverter;
    protected static final int MIN_FLOW_LENGTH = 3;
    protected static final short BLANK_IP_DSCP = 0;
    protected static final double DELAY_MAGNIFY_RATIO = 3.0D;
    protected static final int IP_TOS_FACTOR = 4; // ip ToS value must be times of 4
    protected static final int DEDICATED_RULE_PRIORITY = Integer.MAX_VALUE;

    public SingleFlowRuleGeneratorImpl(OFFactory ofFactory, TagManager tagManager, CookieManager cookieManager,
                                       DedicatedRuleManager dedicatedRuleManager, RuleIndexer ruleIndexer, StatsContext statsContext,
                                       DetectingFlowManager detectingFlowManager, ProbeSelector probeSelector,
                                       IConcreteTopologyService concreteTopologyService){
        this.ofFactory = ofFactory;
        this.tagManager = tagManager;
        this.cookieManager = cookieManager;
        this.dedicatedRuleManager = dedicatedRuleManager;
        this.ruleIndexer = ruleIndexer;
        this.statsContext = statsContext;
        this.detectingFlowManager = detectingFlowManager;
        this.probeSelector = probeSelector;
        this.concreteTopologyService = concreteTopologyService;

        this.flowModConverter = new FlowModHSConverter();
    }

    @Override
    public RuleGenerateResult generateDedicatedRules(Flow flow, int detectionDuration, long maxNetworkDelay) {
        this.checkValidity(flow);
        if(flow.getFlowNode().size() < MIN_FLOW_LENGTH){
            logger.debug("we only generate dedicated rules for flows which greater than {}", MIN_FLOW_LENGTH);
            return null;
        }
        List<FlowNode> probes = this.probeSelector.selectProbes(flow);

        int tag = tagManager.requestDetectionTag();
        List<OFFlowMod> r1Rules = Lists.newArrayList();
        List<DatapathId> r1Dpids = Lists.newArrayList();
        List<OFFlowMod> r2Rules = Lists.newArrayList();
        List<DatapathId> r2Dpids = Lists.newArrayList();
        List<PostValidateAction> postValidateActions = Lists.newArrayList();
        int hardtimeout = detectionDuration;
        this.generateR1Rules(flow, tag, hardtimeout, probes, r1Rules, r1Dpids, postValidateActions);
        hardtimeout = detectionDuration + (int)Math.ceil(maxNetworkDelay*DELAY_MAGNIFY_RATIO/1000.0);
        this.generateR2Rules(flow, tag, hardtimeout, probes, r2Rules, r2Dpids, postValidateActions);
        this.generateTailRule(flow, tag, hardtimeout, probes, r2Rules, r2Dpids, postValidateActions);

        postValidateActions.add(new TagReleasePostValidateAction(tagManager, tag));
        postValidateActions.add(new FlowReleasePostValidateAction(this.detectingFlowManager, flow));
        long enforceDelay = maxNetworkDelay;
        // use anonymous class as result
        return new RuleGenerateResult() {
            @Override
            public List<FlowNode> getSelectedFlowNodes() { return probes; }
            @Override
            public List<OFFlowMod> getR1Rules() { return r1Rules; }
            @Override
            public List<DatapathId> getR1Switches() { return r1Dpids; }
            @Override
            public List<OFFlowMod> getR2Rules() { return r2Rules; }
            @Override
            public List<DatapathId> getR2Switches() { return r2Dpids; }
            @Override
            public long getEnforceDelayBetweenR1AndR2() { return enforceDelay; }
            @Override
            public List<PostValidateAction> getPostValidateActions() { return postValidateActions; }
        };
    }

    protected void checkValidity(Flow flow) {
        if( !(flow instanceof SingleFlow)) {
            throw new OperationNotSupportedException("This class only support generate dedicated rules for SingleFlow");
        }
    }

    protected void generateR1Rules(Flow sf, int tag, int hardTimeout, List<FlowNode> probes,
                                 List<OFFlowMod> r1Rules, List<DatapathId> r1Dpids, List<PostValidateAction> postValidateActions) {
        SingleFlowNode first = (SingleFlowNode)probes.get(0);
        IRuleNode ruleNode = first.getRuleNode();
        // FIXME: we haven't get the really match of current flow, thus we cannot handle flow split yet.
        // Note: these flowMods only contains match
        List<OFFlowMod> convertedFlowMods = this.flowModConverter.read(ruleNode.getReallyMatchHS(), this.ofFactory);
        int i = 1;
        // insert tag, copy actions
        for(OFFlowMod convertFlodMod : convertedFlowMods){
            OFFlowMod.Builder builder = convertFlodMod.createBuilder();
            // build matches
            Match.Builder mb = convertFlodMod.getMatch().createBuilder();
            this.tagManager.createTagMatch(this.tagManager.getBlankTag(), mb);
            builder.setMatch(mb.build());
            // set ToS tag and copy other actions
            List<OFAction> tagActions = this.tagManager.createAttachTagAction(tag, this.ofFactory);
            List<OFAction> actions = Lists.newArrayList(tagActions);
            actions.addAll(ruleNode.getActions());
            if(ruleNode.getOutPort() != null){
                builder.setOutPort(ruleNode.getOutPort());
            }
            builder.setActions(actions);
            // set timeout, priority, SEND_FLOW_REM flag
            builder.setHardTimeout(hardTimeout);
            builder.setPriority(DEDICATED_RULE_PRIORITY);
            builder.setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM));
            // set cookie
            U64 cookie = cookieManager.requestCookie();
            builder.setCookie(cookie);
            OFFlowMod r1Rule = builder.build();
            // update result
            this.addDedicatedRuleToManage(ruleNode, r1Rule);
            r1Rules.add(r1Rule);
            r1Dpids.add(ruleNode.getDatapathId());
            postValidateActions.add(new CookieReleasePostValidateAction(this.cookieManager, cookie));
            postValidateActions.add(new StatsReleasePostValidationAction(statsContext, ruleIndexer.getIndex(r1Rule)));
            postValidateActions.add(new DedicatedRuleReleasePostValidateAction(dedicatedRuleManager, ruleIndexer.getIndex(r1Rule)));
        }
    }

    protected void generateR2Rules(Flow sf, int tag, int timeout, List<FlowNode> probes,
                                 List<OFFlowMod> r2Rules, List<DatapathId> r2Dpids, List<PostValidateAction> postValidateActions) {
        for(int i = 1; i < probes.size()-1; i++) {
            IRuleNode r2 = ((SingleFlowNode)probes.get(i)).getRuleNode();
            IRuleNode preR2 = ((SingleFlowNode)sf.getFlowNode().get(sf.getFlowNode().indexOf(probes.get(i))-1)).getRuleNode();
            // add extra match to match the tag, copy actions
            OFFlowMod flowMod = this.flowModConverter.read(r2.getMatchHS(), this.ofFactory);
            OFFlowMod.Builder builder = flowMod.createBuilder();
            // add match (tag, port). Note, a floodlight bug exists
            Match.Builder mb = flowMod.getMatch().createBuilder();
            this.tagManager.createTagMatch(tag, mb);
            Pair<OFPort, OFPort> link = this.concreteTopologyService.getLink(preR2.getDatapathId(), r2.getDatapathId());
            if(link != null) {
                mb.setExact(MatchField.IN_PORT, link.getRight());
            } else {
                logger.warn("cannot get the link between switch {} and {}", preR2.getDatapathId(), r2.getDatapathId());
            }
            builder.setMatch(mb.build());
            // copy actions
            List<OFAction> actions = Lists.newArrayList(r2.getActions());
            if (r2.getOutPort() != null) {
                builder.setOutPort(r2.getOutPort());
            }
            builder.setActions(actions);
            // set timeout, priority and SEND_FLOW_REM flag
            builder.setHardTimeout(timeout);
            builder.setPriority(DEDICATED_RULE_PRIORITY);
            builder.setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM));
            // set cookie
            U64 cookie = cookieManager.requestCookie();
            builder.setCookie(cookie);
            // build rule and update result
            OFFlowMod r2Rule = builder.build();
            r2Rules.add(r2Rule);
            r2Dpids.add(r2.getDatapathId());
            this.addDedicatedRuleToManage(r2, r2Rule);
            postValidateActions.add(new CookieReleasePostValidateAction(cookieManager, cookie));
            postValidateActions.add(new StatsReleasePostValidationAction(statsContext, this.ruleIndexer.getIndex(r2Rule)));
            postValidateActions.add(new DedicatedRuleReleasePostValidateAction(this.dedicatedRuleManager, this.ruleIndexer.getIndex(r2Rule)));
        }
    }

    protected void generateTailRule(Flow sf, int tag, int timeout, List<FlowNode> probes,
                                       List<OFFlowMod> r2Rules, List<DatapathId> r2Dpids, List<PostValidateAction> postValidateActions) {
        SingleFlowNode tail = (SingleFlowNode)probes.get(probes.size()-1);
        SingleFlowNode preTail = ((SingleFlowNode)sf.getFlowNode().get(sf.getFlowNode().indexOf(tail)-1));
        OFFlowMod flowMod = this.flowModConverter.read(tail.getRuleNode().getMatchHS(), this.ofFactory);
        OFFlowMod.Builder builder = flowMod.createBuilder();
        // build match, Note a floodlight bug exist
        Match.Builder mb = flowMod.getMatch().createBuilder();
        this.tagManager.createTagMatch(tag, mb);
        Pair<OFPort, OFPort> link = this.concreteTopologyService.getLink(preTail.getRuleNode().getDatapathId(), tail.getRuleNode().getDatapathId());
        if(link != null) {
            mb.setExact(MatchField.IN_PORT, link.getRight());
        } else {
            logger.warn("cannot get the link between switch {} and {}", preTail.getRuleNode().getDatapathId(), tail.getRuleNode().getDatapathId());
        }
        builder.setMatch(mb.build());
        // build actions
        List<OFAction> stripActions = this.tagManager.createStripTagAction(tag, this.ofFactory);
        List<OFAction> actions = Lists.newArrayList(stripActions);
        actions.addAll(tail.getRuleNode().getActions());
        if(tail.getRuleNode().getOutPort() != null){
            builder.setOutPort(tail.getRuleNode().getOutPort());
        }
        builder.setActions(actions);
        // set cookie
        U64 cookie = cookieManager.requestCookie();
        builder.setCookie(cookie);
        // set timeout, priority and SEND_FLOW_REM flag
        builder.setHardTimeout(timeout);
        builder.setPriority(DEDICATED_RULE_PRIORITY);
        builder.setFlags(Collections.singleton(OFFlowModFlags.SEND_FLOW_REM));
        // build and update result
        OFFlowMod tailMod = builder.build();
        r2Rules.add(tailMod);
        r2Dpids.add(tail.getRuleNode().getDatapathId());
        this.addDedicatedRuleToManage(tail.getRuleNode(), tailMod);
        postValidateActions.add(new CookieReleasePostValidateAction(cookieManager, cookie));
        postValidateActions.add(new StatsReleasePostValidationAction(statsContext, this.ruleIndexer.getIndex(tailMod)));
        postValidateActions.add(new DedicatedRuleReleasePostValidateAction(this.dedicatedRuleManager, this.ruleIndexer.getIndex(tailMod)));
    }

    protected void addDedicatedRuleToManage(IRuleNode ruleNode, OFFlowMod flowMod) {
        long idx = this.ruleIndexer.getIndex(flowMod);
        this.dedicatedRuleManager.addDedicatedRule(ruleNode, idx);
    }
}
