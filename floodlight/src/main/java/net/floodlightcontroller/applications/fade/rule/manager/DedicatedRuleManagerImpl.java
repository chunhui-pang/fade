package net.floodlightcontroller.applications.fade.rule.manager;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.util.ThreadPoolExcutionExceptionProtector;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * implementation of dedicated rule manager
 */
public class DedicatedRuleManagerImpl implements DedicatedRuleManager {
    private static final Logger logger = LoggerFactory.getLogger(DedicatedRuleManagerImpl.class);
    private Map<Long, IRuleNode> dedicatedRules;
    private AtomicLong dedicateRuleCounter = new AtomicLong(0);
    private IThreadPoolService threadPoolService;
    private ScheduledFuture<?> usageDumperTask;
    private static final long BEFORE_FIRST_USAGE_DUMP = 60*1000;  /* ms */
    private static final long USAGE_DUMP_INTERVAL = 500;          /* ms */

    public DedicatedRuleManagerImpl(IThreadPoolService threadPoolService){
        this.dedicatedRules = Maps.newConcurrentMap();
        this.threadPoolService = threadPoolService;
        this.usageDumperTask = null;
    }

    @Override
    public void addDedicatedRule(IRuleNode ruleNode, long idx) {
        IRuleNode res = this.dedicatedRules.putIfAbsent(idx, ruleNode);
        if(res == ruleNode) { // fails
            logger.error("the index {} has been used by rule {}", idx, this.dedicatedRules.get(idx));
        } else {
            this.dedicateRuleCounter.incrementAndGet();
        }
    }

    @Override
    public void removeDedicatedRule(IRuleNode ruleNode) {
        // very low efficient
        List<Long> indices = Lists.newLinkedList();
        for(Map.Entry<Long, IRuleNode> entry : this.dedicatedRules.entrySet()){
            if(entry.getValue().equals(ruleNode)){
                indices.add(entry.getKey());
            }
        }
        for(Long idx : indices) {
            this.dedicatedRules.remove(idx);
        }
    }

    @Override
    public void removeDedicatedRule(long idx) {
        IRuleNode res = this.dedicatedRules.remove(idx);
        if(res == null){
            logger.warn("the index {} hasn't been associated with any rule", idx);
        }
    }

    @Override
    public IRuleNode getDedicatedRule(long index) {
        return this.dedicatedRules.get(index);
    }

    @Override
    public synchronized void dumpUsage(long initWaiting) {
        if (this.usageDumperTask == null) {
            this.usageDumperTask = this.threadPoolService.getScheduledExecutor().scheduleWithFixedDelay(
                    new ThreadPoolExcutionExceptionProtector(new DedicatedRuleUsageDumper()),
                    initWaiting > 0 ? initWaiting : BEFORE_FIRST_USAGE_DUMP,
                    USAGE_DUMP_INTERVAL, TimeUnit.MILLISECONDS);
        }
    }

    private class DedicatedRuleUsageDumper implements Runnable {
        private boolean showHello = false;
        @Override
        public void run() {
            if(showHello) {
                logger.info("DedicatedRule Usage Dumper is scheduled at the first time...");
                showHello = true;
            }
            logger.info("usage of dedicated flow rules, current size: {}, all usage: {}", dedicatedRules.size(), dedicateRuleCounter.get());
        }
    }
}
