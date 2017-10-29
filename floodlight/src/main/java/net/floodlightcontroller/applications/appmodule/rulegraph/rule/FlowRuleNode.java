package net.floodlightcontroller.applications.appmodule.rulegraph.rule;

import com.google.common.collect.Sets;
import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.util.hsa.FlowModHSConverter;
import net.floodlightcontroller.applications.util.hsa.HeaderSpace;
import net.floodlightcontroller.applications.util.hsa.IHSConverter;
import net.floodlightcontroller.applications.util.hsa.TernaryArray;
import net.floodlightcontroller.applications.util.trie.TrieNodeRetriever;
import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;

import java.util.List;
import java.util.Set;

/**
 * Implements flow rule as {@link IRuleNode}.
 *
 * @author chunhui (chunhui.pang@outlook.com)
 */
public class FlowRuleNode implements IRuleNode {
    private IHSConverter<OFFlowMod> hsConverter;
    private DatapathId datapathId;
    /** Actually, we only support {@link org.projectfloodlight.openflow.protocol.ver10.OFFlowAddVer10}
     *  and {@link org.projectfloodlight.openflow.protocol.ver10.OFFlowDeleteStrictVer10} here
     */
    private OFFlowMod refRule;
    private TernaryArray matchHS;
    private HeaderSpace reallyMatchHS;
    private Pair<TernaryArray, TernaryArray> rewriter;
    private TernaryArray outputHS;
    private HeaderSpace reallyOutputHS;
    private Set<IRuleNode> affectedNodes;
    private Set<IRuleNode> dependsOn;

    private static final int DEFAULT_RULE_PRIORITY = 0;
    private static final OFPort DEFAULT_INPORT = null;
    private static final OFPort DEFAULT_OUTPUT_PORT = null;


    /**
     * Default rule node, which matches all packets (header space)
     */
    public FlowRuleNode(){
        this(null, null);
    }

    /**
     * construct flow rule from rule and the switch on which it is enforced
     * @param flowMod the flow rule
     * @param datapathId the switch on which the flow rule is enforced
     */
    public FlowRuleNode(OFFlowMod flowMod, DatapathId datapathId){
        this.hsConverter = this.getHsConverter();
        if(this.hsConverter == null){
            throw new IllegalStateException("the header space converter for this class hasn't been set. Please see function getHsConverter()");
        }
        this.datapathId = datapathId;
        this.refRule = flowMod;
        /* matched header space */
        this.matchHS = this.hsConverter.parse(this.refRule);
        this.reallyMatchHS = this.hsConverter.parseHeaderSpace(this.refRule);
        this.rewriter = this.hsConverter.parseRewriter(this.refRule);
        /* output header space */
        if(this.rewriter == null){
            this.outputHS = this.matchHS;
        } else {
            this.outputHS = this.matchHS.copyApplyRewrite(this.rewriter.getLeft(), this.rewriter.getRight());
        }
        this.updateOutputHeaderspace();
        this.affectedNodes = Sets.newHashSet();
        this.dependsOn = Sets.newHashSet();
    }

    /**
     * Retrieve the headerspace convert for flow mod rule.
     * This function provides possible customization approach for a new converter
     * @return the converter used by current class to convert {@link OFFlowMod} to {@link HeaderSpace}
     */
    protected IHSConverter<OFFlowMod> getHsConverter(){
        return new FlowModHSConverter();
    }

    /**
     * update the output header space.
     * It shoud be called after the matched header space has been updated
     * @return current object
     */
    protected FlowRuleNode updateOutputHeaderspace(){
        if(this.rewriter == null) {
            this.reallyOutputHS = this.reallyMatchHS;
        } else {
            this.reallyOutputHS = this.reallyMatchHS.copyApplyWrite(this.rewriter.getLeft(), this.rewriter.getRight());
        }
        return this;
    }

    @Override
    public DatapathId getDatapathId() {
        return this.datapathId;
    }

    @Override
    public OFPort getInPort() {
        return this.refRule == null ? DEFAULT_INPORT : this.refRule.getMatch().get(MatchField.IN_PORT);
    }

    @Override
    public int getPriority() {
        return this.refRule == null ? DEFAULT_RULE_PRIORITY : this.refRule.getPriority();
    }

    @Override
    public TernaryArray getMatchHS() {
        return this.matchHS;
    }

    @Override
    public HeaderSpace getReallyMatchHS() {
        return this.reallyMatchHS;
    }

    @Override
    public Iterable<IRuleNode> getAffectedRuleNodes() {
        return this.affectedNodes;
    }

    @Override
    public TernaryArray getOutputHS() {
        return this.outputHS;
    }

    @Override
    public HeaderSpace getReallyOutputHS() {
        return this.reallyOutputHS;
    }

    @Override
    public List<OFAction> getActions() {
        return this.refRule.getActions();
    }

    @Override
    public OFPort getOutPort() {
        return this.refRule == null ? DEFAULT_OUTPUT_PORT : this.refRule.getOutPort();
    }

    @Override
    public TrieNodeRetriever<Object> getRetriever() {
        return new RuleTrieNodeRetriever(this);
    }

    @Override
    public boolean addAffectedRuleNode(IRuleNode affected) {
        /* affected nodes has no impact on the matched header space of current rule node */
        return this.affectedNodes.add(affected);
    }

    @Override
    public boolean removeAffectedRuleNode(IRuleNode affected) {
        return this.affectedNodes.remove(affected);
    }

    @Override
    public boolean addDependOnRuleNode(IRuleNode depends) {
        if(this.dependsOn.add(depends)){
            /* subtract "robbed" header space */
            this.reallyMatchHS.subtract(depends.getMatchHS());
            this.updateOutputHeaderspace();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public boolean removeDependOnRuleNode(IRuleNode depends) {
        if(this.dependsOn.remove(depends)){
            /* add "robbed" header space back to that "really" matched */
            this.reallyMatchHS.appendHeaderSpace(depends.getReallyMatchHS());
            this.updateOutputHeaderspace();
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Iterable<IRuleNode> getDependOnRuleNodes() {
        return this.dependsOn;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        FlowRuleNode that = (FlowRuleNode) o;

        if (datapathId != null ? !datapathId.equals(that.datapathId) : that.datapathId != null) return false;
        if (this.getInPort() != null ? !this.getInPort().equals(that.getInPort()) : that.getInPort() != null) return false;
        return matchHS != null ? matchHS.equals(that.matchHS) : that.matchHS == null;
    }

    @Override
    public int hashCode() {
        int result = datapathId != null ? datapathId.hashCode() : 0;
        result = 31 * result + (matchHS != null ? matchHS.hashCode() : 0);
        result = 31 * result + (this.getInPort() != null ? this.getInPort().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "FlowRuleNode{" + "datapathId=" + datapathId + ", refRule=" + refRule + ", affectedNodeSize=" + affectedNodes.size() + ", dependsOnSize=" + dependsOn.size() + '}';
    }
}
