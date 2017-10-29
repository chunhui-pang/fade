package net.floodlightcontroller.applications.fade.constraint;

import com.google.common.collect.Lists;
import net.floodlightcontroller.applications.fade.exception.LogicError;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.stats.StatsContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * A set of constraint which are validated from left to right.
 * The validation stops when one of the constraint fails, and the result is set to false.
 * If all constraints are satisfied, the result is true.
 */
public class First2LastValidatingConstraint extends SequentialConstraint {

    public First2LastValidatingConstraint(Flow flow) {
        super(flow);
    }

    @Override
    public boolean validateList(StatsContext statsContext) {
        Iterator<Constraint> it = super.getConstraints().iterator();
        while (it.hasNext()) {
            Constraint ct = it.next();
            boolean tmp = ct.validate(statsContext);
            if(tmp == false){
                super.addSuspiciousFlows(Lists.newArrayList(ct.getSuspiciousFlow()));
                return false;
            }
        }
        return true;
    }

    @Override
    public String toString() {
        return "First2LastValidatingConstraint{ " + super.toString() + " }";
    }
}
