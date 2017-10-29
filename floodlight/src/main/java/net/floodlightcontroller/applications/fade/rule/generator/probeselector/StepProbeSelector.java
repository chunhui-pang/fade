package net.floodlightcontroller.applications.fade.rule.generator.probeselector;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import net.floodlightcontroller.applications.fade.rule.generator.ProbeSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A probe selector that select probes from every other flow nodes.
 * For example, for a flow with five nodes: 1, 2, 3, 4, 5.
 * It first select {1} 2 {3} 4 {5} as probes, and if no anomaly is found, then it select 1 {2} 3 {4} 5
 *
 */
public class StepProbeSelector implements ProbeSelector {
    private static Logger logger = LoggerFactory.getLogger(StepProbeSelector.class);
    private Map<Long, Boolean> detectionFromHead = Maps.newConcurrentMap();

    @Override
    public List<FlowNode> selectProbes(Flow flow) {
        List<? extends FlowNode> flowNodes = flow.getFlowNode();
        if(flowNodes.size() < 2)
            return Collections.emptyList();
        List<FlowNode> probes = Lists.newArrayList();
        Iterator<? extends FlowNode> it = flowNodes.iterator();
        if(this.detectionFromHead.getOrDefault(flow.getId(), true)){
            this.detectionFromHead.put(flow.getId(), false);
        } else {
            if(it.hasNext()) {
                it.next();
            }
            this.detectionFromHead.put(flow.getId(), true);
        }
        while(it.hasNext()){
            probes.add(it.next());
            if(it.hasNext())
                it.next();
        }
        logger.debug("select probes for flow {}, probes are {}", flow, probes);
        return probes;
    }
}
