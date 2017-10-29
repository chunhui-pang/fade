package net.floodlightcontroller.applications.fade.constraint;

import com.google.common.collect.Lists;
import net.floodlightcontroller.applications.fade.exception.LogicError;
import net.floodlightcontroller.applications.fade.flow.Flow;
import net.floodlightcontroller.applications.fade.stats.StatsContext;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * a sequence of constraints.
 * These constraints could be evaluated in order and with short-cut grammer
 */
public abstract class SequentialConstraint extends AbstractConstraint {
    private List<Constraint> constraints = null;
    private List<Flow> suspiciousFlows;
    // if all constraints are passed, is there any suspicious flow?
    private List<Flow> passedSuspiciousFlow;

    protected SequentialConstraint(Flow flow){
        super(flow);
        this.constraints = new LinkedList<>();
        this.suspiciousFlows = null;
        this.passedSuspiciousFlow = null;
    }

    /**
     * The implementation of validating the sequence of constraints
     * @param statsContext the statistics context
     * @return the validation result
     */
    protected abstract boolean validateList(StatsContext statsContext);

    /**
     * setters provided for the implementation of subclass.
     * @param suspiciousFlows the suspicious flow
     */
    protected void addSuspiciousFlows(List<Flow> suspiciousFlows){
        if(null == this.suspiciousFlows){
            this.suspiciousFlows = Lists.newArrayList();
        }
        this.suspiciousFlows.addAll(suspiciousFlows);
    }

    public SequentialConstraint setPassedSuspiciousFlow(List<Flow> passedSuspiciousFlow){
        this.passedSuspiciousFlow = passedSuspiciousFlow;
        return this;
    }

    /**
     * getter provided for the subclass's implementation
     * @return the constraints
     */
    protected List<Constraint> getConstraints(){
        return this.constraints;
    }

    /**
     * add new constraint
     * @param constraint the new constraint
     * @return if it was added
     */
    public boolean appendConstraint(Constraint constraint){
        return this.constraints.add(constraint);
    }

    /**
     * add a constraint to a given position
     * @param constraint the constraint
     * @param index the position
     */
    public void addConstraint(Constraint constraint, int index){
        this.constraints.add(index, constraint);
    }

    /**
     * remove a constraint by its position
     * @param index the position of the constraint
     * @return if it is removed
     */
    public boolean removeConstraint(int index){
        if(this.constraints.size() <= index){
            return false;
        } else {
            this.constraints.remove(index);
            return true;
        }
    }

    /**
     * remove all constraint which equals to the specified constraint
     * @param constraint the constraint to be removed
     * @return the number of removed elements
     */
    public int removeConstraint(Constraint constraint){
        Iterator<Constraint> it = this.constraints.iterator();
        int count = 0;
        while(it.hasNext()){
            Constraint cit = it.next();
            if(cit.equals(constraint)){
                count++;
                it.remove();
            }
        }
        return count;
    }

    /**
     * get the number of constraint (children) in it
     * @return the number of constraint
     */
    public int length(){
        return this.constraints.size();
    }

    @Override
    public boolean doValidate(StatsContext statsContext) {
        return validateList(statsContext);
    }

    @Override
    public Collection<Flow> getSuspiciousFlow() {
        if(null == this.execCache){
            throw new LogicError("programming logic error. Please refer to the documentation.");
        } else if(true == this.execCache.booleanValue()){
            return null;
        } else {
            return this.suspiciousFlows;
        }
    }
    @Override
    public List<Flow> getPassedSuspiciousFlow( ) {
        if(null == this.execCache){
            throw new LogicError("programming logic error. Please refer to the documentation.");
        } else if(true == this.execCache.booleanValue()){
            return this.passedSuspiciousFlow;
        } else {
            return null;
        }
    }

    @Override
    public String toString() {
        return "SequentialConstraint{" +
                "flow=" + flow +
                ", constraints=" + constraints +
                '}';
    }
}
