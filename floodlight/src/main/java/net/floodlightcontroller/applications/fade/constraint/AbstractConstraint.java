package net.floodlightcontroller.applications.fade.constraint;

import net.floodlightcontroller.applications.fade.constraint.postvalidateaction.PostValidateAction;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.stats.StatsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * A common {@link Constraint} that implements post actions and a validation framework
 */
public abstract class AbstractConstraint implements Constraint {
    private static Logger logger = LoggerFactory.getLogger(AbstractConstraint.class);
    protected List<PostValidateAction> postValidateActions;
    protected Boolean execCache;
    protected Flow flow;

    protected AbstractConstraint(Flow flow){
        this.postValidateActions = null;
        this.execCache = null;
        this.flow = flow;
    }

    @Override
    public Flow getFlow() {
        return this.flow;
    }

    @Override
    public boolean validate(StatsContext statsContext) {
        if(this.execCache == null){
            this.execCache = this.doValidate(statsContext);
            this.executePostValidateActions();
        }
        return this.execCache.booleanValue();
    }

    @Override
    public Constraint setPostValidateActions(List<PostValidateAction> postValidateActions) {
        this.postValidateActions = postValidateActions;
        return this;
    }

    protected void executePostValidateActions() {
        if(postValidateActions != null) {
            for (PostValidateAction postValidateAction : postValidateActions) {
                postValidateAction.execute(this, this.execCache);
                if(logger.isDebugEnabled()) {
                    logger.debug("post validate action {} was executed", postValidateAction);
                }
            }
        }
    }

    protected abstract boolean doValidate(StatsContext statsContext);


}
