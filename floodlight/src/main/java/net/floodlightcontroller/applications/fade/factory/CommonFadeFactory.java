package net.floodlightcontroller.applications.fade.factory;

import net.floodlightcontroller.applications.appmodule.concretetopology.IConcreteTopologyService;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleGraphService;
import net.floodlightcontroller.applications.fade.constraint.evaluator.ConstraintEvaluator;
import net.floodlightcontroller.applications.fade.constraint.evaluator.ConstraintEvaluatorImpl;
import net.floodlightcontroller.applications.fade.constraint.generator.ConstraintGenerator;
import net.floodlightcontroller.applications.fade.flow.FlowSelector;
import net.floodlightcontroller.applications.fade.flow.manager.DetectingFlowManager;
import net.floodlightcontroller.applications.fade.flow.manager.DetectingFlowManagerImpl;
import net.floodlightcontroller.applications.fade.identifier.AnomalyIdentifier;
import net.floodlightcontroller.applications.fade.rule.enforcer.RuleEnforcer;
import net.floodlightcontroller.applications.fade.rule.enforcer.RuleEnforcerImpl;
import net.floodlightcontroller.applications.fade.rule.generator.ProbeSelector;
import net.floodlightcontroller.applications.fade.rule.generator.RuleGenerator;
import net.floodlightcontroller.applications.fade.rule.manager.DedicatedRuleManager;
import net.floodlightcontroller.applications.fade.rule.manager.DedicatedRuleManagerImpl;
import net.floodlightcontroller.applications.fade.rule.manager.RuleIndexer;
import net.floodlightcontroller.applications.fade.rule.manager.RuleIndexerImpl;
import net.floodlightcontroller.applications.fade.stats.StatsCollector;
import net.floodlightcontroller.applications.fade.stats.StatsCollectorImpl;
import net.floodlightcontroller.applications.fade.util.CookieManager;
import net.floodlightcontroller.applications.fade.util.CookieManagerImpl;
import net.floodlightcontroller.applications.fade.util.DscpVlanTagManager;
import net.floodlightcontroller.applications.fade.util.TagManager;
import net.floodlightcontroller.core.internal.IOFSwitchService;
import net.floodlightcontroller.threadpool.IThreadPoolService;
import org.projectfloodlight.openflow.protocol.OFFactory;

/**
 * a common fade factory for both {@link SingleFlowFadeFactory} and {@link AggregatedFlowFadeFactory}
 */
public abstract class CommonFadeFactory implements FadeFactory {
    private static final double DETECTING_TAG_RATIO = .95;
    private int appId;
    private String appName;
    protected double acceptedStatsDeviation;
    protected double detectingTagRatio;
    protected boolean ignoreTimeoutIssues;
    protected OFFactory ofFactory;
    protected IConcreteTopologyService concreteTopologyService;
    protected IOFSwitchService ofSwitchService;
    protected IRuleGraphService ruleGraphService;
    protected IThreadPoolService threadPoolService;
    protected ProbeSelector probeSelector;
    protected CommonFadeFactory(int appId, String appName, double acceptedStatsDeviation, double detectingTagRatio, boolean ignoreTimeoutIssues,
                                OFFactory ofFactory, IConcreteTopologyService concreteTopologyService,
                                IOFSwitchService ofSwitchService, IRuleGraphService ruleGraphService,
                                IThreadPoolService threadPoolService, ProbeSelector probeSelector){
        this.appId = appId;
        this.appName = appName;
        this.acceptedStatsDeviation = acceptedStatsDeviation;
        this.detectingTagRatio = detectingTagRatio;
        this.ignoreTimeoutIssues = ignoreTimeoutIssues;
        this.ofFactory = ofFactory;
        this.concreteTopologyService = concreteTopologyService;
        this.ofSwitchService = ofSwitchService;
        this.ruleGraphService = ruleGraphService;
        this.threadPoolService = threadPoolService;
        this.probeSelector = probeSelector;
    }

    @Override
    public FadeContext createFadeContext(){
        FadeContextEntity fadeContext = new FadeContextEntity();
        fadeContext.setRuleIndexer(this.getRuleIndexer())
                .setCookieManager(this.getCookieManager())
                .setRuleEnforcer(this.getRuleEnforcer())
                .setStatsCollector(this.getStatsCollector())
                .setDedicatedRuleManager(this.getDedicatedRuleManager())
                .setConstraintEvaluator(this.getConstraintEvaluator(fadeContext))
                .setDetectingFlowManager(this.getDetectingFlowManager())
                .setProbeSelector(this.getProbeSelector())
                .setTagManager(this.getTagManager());
        // set other fields. Note the order should be adjusted if there dependency is disobeyed.
        fadeContext.setAnomalyIdentifier(this.getAnomalyIdentifier(fadeContext))
                .setFlowSelector(this.getFlowSelector(fadeContext))
                .setRuleGenerator(this.getRuleGenerator(fadeContext))
                .setConstraintGenerator(this.getConstraintGenerator(fadeContext));

        return fadeContext;
    }

    protected RuleIndexer getRuleIndexer() {
        return new RuleIndexerImpl();
    }

    protected CookieManager getCookieManager( ){
        return new CookieManagerImpl(this.appId, this.appName);
    }

    protected RuleEnforcer getRuleEnforcer( ){
        return new RuleEnforcerImpl(this.ofSwitchService, this.threadPoolService);
    }

    protected StatsCollector getStatsCollector(){
        return new StatsCollectorImpl();
    }

    protected DedicatedRuleManager getDedicatedRuleManager(){
        return new DedicatedRuleManagerImpl(this.threadPoolService);
    }

    protected DetectingFlowManager getDetectingFlowManager() { return new DetectingFlowManagerImpl(); }

    protected ConstraintEvaluator getConstraintEvaluator(FadeContext fadeContext){
        return new ConstraintEvaluatorImpl(fadeContext.getStatsCollector().getStatsContext(), this.threadPoolService);
    }

    protected ProbeSelector getProbeSelector(){
        return this.probeSelector;
    }

    protected TagManager getTagManager() {
        return new DscpVlanTagManager(this.detectingTagRatio);
    }

    protected abstract FlowSelector getFlowSelector(FadeContext fadeContext);

    protected abstract RuleGenerator getRuleGenerator(FadeContext fadeContext);

    protected abstract ConstraintGenerator getConstraintGenerator(FadeContext fadeContext);

    protected abstract AnomalyIdentifier getAnomalyIdentifier(FadeContext fadeContext);
}

/**
 * implement the {@link FadeContext}
 */
class FadeContextEntity implements FadeContext {
    private TagManager tagManager;
    private CookieManager cookieManager;
    private DedicatedRuleManager dedicatedRuleManager;
    private RuleIndexer ruleIndexer;
    private DetectingFlowManager detectingFlowManager;
    private FlowSelector flowSelector;
    private RuleGenerator ruleGenerator;
    private ConstraintGenerator constraintGenerator;
    private RuleEnforcer ruleEnforcer;
    private StatsCollector statsCollector;
    private ConstraintEvaluator constraintEvaluator;
    private AnomalyIdentifier anomalyIdentifier;
    private ProbeSelector probeSelector;

    @Override
    public TagManager getTagManager() {
        return this.tagManager;
    }

    @Override
    public CookieManager getCookieManager() {
        return this.cookieManager;
    }

    @Override
    public DedicatedRuleManager getDedicatedRuleManager() {
        return this.dedicatedRuleManager;
    }

    @Override
    public RuleIndexer getRuleIndexer() {
        return this.ruleIndexer;
    }

    @Override
    public DetectingFlowManager getDetectingFlowManager() { return this.detectingFlowManager; }

    @Override
    public FlowSelector getFlowSelector() {
        return this.flowSelector;
    }

    @Override
    public RuleGenerator getRuleGenerator() {
        return this.ruleGenerator;
    }

    @Override
    public ConstraintGenerator getConstraintGenerator() {
        return this.constraintGenerator;
    }

    @Override
    public RuleEnforcer getRuleEnforcer() {
        return this.ruleEnforcer;
    }

    @Override
    public StatsCollector getStatsCollector() {
        return this.statsCollector;
    }

    @Override
    public ConstraintEvaluator getConstraintEvaluator() {
        return this.constraintEvaluator;
    }

    @Override
    public AnomalyIdentifier getAnomalyIdentifier() {
        return this.anomalyIdentifier;
    }

    @Override
    public ProbeSelector getProbeSelector() {
        return this.probeSelector;
    }

    public FadeContextEntity setTagManager(TagManager tagManager) {
        this.tagManager = tagManager;
        return this;
    }

    public FadeContextEntity setCookieManager(CookieManager cookieManager) {
        this.cookieManager = cookieManager;
        return this;
    }

    public FadeContextEntity setDedicatedRuleManager(DedicatedRuleManager dedicatedRuleManager) {
        this.dedicatedRuleManager = dedicatedRuleManager;
        return this;
    }

    public FadeContextEntity setRuleIndexer(RuleIndexer ruleIndexer) {
        this.ruleIndexer = ruleIndexer;
        return this;
    }

    public FadeContextEntity setDetectingFlowManager(DetectingFlowManager detectingFlowManager){
        this.detectingFlowManager = detectingFlowManager;
        return this;
    }

    public FadeContextEntity setFlowSelector(FlowSelector flowSelector) {
        this.flowSelector = flowSelector;
        return this;
    }

    public FadeContextEntity setRuleGenerator(RuleGenerator ruleGenerator) {
        this.ruleGenerator = ruleGenerator;
        return this;
    }

    public FadeContextEntity setConstraintGenerator(ConstraintGenerator constraintGenerator) {
        this.constraintGenerator = constraintGenerator;
        return this;
    }

    public FadeContextEntity setRuleEnforcer(RuleEnforcer ruleEnforcer) {
        this.ruleEnforcer = ruleEnforcer;
        return this;
    }

    public FadeContextEntity setStatsCollector(StatsCollector statsCollector) {
        this.statsCollector = statsCollector;
        return this;
    }

    public FadeContextEntity setConstraintEvaluator(ConstraintEvaluator constraintEvaluator) {
        this.constraintEvaluator = constraintEvaluator;
        return this;
    }

    public FadeContextEntity setAnomalyIdentifier(AnomalyIdentifier anomalyIdentifier) {
        this.anomalyIdentifier = anomalyIdentifier;
        return this;
    }

    public FadeContextEntity setProbeSelector(ProbeSelector probeSelector){
        this.probeSelector = probeSelector;
        return this;
    }
}
