package net.floodlightcontroller.applications.fade.constraint.postvalidateaction;

import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.fade.constraint.Constraint;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.stats.StatsContext;

/**
 * release flow statistics
 */
public class StatsReleasePostValidationAction implements PostValidateAction {
    private StatsContext statsContext;
    private long index;

    public StatsReleasePostValidationAction(StatsContext statsContext, long index){
        this.statsContext = statsContext;
        this.index = index;
    }

    @Override
    public void execute(Constraint constraint, boolean validationResult) {
        this.statsContext.releaseStats(this.index);
    }

    @Override
    public String toString() {
        return "StatsReleasePostValidationAction{" +
                "index=" + index +
                '}';
    }
}
