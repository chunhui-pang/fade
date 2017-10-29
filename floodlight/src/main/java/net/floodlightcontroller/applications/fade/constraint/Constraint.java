package net.floodlightcontroller.applications.fade.constraint;

import net.floodlightcontroller.applications.fade.constraint.postvalidateaction.PostValidateAction;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.stats.StatsContext;

import java.util.Collection;
import java.util.List;


/**
 * The statistics constraint.
 * Each constraint includes one/several expressions, and one/more optional post actions.
 * We validates the satisfaction of these expressions, and after the validation, we do the post actions.
 * We set suspicious flow for two situations:
 * <ul>
 *     <li>not passed: the constraint is not satisfied, and there is a suspicious flow</li>
 *     <li>passed: the constraint is satisfied, however, this is not the case. Then a suspicious flow could be set.</li>
 * </ul>
 */
public interface Constraint {

    /**
     * get the corresponding flow
     * @return the flow
     */
    Flow getFlow();
    /**
     * validate whether if the constraint is satisfied.
     * @param statsContext the statistics provider
     * @return true if is satisfied.
     */
    boolean validate(StatsContext statsContext);

    /**
     * set post validate actions for
     * @param postValidateActions
     * @return
     */
    Constraint setPostValidateActions(List<PostValidateAction> postValidateActions);

    /**
     * If the validation fails, some suspicious flows should be returned to localize anomalies.
     * @return a list of subflows used to localize anomalies.
     */
    Collection<Flow> getSuspiciousFlow();

    /**
     * Return the suspicious flow if the constraint is satisfied.
     * @return the suspicious flow
     */
    Collection<Flow> getPassedSuspiciousFlow();

}
