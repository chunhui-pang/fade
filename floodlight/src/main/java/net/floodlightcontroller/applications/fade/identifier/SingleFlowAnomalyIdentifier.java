package net.floodlightcontroller.applications.fade.identifier;

import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.exception.InvalidArgumentException;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import net.floodlightcontroller.applications.fade.flow.singleflow.SingleFlow;
import net.floodlightcontroller.applications.fade.flow.singleflow.SingleFlowNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.List;

/**
 * implementation of {@link AnomalyIdentifier} for {@link SingleFlow}.
 */
public class SingleFlowAnomalyIdentifier implements AnomalyIdentifier {
    private static Logger logger = LoggerFactory.getLogger(SingleFlowAnomalyIdentifier.class);
    @Override
    public IRuleNode identifyAnomaly(List<Flow> flows) {
        if(flows.size() != 1){
            return null;
        }
        Flow flow = flows.get(0);
        if(flow instanceof SingleFlow){
            SingleFlow sf = (SingleFlow) flow;
            if (sf.length() == 3){
                FlowNode mid = sf.getFlowNode().get(1);
                logger.error("find anomaly of flow {} in rule {} at time {}", new Object[]{sf.getId(), mid, Calendar.getInstance().getTime()});
                return ((SingleFlowNode) mid).getRuleNode();
            } else {
                return null;
            }
        } else {
            throw new InvalidArgumentException("This implementation could only identify anomalies for SingleFlow");
        }
    }
}
