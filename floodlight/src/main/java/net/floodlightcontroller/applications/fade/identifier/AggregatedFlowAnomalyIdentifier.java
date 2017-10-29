package net.floodlightcontroller.applications.fade.identifier;

import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.constraint.generator.AggregatedFlowConstraintGenerator;
import net.floodlightcontroller.applications.fade.exception.InvalidArgumentException;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import net.floodlightcontroller.applications.fade.flow.aggregatedflow.AggregatedFlow;
import net.floodlightcontroller.applications.fade.flow.aggregatedflow.AggregatedFlowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.List;

/**
 * Implementation for {@link AnomalyIdentifier} for {@link AggregatedFlow}.
 */
public class AggregatedFlowAnomalyIdentifier implements AnomalyIdentifier {
    private static final Logger logger = LoggerFactory.getLogger(AggregatedFlowConstraintGenerator.class);
    @Override
    public IRuleNode identifyAnomaly(List<Flow> flows) {
        if(flows.size() != 1){
            return null;
        }
        Flow flow = flows.get(0);
        if (flow instanceof AggregatedFlow) {
            AggregatedFlow af = (AggregatedFlow) flow;
            if (1 == af.size() && 3 == af.length()){
                FlowNode mid = af.getFlowNode().get(1);
                AggregatedFlowNode afn = (AggregatedFlowNode) mid;
                logger.error("find anomaly of flow {} in rule {} at time {}", new Object[]{af.getId(), mid, Calendar.getInstance().getTime()});
                return afn.getRuleNodes().get(0);
            } else {
                return null;
            }
        } else {
            throw new InvalidArgumentException("This implementation could only identify anomalies for AggregatedFlow");
        }
    }
}
