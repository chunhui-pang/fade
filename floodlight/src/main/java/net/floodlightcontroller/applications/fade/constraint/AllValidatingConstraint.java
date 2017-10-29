package net.floodlightcontroller.applications.fade.constraint;

import com.google.common.collect.Lists;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.stats.StatsContext;

import java.util.Iterator;

/**
 * validate all constraints
 */
public class AllValidatingConstraint extends SequentialConstraint {

    public AllValidatingConstraint(Flow flow) {
        super(flow);
    }

    @Override
    protected boolean validateList(StatsContext statsContext) {
        Iterator<Constraint> it = super.getConstraints().iterator();
        boolean result = true;
        while (it.hasNext()) {
            Constraint ct = it.next();
            boolean tmp = ct.validate(statsContext);
            result &= tmp;
            if(tmp == false){
                super.addSuspiciousFlows(Lists.newArrayList(ct.getSuspiciousFlow()));
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return "AllValidatingConstraint{ " + super.toString() + " }";
    }
}
