package net.floodlightcontroller.applications.fade.rule.enforcer;

import net.floodlightcontroller.applications.fade.util.ThreadPoolExcutionExceptionProtector;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.types.DatapathId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Implementation of {@link RuleEnforcer}.
 * It use a background thread to periodically install rules.
 * For a delayed rule, it would install it no before than the specified delay.
 *
 * Note, as for we periodically install rules, there induces an extra delay,
 * the delay should be tolerated in the implementation.
 * At the same time, the extra delay should be as small as possible,
 * and the awake interval of the background thread should be as short as possible.
 */
public class RuleEnforcerImpl implements RuleEnforcer {
    private static final Logger logger = LoggerFactory.getLogger(RuleEnforcer.class);
    private static final long DEFAULT_AWAKE_INTERVAL = 100L; // ms
    private static final long BEFORE_FIRST_ENFORCEMENT = 60*1000; //ms
    private ScheduledFuture<?> enforceTask = null;
    private IThreadPoolService threadPoolService;
    private long awakeInterval;
    // we use a bidirectional linked list with a sentinel instead of priority queue as the queue.
    // This is because almost all delayed rules have the same delay, and we could just add it to the tail of the list.
    // If this happens, we only cost O(1), but with priority queue, it cost O(lg(n)).
    private EnforceTaskInfos taskListHead, taskListTail;
    private Lock taskLock;
    private IOFSwitchService switchService;

    public RuleEnforcerImpl (IOFSwitchService switchService, IThreadPoolService threadPoolService, long awakeInterval ) {
        this.switchService = switchService;
        this.awakeInterval = awakeInterval;
        this.threadPoolService = threadPoolService;
        this.taskLock = new ReentrantLock();
        // the sentinel
        this.taskListHead = this.taskListTail = new EnforceTaskInfos(null, null, 0, null, null);
        this.enforceTask = null;
    }

    public RuleEnforcerImpl ( IOFSwitchService switchService, IThreadPoolService threadPoolService ) {
        this(switchService, threadPoolService, DEFAULT_AWAKE_INTERVAL);
    }

    private void installRule(OFFlowMod flowMod, DatapathId dpid) {
        IOFSwitch ofSwitch = this.switchService.getActiveSwitch(dpid);
        if(null == ofSwitch) {
            logger.error("could not install rule {} on switch {} for there is no active instance", flowMod, dpid);
        } else {
            ofSwitch.write(flowMod);
            if(logger.isDebugEnabled()) {
                logger.debug("enforced rule {} on switch {}", flowMod, dpid);
            }
        }
    }

    @Override
    public void addTask(List<OFFlowMod> rules, List<DatapathId> dpids) {
        if(rules.size() != dpids.size()){
            logger.error("could not install rules {} on switches {}, their length of not equal", rules, dpids);
        } else {
            Iterator<OFFlowMod> rit = rules.iterator();
            Iterator<DatapathId> dit = dpids.iterator();
            while(rit.hasNext()){
                this.installRule(rit.next(), dit.next());
            }
        }
    }

    @Override
    public void addDelayedTask(List<OFFlowMod> rules, List<DatapathId> dpids, long delay) {
        long enforceTime = Calendar.getInstance().getTimeInMillis() + delay;
        EnforceTaskInfos newInfos = new EnforceTaskInfos(rules, dpids, enforceTime, null, null);
        this.taskLock.lock();
        try {
            // check if the task could be simply added to the tail
            if(this.taskListTail.enforceTime <= enforceTime){
                newInfos.prev = this.taskListTail;
                this.taskListTail.next = newInfos;
                this.taskListTail = newInfos;
            } else {
                // find proper position to insert
                EnforceTaskInfos tmp = this.taskListTail;
                while(tmp.prev != null && tmp.prev.enforceTime > enforceTime){
                    tmp = tmp.prev;
                }
                newInfos.prev = tmp;
                newInfos.next = tmp.next;
                tmp.next.prev = newInfos;
                tmp.next = newInfos;
            }
        } finally {
            this.taskLock.unlock();
        }
        if(logger.isDebugEnabled()) {
            logger.debug("add delayed task, rules: {}, dpids: {}", rules, dpids);
        }
    }

    @Override
    public synchronized void startEnforce(long initDelay) {
        if(this.enforceTask == null){
            this.enforceTask = this.threadPoolService.getScheduledExecutor().scheduleWithFixedDelay(
                    new ThreadPoolExcutionExceptionProtector(new EnforceRunner()),
                    initDelay > 0 ? initDelay : BEFORE_FIRST_ENFORCEMENT, awakeInterval, TimeUnit.MILLISECONDS);
        }
    }

    private class EnforceRunner implements Runnable {
        private boolean showHello = false;
        @Override
        public void run() {
            if(!showHello){
                logger.info("RuleEnforcer is scheduled at the first time...");
                showHello = true;
            }
            long currentTime = Calendar.getInstance().getTimeInMillis();
            int numberOfTasks = 0;
            EnforceTaskInfos taskEnd = taskListHead, taskStart = null;
            taskLock.lock();
            try {
                taskStart = taskEnd.next;
                while(taskEnd.next != null && taskEnd.next.enforceTime <= currentTime){
                    taskEnd = taskEnd.next;
                }
                if(taskEnd.next != null){
                    taskEnd.next.prev = taskListHead;
                } else {
                    taskListTail = taskListHead;
                }
                taskListHead.next = taskEnd.next;
                // tasks are saved in [taskStart, taskEnd]
            } finally {
                taskLock.unlock();
            }
            if(taskEnd != taskListHead) { // non empty result
                while (taskStart != null && taskStart != taskEnd) {
                    RuleEnforcerImpl.this.addTask(taskStart.flowMods, taskStart.dpids);
                    taskStart = taskStart.next;
                }
                RuleEnforcerImpl.this.addTask(taskEnd.flowMods, taskEnd.dpids);
            }
        }
    }

    private class EnforceTaskInfos {
        private List<OFFlowMod> flowMods;
        private List<DatapathId> dpids;
        private long enforceTime;
        private EnforceTaskInfos prev;
        private EnforceTaskInfos next;

        public EnforceTaskInfos(List<OFFlowMod> flowMods, List<DatapathId> dpids, long enforceTime, EnforceTaskInfos prev, EnforceTaskInfos next){
            this.flowMods = flowMods;
            this.dpids = dpids;
            this.enforceTime = enforceTime;
            this.prev = prev;
            this.next = next;
        }
    }
}
