package net.floodlightcontroller.applications.fade;

import com.google.common.collect.Lists;
import net.floodlightcontroller.applications.appmodule.concretetopology.IConcreteTopologyService;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleGraphService;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.constraint.Constraint;
import net.floodlightcontroller.applications.fade.constraint.evaluator.ConstraintEvaluator;
import net.floodlightcontroller.applications.fade.constraint.generator.ConstraintGenerator;
import net.floodlightcontroller.applications.fade.factory.AggregatedFlowFadeFactory;
import net.floodlightcontroller.applications.fade.factory.FadeContext;
import net.floodlightcontroller.applications.fade.factory.FadeFactory;
import net.floodlightcontroller.applications.fade.factory.SingleFlowFadeFactory;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.flow.FlowSelector;
import net.floodlightcontroller.applications.fade.flow.manager.DetectingFlowManager;
import net.floodlightcontroller.applications.fade.identifier.AnomalyIdentifier;
import net.floodlightcontroller.applications.fade.rule.enforcer.RuleEnforcer;
import net.floodlightcontroller.applications.fade.rule.generator.ProbeSelector;
import net.floodlightcontroller.applications.fade.rule.generator.RuleGenerateResult;
import net.floodlightcontroller.applications.fade.rule.generator.RuleGenerator;
import net.floodlightcontroller.applications.fade.rule.generator.probeselector.RandomProbeSelector;
import net.floodlightcontroller.applications.fade.rule.generator.probeselector.StepProbeSelector;
import net.floodlightcontroller.applications.fade.rule.manager.RuleIndexer;
import net.floodlightcontroller.applications.fade.util.TagManager;
import net.floodlightcontroller.applications.fade.util.TagRunOutException;
import net.floodlightcontroller.applications.fade.util.ThreadPoolExcutionExceptionProtector;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.util.AppCookie;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.apache.commons.lang3.StringUtils;
import org.projectfloodlight.openflow.protocol.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.*;

/**
 * The controller of forwarding anomaly detection.
 * It controls the progress of detections.
 */
public class FADEController implements IFloodlightModule, IOFMessageListener {
    private static final Logger logger = LoggerFactory.getLogger(FADEController.class);
    private static final String APP_NAME = "fade";
    private static final int APP_ID = 0x100;
    // floodlight service dependencies
    private IFloodlightProviderService floodlightProviderService;
    private IOFSwitchService ofSwitchService = null;
    private IThreadPoolService threadPoolService = null;
    private IRuleGraphService ruleGraphService = null;
    private IConcreteTopologyService concreteTopologyService = null;

    // service logic dependencies
    private FadeFactory fadeFactory = null;
    private FadeContext fadeContext = null;
    private ScheduledFuture<?> detectTask = null;
    private ScheduledFuture<?> suspiciousPollTask = null;

    private enum DetectionMode {
        SINGLE_FLOW("SingleFlow"), AGGREGATED_FLOW("AggregatedFlow");
        private String configVal;
        DetectionMode(String configVal){ this.configVal = configVal; }
        String getConfigVal() { return this.configVal; }
    }
    private enum ProbeSelectMode {
        RANDOM("random"), STEP("step");
        private String configVal;
        ProbeSelectMode(String configVal){ this.configVal = configVal; }
        String getConfigVal() { return this.configVal; }
    }
    // configuration names
    private static final String DETECTION_MODE = "detectionMode";
    private static final String DETECTION_DURATION = "detectionDuration";
    private static final String DETECTION_INTERVAL = "detectionInterval";
    private static final String SWITCH_TCAM_RESERVATION = "switchTcamReservations";
    private static final String DETECTION_TAG_RATIO = "detectionTagRatio";
    private static final String NETWORK_START_TIME = "networkStartTime";
    private static final String MAX_NETWORK_DELAY = "maxNetworkDelay";
    private static final String ACCEPTED_STATS_DEVIATION = "acceptedStatsDeviation";
    private static final String ACCEPTED_COUNTER_DEVIATION = "acceptedCounterDeviation";
    private static final String MAX_LOCALIZATION_RUN = "maxLocalizationRun";
    private static final String POLL_SUSPICIOUS_INTERVAL = "pollSuspiciousInterval";
    private static final String PROBE_SELECT_MODE = "probeSelectMode";
    /* whether if we ignore errors caused by receive no flow statistics */
    private static final String IGNORE_TIMEOUT_ISSUES = "ignoreTimeoutIssues";
    // default configuration values
    private DetectionMode detectionMode = DetectionMode.SINGLE_FLOW;  // single flow detection
    private long detectionDuration = 1;                               // 1s    hard timeout
    private long detectionInterval = 20;                              // 20s   20s per time
    private List<Long> switchTcamReservations = null;                 // unlimited resources
    private double detectionTagRatio = 0.8;                           // detection/confusing = 4:1
    private long networkStartTime = 60;                               // 60s to start
    private float maxNetworkDelay = 0.5F;                             // 0.5, in seconds
    private double acceptedStatsDeviation = .15F;                     // the deviation of statistics is up to 15%
    private long acceptedCounterDeviation = 10;                       // the deviation in aggregated mode is up to 10
    private int maxLocalizationRun = 10;                              // the maximum number of localization run is 10
    private double pollSuspiciousInterval = .5;                       // 0.5s
    private ProbeSelectMode probeSelectMode = ProbeSelectMode.RANDOM; // random
    private boolean ignoreTimeoutIssues = true;                       // true

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleServices() {
        return null;
    }

    @Override
    public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
        return null;
    }

    @Override
    public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
        return Lists.newArrayList(
                IOFSwitchService.class,
                IThreadPoolService.class,
                IRuleGraphService.class,
                IFloodlightProviderService.class,
                IConcreteTopologyService.class);
    }

    @Override
    public String getName() {
        return APP_NAME;
    }

    public static int getAppId(){
        return APP_ID;
    }

    @Override
    public Command receive(IOFSwitch sw, OFMessage msg, FloodlightContext cnt) {
        try {
            switch (msg.getType()) {
                case FLOW_REMOVED:
                    OFFlowRemoved flowRemoved = (OFFlowRemoved) msg;
                    if (AppCookie.extractApp(((OFFlowRemoved) msg).getCookie()) == APP_ID) {
                        RuleIndexer ruleIndexer = fadeContext.getRuleIndexer();
                        this.fadeContext.getStatsCollector().addStatsRecord(ruleIndexer.getIndex(flowRemoved),
                                flowRemoved.getPacketCount().getValue(), flowRemoved.getByteCount().getValue());
                        if(logger.isDebugEnabled()) {
                            logger.debug("receive OFFlowRemoved message {} from switch {}", msg, sw.getId());
                        }
                    }
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            logger.error("handling OpenFlow message fails, message: {}, exception: {}", msg, e);
        }

        return Command.CONTINUE;
    }

    @Override
    public boolean isCallbackOrderingPrereq(OFType type, String name) {
        return (type == OFType.FLOW_REMOVED);
    }

    @Override
    public boolean isCallbackOrderingPostreq(OFType type, String name) {
        return false;
    }

    @Override
    public void init(FloodlightModuleContext context) throws FloodlightModuleException {
        this.ofSwitchService = context.getServiceImpl(IOFSwitchService.class);
        this.threadPoolService = context.getServiceImpl(IThreadPoolService.class);
        this.floodlightProviderService = context.getServiceImpl(IFloodlightProviderService.class);
        this.ruleGraphService = context.getServiceImpl(IRuleGraphService.class);
        this.concreteTopologyService = context.getServiceImpl(IConcreteTopologyService.class);
        Map<String, String> configs = context.getConfigParams(this);
        this.parseConfigs(configs);
        this.initFadeDetection();
        this.floodlightProviderService.addOFMessageListener(OFType.FLOW_REMOVED, this);
    }

    @Override
    public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
        this.fadeContext.getRuleEnforcer().startEnforce(this.networkStartTime*1000);
        this.fadeContext.getConstraintEvaluator().startEvaluating(this.networkStartTime*1000);
        this.suspiciousPollTask = this.threadPoolService.getScheduledExecutor().scheduleWithFixedDelay(
                new ThreadPoolExcutionExceptionProtector(new SuspiciousFlowPollRunner()),
                (long)((this.networkStartTime + this.pollSuspiciousInterval)*1000),
                (long)(this.pollSuspiciousInterval*1000),
                TimeUnit.MILLISECONDS);
        this.detectTask = this.threadPoolService.getScheduledExecutor().scheduleWithFixedDelay(
                new ThreadPoolExcutionExceptionProtector(new DetectionRunner()),
                this.networkStartTime,
                this.detectionInterval,
                TimeUnit.SECONDS);
        this.fadeContext.getDedicatedRuleManager().dumpUsage(this.networkStartTime*1000);
        logger.info("module FADEController[mode={}] started :)", this.detectionMode.getConfigVal());
    }

    private void parseConfigs(Map<String, String> configs) throws FloodlightModuleException {
        for(String configName : configs.keySet()){
            String configVal = configs.get(configName);
            boolean parseError = false;
            if(StringUtils.isBlank(configVal)){
                continue;
            }
            switch (configName){
                case DETECTION_MODE:
                    this.detectionMode = null;
                    for(DetectionMode mode : DetectionMode.values()){
                        if(mode.getConfigVal().toLowerCase().equals(configVal.toLowerCase())){
                            this.detectionMode = mode;
                            break;
                        }
                    }
                    parseError = (null == this.detectionMode);
                    break;
                case DETECTION_DURATION:
                    try {
                        this.detectionDuration = Long.parseLong(configVal);
                        parseError = (this.detectionDuration <= 0);
                    } catch (NumberFormatException e) {
                        parseError = true;
                    }
                    break;
                case DETECTION_INTERVAL:
                    try {
                        this.detectionInterval = Long.parseLong(configVal);
                        parseError = (this.detectionInterval <= 0);
                    } catch (NumberFormatException e) {
                        parseError = true;
                    }
                    break;
                case SWITCH_TCAM_RESERVATION:
                    // this configuration will be verified in runtime
                    this.switchTcamReservations = Lists.newArrayList();
                    String[] reserves = configVal.split("\\s+");
                    try {
                        for (String reserve : reserves) {
                            this.switchTcamReservations.add(Long.parseLong(reserve));
                        }
                    } catch (NumberFormatException e) {
                        parseError = true;
                    }
                    break;
                case DETECTION_TAG_RATIO:
                    try {
                        this.detectionTagRatio = Double.parseDouble(configVal);
                        parseError = (this.detectionTagRatio <= 0 || this.detectionTagRatio >= 1.0);
                    } catch (NumberFormatException e) {
                        parseError = true;
                    }
                    break;
                case NETWORK_START_TIME:
                    try {
                        this.networkStartTime = Long.parseLong(configVal);
                        parseError = (this.networkStartTime <= 0);
                    } catch (NumberFormatException e) {
                        parseError = true;
                    }
                    break;
                case MAX_NETWORK_DELAY:
                    try {
                        this.maxNetworkDelay = Float.parseFloat(configVal);
                        parseError = (this.maxNetworkDelay <= 0);
                    } catch (NumberFormatException e) {
                        parseError = true;
                    }
                    break;
                case ACCEPTED_STATS_DEVIATION:
                    try{
                        this.acceptedStatsDeviation = Double.parseDouble(configVal);
                        parseError = (this.acceptedStatsDeviation < 0 || this.acceptedStatsDeviation >= 1);
                    } catch (NumberFormatException e){
                        parseError = true;
                    }
                    break;
                case ACCEPTED_COUNTER_DEVIATION:
                    try{
                        this.acceptedCounterDeviation = Long.parseLong(configVal);
                        parseError = (this.acceptedCounterDeviation < 0);
                    } catch (NumberFormatException e){
                        parseError = true;
                    }
                    break;
                case MAX_LOCALIZATION_RUN:
                    try{
                      this.maxLocalizationRun = Integer.parseInt(configVal);
                      parseError = (this.maxLocalizationRun <= 0);
                    } catch (NumberFormatException e){
                        parseError = true;
                    }
                    break;
                case POLL_SUSPICIOUS_INTERVAL:
                    try {
                        this.pollSuspiciousInterval = Double.parseDouble(configVal);
                        parseError = (this.pollSuspiciousInterval <= 0 && this.pollSuspiciousInterval >= 1.0D);
                    } catch (NumberFormatException e){
                        parseError = true;
                    }
                    break;
                case PROBE_SELECT_MODE:
                    this.probeSelectMode = null;
                    for(ProbeSelectMode mode : ProbeSelectMode.values()) {
                        if (mode.getConfigVal().toLowerCase().equals(configVal.toLowerCase())) {
                            this.probeSelectMode = mode;
                            break;
                        }
                    }
                    parseError = (this.probeSelectMode == null);
                    break;
                case IGNORE_TIMEOUT_ISSUES:
                    this.ignoreTimeoutIssues = Boolean.parseBoolean(configVal);
                    parseError = false;
                    break;
                default:
                    throw new FloodlightModuleException("unrecognized configuration: " + configName);
            }
            if(parseError) {
                throw new FloodlightModuleException(String.format("unrecognized/illegal value %s for parameter %s.", configVal, configName));
            }
        }
    }

    private void initFadeDetection( ) throws FloodlightModuleException {
        OFFactory ofFactory = OFFactories.getFactory(OFVersion.OF_10);
        ProbeSelector probeSelector = null;
        switch(this.probeSelectMode){
            case RANDOM:
                probeSelector = new RandomProbeSelector();
                break;
            case STEP:
                probeSelector = new StepProbeSelector();
                break;
            default:
                break;
        }
        switch (this.detectionMode) {
            case AGGREGATED_FLOW:
                this.fadeFactory = new AggregatedFlowFadeFactory(APP_ID, APP_NAME,
                        this.acceptedCounterDeviation, this.detectionTagRatio, this.ignoreTimeoutIssues,
                        ofFactory, this.ofSwitchService, this.ruleGraphService, this.concreteTopologyService, this.threadPoolService,
                        probeSelector);
                break;
            case SINGLE_FLOW:
            default:
                this.fadeFactory = new SingleFlowFadeFactory(APP_ID, APP_NAME,
                        this.acceptedStatsDeviation, this.detectionTagRatio, this.ignoreTimeoutIssues,
                        ofFactory, this.ofSwitchService, this.ruleGraphService, this.concreteTopologyService, this.threadPoolService,
                        probeSelector);
                break;
        }
        this.fadeContext = this.fadeFactory.createFadeContext();
        this.detectTask = null;
        this.suspiciousPollTask = null;
    }

    private List<Flow> detectFlows(List<Flow> flows, RuleGenerator ruleGenerator,
                             ConstraintGenerator constraintGenerator, boolean anomalyIdentificationPhase,
                             RuleEnforcer ruleEnforcer, ConstraintEvaluator constraintEvaluator) {
        int detectionDuration = (int) FADEController.this.detectionDuration;
        long maximumNetworkDelay = (long) Math.ceil(this.maxNetworkDelay * 1000);
        // the timeout of r2 is duration + 3*delay. with 2 message (one rule install, one flow removed) delay
        // as the sake of robustness, we add extra 2 message delay
        final long evalDelay = 1000 *
                (detectionDuration +
                        (long)Math.ceil(3.0D * this.maxNetworkDelay) +
                        (long) Math.ceil(4 * this.maxNetworkDelay)); // milliseconds
        List<Flow> unDetected = Lists.newArrayList();
        for(int idx = 0; idx < flows.size(); idx++){
            Flow flow = flows.get(idx);
            try {
                RuleGenerateResult genResult = ruleGenerator.generateDedicatedRules(flow, detectionDuration, maximumNetworkDelay);
                if (genResult != null) {
                    List<Constraint> constraints = constraintGenerator.generateConstraint(flow, genResult, anomalyIdentificationPhase);
                    for (Constraint constraint : constraints)
                        constraintEvaluator.addConstraint(constraint, evalDelay);
                    ruleEnforcer.addTask(genResult.getR2Rules(), genResult.getR2Switches());
                    ruleEnforcer.addDelayedTask(genResult.getR1Rules(), genResult.getR1Switches(), maximumNetworkDelay);
                } else {
                    unDetected.add(flow);
                }
            } catch (TagRunOutException e) {
                logger.warn("all tags are run out, cancel remaining flows in current detection");
                unDetected.addAll(flows.subList(idx, flows.size()));
                break;
            }
        }
        return unDetected;
    }

    private class DetectionRunner implements Runnable {
        private static final boolean IN_ANOMALY_IDENTIFICATION_PHASE = false;
        private FlowSelector flowSelector = fadeContext.getFlowSelector();
        private RuleGenerator ruleGenerator = fadeContext.getRuleGenerator();
        private ConstraintGenerator constraintGenerator = fadeContext.getConstraintGenerator();
        private RuleEnforcer ruleEnforcer = fadeContext.getRuleEnforcer();
        private ConstraintEvaluator constraintEvaluator = fadeContext.getConstraintEvaluator();
        private DetectingFlowManager detectingFlowManager = fadeContext.getDetectingFlowManager();
        private TagManager tagManager = fadeContext.getTagManager();
        private int run_id = 1;
        @Override
        public void run() {
            logger.info("start new detection run, run id = {}", run_id);
            int maxDetection = this.tagManager.getNumOfAvailableTag();
            List<Flow> flows = this.flowSelector.getFlows(maxDetection);
            List<Flow> detectable = Lists.newArrayList();
            for (Flow flow : flows) {
                if (this.detectingFlowManager.tryAddFlow(flow)) {
                    detectable.add(flow);
                } else {
                    logger.info("flow {} is being detected now, skip it", flow.getId());
                }
            }
            List<Flow> unDetected = FADEController.this.detectFlows(detectable, this.ruleGenerator, this.constraintGenerator,
                    IN_ANOMALY_IDENTIFICATION_PHASE, this.ruleEnforcer, this.constraintEvaluator);
            for (Flow flow : unDetected)
                this.detectingFlowManager.releaseDetectingFlow(flow);
            logger.info("detection run {} has finished, {} flows is submitted", run_id++, flows.size() - unDetected.size());
        }
    }

    private class SuspiciousFlowPollRunner implements Runnable {
        private static final boolean IN_ANOMALY_IDENTIFICATION_PHASE = true;
        private ConstraintEvaluator constraintEvaluator = fadeContext.getConstraintEvaluator();
        private RuleGenerator ruleGenerator = fadeContext.getRuleGenerator();
        private ConstraintGenerator consGenerator = fadeContext.getConstraintGenerator();
        private RuleEnforcer ruleEnforcer = fadeContext.getRuleEnforcer();
        private AnomalyIdentifier anomalyIdentifier = fadeContext.getAnomalyIdentifier();
        private DetectingFlowManager detectingFlowManager = fadeContext.getDetectingFlowManager();
        private List<Flow> toBeDetectedSuspicousFlows = Lists.newArrayList();

        @Override
        public void run() {
            List<Flow> undetected = FADEController.this.detectFlows(toBeDetectedSuspicousFlows, this.ruleGenerator, this.consGenerator,
                    IN_ANOMALY_IDENTIFICATION_PHASE, this.ruleEnforcer, this.constraintEvaluator);
            if (undetected.size() > 0) {
                toBeDetectedSuspicousFlows = undetected;
                return;
            }
            toBeDetectedSuspicousFlows = Lists.newArrayList();
            // poll from constraint evaluator
            List<Flow> localizeTask = Lists.newArrayList();
            List<List<Flow>> suspiciousFlows = this.constraintEvaluator.pollSuspiciousFlows(-1);
            IRuleNode anomaly = null;
            Iterator<List<Flow>> it = suspiciousFlows.iterator();
            while (it.hasNext()) {
                // try to identify malicious rules in suspicious flow path
                List<Flow> flows = it.next();
                if ((this.anomalyIdentifier.identifyAnomaly(flows)) != null) {
                    // find anomaly
                    flows.forEach(flow -> this.detectingFlowManager.releaseDetectingFlow(flow));
                    continue;
                }
                for (Flow flow : flows) {
                    if (flow.length() < 3) {
                        this.detectingFlowManager.releaseDetectingFlow(flow);
                        logger.warn("find no anomaly in flow {} at time {}, however, we are identifying anomalies.", flow, Calendar.getInstance().getTime());
                    } else {
                        localizeTask.add(flow);
                        logger.info("start to do localization for flow {}", flow);
                    }
                }
            }
            List<List<Flow>> passedSuspiciousFlows = this.constraintEvaluator.pollPassedSuspiciousFlows(-1);
            it = passedSuspiciousFlows.iterator();
            while (it.hasNext()) {
                List<Flow> flows = it.next();
                for (Flow flow : flows) {
                    int localizationRun = this.detectingFlowManager.addLocalizationRun(flow);
                    if (localizationRun > maxLocalizationRun) {
                        // too many times of localization, and finding nothing
                        logger.warn("we have tried to localize flow {} up to {} times, giving up.", flow, localizationRun);
                        this.detectingFlowManager.releaseDetectingFlow(flow);
                    } else {
                        localizeTask.add(flow);
                        logger.info("start to do localization for flow {}, run {}", flow, localizationRun);
                    }
                }
            }
            List<Flow> unDetected = FADEController.this.detectFlows(localizeTask, this.ruleGenerator, this.consGenerator,
                    IN_ANOMALY_IDENTIFICATION_PHASE, this.ruleEnforcer, this.constraintEvaluator);
            toBeDetectedSuspicousFlows.addAll(unDetected);
        }
    }
}
