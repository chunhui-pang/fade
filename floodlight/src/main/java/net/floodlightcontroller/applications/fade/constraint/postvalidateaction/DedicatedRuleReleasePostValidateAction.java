package net.floodlightcontroller.applications.fade.constraint.postvalidateaction;

import net.floodlightcontroller.applications.fade.constraint.Constraint;
import net.floodlightcontroller.applications.fade.rule.manager.DedicatedRuleManager;

/**
 * Release dedicated flow rules by the index of the generated dedicated flow rule
 */
public class DedicatedRuleReleasePostValidateAction implements PostValidateAction {
    private DedicatedRuleManager dedicatedRuleManager;
    private long index;

    public DedicatedRuleReleasePostValidateAction(DedicatedRuleManager dedicatedRuleManager, long idx){
        this.dedicatedRuleManager = dedicatedRuleManager;
        this.index = idx;
    }

    @Override
    public void execute(Constraint constraint, boolean validateResult) {
        this.dedicatedRuleManager.removeDedicatedRule(index);
    }

    @Override
    public String toString() {
        return "DedicatedRuleReleasePostValidateAction{" +
                "index=" + index +
                '}';
    }
}
