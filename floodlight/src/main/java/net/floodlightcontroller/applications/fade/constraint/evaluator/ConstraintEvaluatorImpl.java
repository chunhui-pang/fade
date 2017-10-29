package net.floodlightcontroller.applications.fade.constraint.evaluator;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import net.floodlightcontroller.applications.fade.constraint.Constraint;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.stats.StatsContext;
import net.floodlightcontroller.applications.fade.util.ThreadPoolExcutionExceptionProtector;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * An implementation of {@link ConstraintEvaluator}.
 * We evaluate constraint every seconds (the interval could be customized), and generate suspicious flows.
 */
public class ConstraintEvaluatorImpl implements ConstraintEvaluator {
    private static final Logger logger = LoggerFactory.getLogger(ConstraintEvaluatorImpl.class);
    private static final long BEFORE_FIRST_EVALUATION = 60*1000;
    private static final long DEFAULT_AWAKE_INTERVAL = 1000;
    // in ms
    private long awakeInterval;

    private StatsContext statsContext;
    private PriorityQueue<Constraint> constraints;
    private Map<Constraint, Long> evalTimes;
    private ReentrantLock evalLock;

    private List<List<Flow>> suspiciousFlows;
    private List<List<Flow>> passedSuspicousFlows;
    private ReentrantLock flowLock;

    private IThreadPoolService threadPoolService;
    private ScheduledFuture<?> evalTask;

    public ConstraintEvaluatorImpl(StatsContext statsContext, IThreadPoolService threadPoolService){
        this(statsContext, threadPoolService, DEFAULT_AWAKE_INTERVAL);
    }

    public ConstraintEvaluatorImpl(StatsContext statsContext, IThreadPoolService threadPoolService, long awakeInterval){
        this.evalLock = new ReentrantLock();
        this.evalTimes = Maps.newHashMap();
        this.constraints = new PriorityQueue<>(Comparator.comparingLong(constraint -> evalTimes.getOrDefault(constraint, 0L)));
        this.statsContext = statsContext;
        this.threadPoolService = threadPoolService;
        this.suspiciousFlows = Lists.newArrayList();
        this.passedSuspicousFlows = Lists.newArrayList();
        this.flowLock = new ReentrantLock();
        this.awakeInterval = awakeInterval;
        this.evalTask = null;
    }

    @Override
    public void addConstraint(Constraint constraint, long evalDelay) {
        this.evalTimes.put(constraint, Calendar.getInstance().getTimeInMillis() + evalDelay);
        this.evalLock.lock();
        try {
            this.constraints.add(constraint);
        } finally {
            this.evalLock.unlock();
        }
    }

    @Override
    public List<List<Flow>> pollSuspiciousFlows(int limit) {
        List<List<Flow>> result = null;
        this.flowLock.lock();
        try{
            if(limit <= 0 || limit >= this.suspiciousFlows.size()){
                result = suspiciousFlows;
                this.suspiciousFlows = Lists.newArrayList();
            } else {
                List<List<Flow>> tmp = this.suspiciousFlows.subList(0, limit);
                result = Lists.newArrayList(tmp);
                tmp.clear();
            }
        } finally {
            this.flowLock.unlock();
        }
        return result;
    }

    @Override
    public List<List<Flow>> pollPassedSuspiciousFlows(int limit) {
        List<List<Flow>> result = null;
        this.flowLock.lock();
        try{
            if(limit <= 0 || limit >= this.passedSuspicousFlows.size()){
                result = this.passedSuspicousFlows;
                this.passedSuspicousFlows = Lists.newArrayList();
            } else {
                List<List<Flow>> tmp = this.passedSuspicousFlows.subList(0, limit);
                result = Lists.newArrayList(tmp);
                tmp.clear();
            }
        } finally {
            this.flowLock.unlock();
        }
        return result;
    }

    @Override
    public void startEvaluating( long initDelay ) {
        this.evalTask = this.threadPoolService.getScheduledExecutor().scheduleWithFixedDelay(
                new ThreadPoolExcutionExceptionProtector(new ConstraintEvalRunnable()),
                initDelay > 0 ? initDelay : BEFORE_FIRST_EVALUATION,
                awakeInterval,
                TimeUnit.MILLISECONDS);
    }

    private class ConstraintEvalRunnable implements Runnable {
        private boolean showHello = false;
        @Override
        public void run() {
            if(!showHello){
                logger.info("constraint evaluator is schedule at the first time");
                showHello = true;
            }
            long currentTime = Calendar.getInstance().getTimeInMillis();
            List<Constraint> timeoutCons = Lists.newArrayList();
            // retrieving timeout constraints
            evalLock.lock();
            try{
                while(constraints.size() != 0 && evalTimes.get(constraints.element()) <= currentTime){
                    timeoutCons.add(constraints.poll());
                }
            } finally {
                evalLock.unlock();
            }
            // evaluating
            List<List<Flow>> suspicious = Lists.newArrayList();
            List<List<Flow>> passedSuspicious = Lists.newArrayList();
            for(Constraint cons : timeoutCons){
                boolean evalResult = cons.validate(statsContext);
                if(!evalResult){
                    if(cons.getSuspiciousFlow() != null) {
                        suspicious.add(Lists.newArrayList(cons.getSuspiciousFlow()));
                    }
                } else {
                    if(cons.getPassedSuspiciousFlow() != null){
                        passedSuspicious.add(Lists.newArrayList(cons.getPassedSuspiciousFlow()));
                    }
                }
                logger.info("constraint {} was evaluated, result is {}", cons, evalResult);
            }
            // add to suspicious flows
            if(suspicious.size() != 0 || passedSuspicious.size() != 0){
                flowLock.lock();
                try{
                    suspiciousFlows.addAll(suspicious);
                    passedSuspicousFlows.addAll(passedSuspicious);
                } finally {
                    flowLock.unlock();
                }
            }
        }
    }
}
