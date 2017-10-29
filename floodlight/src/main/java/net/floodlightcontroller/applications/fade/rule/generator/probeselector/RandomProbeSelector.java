package net.floodlightcontroller.applications.fade.rule.generator.probeselector;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import net.floodlightcontroller.applications.fade.rule.generator.ProbeSelector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * A probe selector that random choose a set of probes
 */
public class RandomProbeSelector implements ProbeSelector {
    private static final Logger logger = LoggerFactory.getLogger(RandomProbeSelector.class);
    private static final int[] numOfIntermediateProbes = {
            /* 00 - 03 */ 0, 0, 0, 0,
            /* 04 - 08 */ 1, 1, 1, 1, 1,
            /* 09 - 13 */ 2, 2, 2, 2, 2,
            /* 14 - 21 */ 3, 3, 3, 3, 3, 3, 3, 3,
            /* 22 - 32 */ 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4};
    private static final int MIN_FLOW_LENGTH = 3;
    private Random random = new SecureRandom();

    @Override
    public List<FlowNode> selectProbes(Flow flow) {
        List<? extends FlowNode> flowNodes = flow.getFlowNode();
        if(flowNodes.size() < 2) {
            return Collections.emptyList();
        }
        int virtualLen = Math.min(flowNodes.size(), numOfIntermediateProbes.length);
        int numOfIntermediateProbe = this.numOfIntermediateProbes[virtualLen];
        // random between [0, size - 3], the add 1, [1, size - 2]
        int maxProbe = flowNodes.size() - 2;
        Set<Integer> selectedIndices = Sets.newHashSet();
        while(numOfIntermediateProbe > 0) {
            // adjust to [1, size - 2]k
            int pos = random.nextInt(maxProbe) + 1;
            if(selectedIndices.add(pos)){ // inserted
                numOfIntermediateProbe--;
            }
        }
        List<FlowNode> probes = Lists.newArrayList();
        // the first and the last must be selected
        probes.add(flowNodes.get(0));
        selectedIndices.forEach(idx -> {
            FlowNode node = flowNodes.get(idx);
            probes.add(node);
        });
        probes.add(flowNodes.get(flowNodes.size()-1));
        if(logger.isDebugEnabled()) {
            logger.debug("select probes for flow {}, probes are {}", flow, probes);
        }
        return probes;
    }
}
