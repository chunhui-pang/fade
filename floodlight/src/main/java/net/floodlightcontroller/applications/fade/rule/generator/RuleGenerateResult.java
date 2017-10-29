package net.floodlightcontroller.applications.fade.rule.generator;

import net.floodlightcontroller.applications.fade.constraint.postvalidateaction.PostValidateAction;
import net.floodlightcontroller.applications.fade.flow.FlowNode;
import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.types.DatapathId;

import java.util.List;

/**
 * The result data of {@link RuleGenerator}
 */
public interface RuleGenerateResult {
    /**
     * get all selected flow nodes
     * @return all selected flow nodes
     */
    List<FlowNode> getSelectedFlowNodes();

    /**
     * get generated r1 rules (the first group of dedicated rules)
     * @return r1 rules
     */
    List<OFFlowMod> getR1Rules();

    /**
     * get the install switch of r1 rules
     * @return a list of switches
     */
    List<DatapathId> getR1Switches();

    /**
     * get the generated r2 rules (the second group of dedicated rules)
     * @return r2 rules
     */
    List<OFFlowMod> getR2Rules();

    /**
     * get the install switch of r2 rules
     * @return a list of switches
     */
    List<DatapathId> getR2Switches();

    /**
     * get the enforce delay between r1 and r2 rules.
     * Note, we install r2 first by default.
     * @return the delay in milliseconds
     */
    long getEnforceDelayBetweenR1AndR2();

    /**
     * get all post validate actions that are used to clean data
     * @return the post actions
     */
    List<PostValidateAction> getPostValidateActions();
}
