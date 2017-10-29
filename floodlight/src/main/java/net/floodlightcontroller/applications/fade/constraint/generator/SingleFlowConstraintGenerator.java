package net.floodlightcontroller.applications.fade.constraint.generator;

import com.google.common.collect.Lists;
import net.floodlightcontroller.applications.fade.constraint.Constraint;
import net.floodlightcontroller.applications.fade.constraint.First2LastValidatingConstraint;
import net.floodlightcontroller.applications.fade.constraint.SequentialConstraint;
import net.floodlightcontroller.applications.fade.constraint.StatsExpressionConstraint;
import net.floodlightcontroller.applications.fade.exception.InvalidArgumentException;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import net.floodlightcontroller.applications.fade.flow.singleflow.SingleFlow;
import net.floodlightcontroller.applications.fade.rule.manager.DedicatedRuleManager;
import net.floodlightcontroller.applications.fade.rule.generator.RuleGenerateResult;
import net.floodlightcontroller.applications.fade.rule.manager.RuleIndexer;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * constraint generator for single flow
 */
public class SingleFlowConstraintGenerator implements ConstraintGenerator {
    private static Logger logger = LoggerFactory.getLogger(SingleFlowConstraintGenerator.class);

    protected DedicatedRuleManager dedicatedRuleManager;
    protected RuleIndexer ruleIndexer;
    protected static final double ACCEPTED_DEVIATION = 0.03F;
    protected static final double DOUBLE_COMPARISON_FIX = 1.0e-6;
    protected double acceptedDeviation = ACCEPTED_DEVIATION;
    protected boolean ignoreTimeoutIssues;

    public SingleFlowConstraintGenerator( DedicatedRuleManager dedicatedRuleManager, RuleIndexer ruleIndexer, double acceptedDeviation, boolean ignoreTimeoutIssues){
        this.dedicatedRuleManager = dedicatedRuleManager;
        this.ruleIndexer = ruleIndexer;
        this.acceptedDeviation = acceptedDeviation;
        this.ignoreTimeoutIssues = ignoreTimeoutIssues;
    }

    @Override
    public List<Constraint> generateConstraint(Flow flow, RuleGenerateResult ruleGenerateResult, boolean hasAnomaly) {
        this.validateFlows(flow);
        List<Constraint> constraints = Lists.newArrayList();
        SequentialConstraint seqConst = this.createSequentialConstraint(flow);

        String firstStats = this.getFirstNodeStats(ruleGenerateResult.getR1Rules());

        int idx = 0;
        for (OFFlowMod flowMod : ruleGenerateResult.getR2Rules()){
            String exp = this.buildEqualConstraintWithDeviation(firstStats, this.getOtherNodeStats(flowMod));
            FlowNode prevFlowNode = this.getR2RelatedFlowNode(ruleGenerateResult.getSelectedFlowNodes(), idx - 1);
            FlowNode currentFlowNode = this.getR2RelatedFlowNode(ruleGenerateResult.getSelectedFlowNodes(), idx);
            Collection<Flow> suspiciousFlow = this.generateSuspiciousFlows(flow, prevFlowNode, currentFlowNode);
            StatsExpressionConstraint seCons = new StatsExpressionConstraint(flow, dedicatedRuleManager, exp, suspiciousFlow);
            seqConst.appendConstraint(seCons);
            idx++;
        }
        seqConst.setPostValidateActions(ruleGenerateResult.getPostValidateActions());
        // anomaly identification
        seqConst.setPassedSuspiciousFlow(this.buildPassedSuspiciousFlow(flow, ruleGenerateResult, hasAnomaly));
        constraints.add(seqConst);
        return constraints;
    }

    protected List<Flow> buildPassedSuspiciousFlow(Flow flow, RuleGenerateResult ruleGenerateResult, boolean hasAnomaly){
        if(hasAnomaly) {
            return Collections.singletonList(flow);
        } else {
            return null;
        }
    }

    protected SequentialConstraint createSequentialConstraint(Flow flow){
        return new First2LastValidatingConstraint(flow);
    }

    protected void validateFlows(Flow flow){
        if(!(flow instanceof SingleFlow)){
            throw new InvalidArgumentException("This class only generate constraints for SingleFlow.");
        }
    }

    protected FlowNode getR2RelatedFlowNode(List<FlowNode> selectedFlowNodes, int pos){
        return selectedFlowNodes.get(pos + 1);
    }

    protected Collection<Flow> generateSuspiciousFlows(Flow flow, FlowNode pos1, FlowNode pos2){
        Flow suspicious = flow.split(pos1, pos2);
        return Collections.singleton(suspicious);
    }

    protected String getFirstNodeStats(List<OFFlowMod> r1) {
        StringBuilder sb = new StringBuilder();
        if(r1.size() != 1) {
            sb.append('(');
        }
        Iterator<OFFlowMod> it = r1.iterator();
        while(it.hasNext()) {
            OFFlowMod flowMod = it.next();
            long idx = this.ruleIndexer.getIndex(flowMod);
            String localStats = StatsExpressionConstraint.statsOf(idx);
            sb.append(localStats);
            if(it.hasNext()){ // add other rules' statistics
                sb.append('+');
            } else {
                break;
            }
        }
        if(r1.size() != 1) {
            sb.append(')');
        }
        return sb.toString();
    }

    protected String getOtherNodeStats(OFFlowMod r2){
        long idx = this.ruleIndexer.getIndex(r2);
        return StatsExpressionConstraint.statsOf(idx);
    }

    protected String buildEqualConstraintWithDeviation(String statsLeft, String statsRight){
        // ABS(stats(left) - stats(right)) <= (deviation * MAX(stats(left), stats(right)) + DOUBLE_COMPARISON_FIX)
        StringBuilder sb = new StringBuilder();
        sb.append("ABS(").append(statsLeft).append('-').append(statsRight).append(")");
        sb.append("<=");
        sb.append('(');
        sb.append(this.acceptedDeviation).append('*').append("MAX(").append(statsLeft).append(',').append(statsRight).append(')');
        sb.append('+');
        sb.append(DOUBLE_COMPARISON_FIX);
        sb.append(')');
        if(ignoreTimeoutIssues){
            sb.append(" || " + statsLeft + " < 0 || " + statsRight + " < 0");
        }
        return sb.toString();
    }
}
