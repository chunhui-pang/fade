package net.floodlightcontroller.applications.fade.constraint.generator;

import com.google.common.collect.Lists;
import net.floodlightcontroller.applications.fade.constraint.AllValidatingConstraint;
import net.floodlightcontroller.applications.fade.constraint.First2LastValidatingConstraint;
import net.floodlightcontroller.applications.fade.constraint.SequentialConstraint;
import net.floodlightcontroller.applications.fade.exception.InvalidArgumentException;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import net.floodlightcontroller.applications.fade.flow.aggregatedflow.AggregatedFlow;
import net.floodlightcontroller.applications.fade.rule.generator.AggregatedFlowRuleGeneratorImpl;
import net.floodlightcontroller.applications.fade.rule.generator.RuleGenerateResult;
import net.floodlightcontroller.applications.fade.rule.manager.DedicatedRuleManager;
import net.floodlightcontroller.applications.fade.rule.manager.RuleIndexer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * constraint generator for {@link AggregatedFlow}.
 * Unlike {@link SingleFlowConstraintGenerator}, we use packet count as the threshold.
 * This is because the affect of anomalies may become very small in aggregated flows so that we even cannot notice it.
 *
 * @implNote In contrast to generateConstraint in SingleFlowConstraintGenerator, the suspicious flows and the equal constraints are different.
 */
public class AggregatedFlowConstraintGenerator extends SingleFlowConstraintGenerator {
    private static final Logger logger = LoggerFactory.getLogger(AggregatedFlowConstraintGenerator.class);

    private static final int SLICE_RATIO = 4;
    private static final long DEFAULT_ALLOWED_COUNTER_DEVIATION = 10;
    private long allowedCounterDeviation;

    public AggregatedFlowConstraintGenerator(DedicatedRuleManager dedicatedRuleManager, RuleIndexer ruleIndexer, long allowedCounterDeviation, boolean ignoreTimeoutIssues) {
        super(dedicatedRuleManager, ruleIndexer, 0.0, ignoreTimeoutIssues);
        this.allowedCounterDeviation = allowedCounterDeviation;
    }

    @Override
    protected void validateFlows(Flow flow) {
        if(!(flow instanceof AggregatedFlow)) {
            throw new InvalidArgumentException("This class only generate constraints for AggregatedFlow.");
        }
    }


    @Override
    protected SequentialConstraint createSequentialConstraint(Flow flow){
        // return new AllValidatingConstraint(flow);
        return new First2LastValidatingConstraint(flow);
    }

    @Override
    protected Collection<Flow> generateSuspiciousFlows(Flow flow, FlowNode pos1, FlowNode pos2) {
        AggregatedFlow af = (AggregatedFlow) flow;
        if(flow.size() == 0) {
            return null;
        } else if(flow.size() == 1){
            return super.generateSuspiciousFlows(flow, pos1, pos2);
        } else if(flow.size() <= SLICE_RATIO) { // split to 1
            List<Flow> result = Lists.newArrayList();
            List<AggregatedFlow> slice = af.slice(1);
            while (slice.size() == 2) {
                result.add(slice.get(0));
                slice = slice.get(1).slice(1);
            }
            result.add(slice.get(0));
            return result;
        } else {
            int sliceSize = af.size() / SLICE_RATIO;
            List<Flow> result = Lists.newArrayList();
            List<AggregatedFlow> slice = af.slice(sliceSize);
            while (slice.size() == 2) {
                result.add(slice.get(0));
                slice = slice.get(1).slice(sliceSize);
            }
            result.add(slice.get(0));
            return result;
        }
    }

    @Override
    protected String buildEqualConstraintWithDeviation(String statsLeft, String statsRight) {
        // ABS(stats(left) - stats(right)) <= (deviation * MAX(stats(left), stats(right)) + DOUBLE_COMPARISON_FIX)
        StringBuilder sb = new StringBuilder();
        sb.append("(").append(statsLeft).append('-').append(statsRight).append(")").append("<=").append(this.allowedCounterDeviation);
        if(this.ignoreTimeoutIssues){
            sb.append(" || " + statsLeft + " < 0 || " + statsRight + " < 0");
        }
        return sb.toString();
    }
}
