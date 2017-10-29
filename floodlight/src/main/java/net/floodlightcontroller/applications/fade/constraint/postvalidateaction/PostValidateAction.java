package net.floodlightcontroller.applications.fade.constraint.postvalidateaction;

import net.floodlightcontroller.applications.fade.constraint.Constraint;
import net.floodlightcontroller.applications.fade.flow.Flow;

/**
 * An actions happens after the validation of an {@link Constraint}.
 * You could leverage this action to do extra operations.
 * For example, releasing tags, cookies, and release flow statistics.
 */
public interface PostValidateAction {
    /**
     * execute the post validate action
     * @param constraint the constraint related to the validation
     * @param validateResult the validation result
     */
    void execute(Constraint constraint, boolean validateResult);
}
