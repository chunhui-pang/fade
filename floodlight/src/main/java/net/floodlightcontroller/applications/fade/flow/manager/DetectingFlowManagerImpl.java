package net.floodlightcontroller.applications.fade.flow.manager;

import com.google.common.collect.Maps;
import net.floodlightcontroller.applications.fade.flow.Flow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of {@link DetectingFlowManager}.
 * In order to distinguish subflow from flows, we only use a field in {@link FlowInfo}
 * to record the number of flow nodes that have been released.
 * We release the flow after all its nodes are released.
 */
public class DetectingFlowManagerImpl implements DetectingFlowManager {
    private static final Logger logger = LoggerFactory.getLogger(DetectingFlowManagerImpl.class);
    private Map<Long, FlowInfo> detectingFlows;
    private Map<Flow, AtomicInteger> localizationRuns;
    private static final FlowInfo DEFAULT_INFO = new FlowInfo();

    public DetectingFlowManagerImpl(){
        this.detectingFlows = Maps.newConcurrentMap();
        this.localizationRuns = Maps.newConcurrentMap();
    }

    @Override
    public boolean tryAddFlow(Flow flow) {
        FlowInfo flowInfo = new FlowInfo(flow);
        FlowInfo prevVal = this.detectingFlows.putIfAbsent(flow.getId(), flowInfo);
        this.localizationRuns.putIfAbsent(flow, new AtomicInteger(0));
        return prevVal == null;
    }

    @Override
    public boolean isBeingDetected(Flow flow) {
        return this.detectingFlows.containsKey(flow.getId());
    }

    @Override
    public int addLocalizationRun(Flow flow) {
        if(this.localizationRuns.containsKey(flow)){
            return this.localizationRuns.get(flow).addAndGet(1);
        } else {
            logger.warn("the flow {} isn't in localization phase", flow);
            this.localizationRuns.putIfAbsent(flow, new AtomicInteger(1));
            return 1;
        }
    }

    @Override
    public void releaseDetectingFlow(Flow flow) {
        this.localizationRuns.remove(flow);
        FlowInfo info = this.detectingFlows.get(flow.getId());
        info.remainingNodes -= (flow.size() * flow.length());
        if(info.remainingNodes <= 0) {
            // remove only when all nodes are released
            logger.info("release detecting flow {}", flow.getId());
            this.detectingFlows.remove(flow.getId());
        } else {
            logger.debug("detecting flow {} still has {} nodes to be released", info.flow, info.remainingNodes);
        }
    }

    @Override
    public void releaseFlowNodes(Flow flow, int numOfNodes) {
        this.detectingFlows.getOrDefault(flow.getId(), DEFAULT_INFO).remainingNodes -= numOfNodes;
    }

    private static class FlowInfo {
        private Flow flow;
        private int remainingNodes;

        public FlowInfo(){
            this.flow = null;
            this.remainingNodes = 0;
        }

        public FlowInfo(Flow flow){
            this.flow = flow;
            this.remainingNodes = flow.size()*flow.length();
        }
    }
}
