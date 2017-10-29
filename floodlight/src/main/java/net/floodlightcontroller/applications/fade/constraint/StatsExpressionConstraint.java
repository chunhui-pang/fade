package net.floodlightcontroller.applications.fade.constraint;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.udojava.evalex.Expression;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.exception.LogicError;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.rule.manager.DedicatedRuleManager;
import net.floodlightcontroller.applications.fade.stats.StatsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * constraint implementation by read statistics context.
 * We use EvalEx to evaluate expressions,
 * and in the implementation, we use {@code stats(tag)} to retrieve flow statistics from StatsContext.
 * This class disable the "passed suspicious flow" feature.
 */
public class StatsExpressionConstraint extends AbstractConstraint {
    private static final Logger logger = LoggerFactory.getLogger(StatsExpressionConstraint.class);
    private DedicatedRuleManager dedicatedRuleManager;
    private String expression;
    private Collection<Flow> suspiciousFlows;
    public static final String RETRIEVE_STATS_FUNC_NAME = "stats";

    /**
     * Constructor.
     * @param dedicatedRuleManager the dedicated rule util used to get rule by its tag
     * @param expression the expression
     * @param suspiciousFlows the suspicious flows if the expression is not satisfied.
     */
    public StatsExpressionConstraint(Flow flow, DedicatedRuleManager dedicatedRuleManager, String expression, Collection<Flow> suspiciousFlows){
        super(flow);
        this.dedicatedRuleManager = dedicatedRuleManager;
        this.expression = expression;
        this.suspiciousFlows = suspiciousFlows;
    }

    @Override
    public boolean doValidate(StatsContext statsContext) {
        // evaluate the function
        Expression exp = new Expression(expression, MathContext.UNLIMITED);
        final DedicatedRuleManager dedicatedRuleManager = this.dedicatedRuleManager;
        final StatsContext sc = statsContext;
        final Set<Long> cookies = Sets.newHashSet();
        final List<Long> stats = Lists.newArrayList();
        exp.addFunction(exp.new Function(RETRIEVE_STATS_FUNC_NAME, 1) {
            @Override
            public BigDecimal eval(List<BigDecimal> parameters) {
                if(parameters.size() != 1){
                    throw new LogicError("invalid expression, we use stats(tag) to retrieve statistics.");
                }
                long cookie = parameters.get(0).longValue(); // retrieve the index
                BigDecimal pktCount = BigDecimal.valueOf(statsContext.getPacketCount(cookie));
                logger.debug("retrieve flow rule with index {} with packet count {}", cookie, pktCount);
                if(logger.isDebugEnabled()){
                    if(cookies.add(cookie)){
                        stats.add(statsContext.getPacketCount(cookie));
                    }
                }
                return pktCount;
            }
        });
        BigDecimal result = exp.eval();
        if(logger.isDebugEnabled()) {
            logger.debug("get flow statistics of flow {}: values are: {} (possibly incomplete)", this.flow, stats);
        }
        return (result.intValue() != 0);
    }

    @Override
    public Collection<Flow> getSuspiciousFlow() {
        if(null == this.execCache){
            throw new LogicError("the expression has not been validated yet.");
        } else if(true == this.execCache.booleanValue()) {
            return null;
        } else {
            return this.suspiciousFlows;
        }
    }

    @Override
    public Collection<Flow> getPassedSuspiciousFlow(){
        return null;
    }

    public static String statsOf(long idx) {
        return RETRIEVE_STATS_FUNC_NAME + "( " + idx + " )";
    }

    @Override
    public String toString() {
        return "StatsExpressionConstraint{" + "expression='" + expression + '\'' + '}';
    }

}
