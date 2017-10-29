package net.floodlightcontroller.applications.util.hsa;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author chunhui <chunhui.pang@outlook.com>
 * header space, borrowing from HSA
 *
 * A header space is a list of ternary arrays
 *
 */
public class HeaderSpace implements Cloneable{
	private int length;
    private List<TernaryArray> additions;
    private List<TernaryArray> subtractions;
	
	public HeaderSpace(int length){
		this.length = length;	
		this.additions = new ArrayList<>();
		this.subtractions = new ArrayList<>();
	}
	
	/**
	 * Append a given header space to current header space.
	 * Note: this function is different from union operation
	 * @param hs header space to append
	 * @return current object
	 * @throws InvalidOperationException if the two header space's length is not equal
	 */
	public HeaderSpace appendHeaderSpace(HeaderSpace hs) throws InvalidOperationException{
		if(hs.length != this.length)
			throw new InvalidOperationException(
					String.format("length of header space not match: %d vs %d!", 
							this.length, hs.length));
		for(TernaryArray ta : hs.additions)
			this.additions.add(ta);
		for(TernaryArray ta : hs.subtractions)
			this.subtractions.add(ta);
		return this;
	}
	
	/**
	 * Append ternary array to current header space
	 * @param ta the ternary array
	 * @return current object
	 * @throws InvalidOperationException
	 */
	public HeaderSpace appendArray(TernaryArray ta) throws InvalidOperationException{
		if(this.length != ta.getLength())
			throw new InvalidOperationException(
					String.format("length of header space and array not match: %d vs %d!", 
							this.length, ta.getLength()));
		this.additions.add(ta);
		return this;
	}
	/**
	 * subtract a ternary array
	 * @param ta the ternary array
	 * @return current object
	 * @throws InvalidOperationException
	 */
	public HeaderSpace diff(TernaryArray ta) throws InvalidOperationException{
		if(this.length != ta.getLength())
			throw new InvalidOperationException(
					String.format("length of header space and array not match: %d vs %d!", 
							this.length, ta.getLength()));
		this.subtractions.add(ta);
		return this;
	}

	/**
	 * get the current size of addition list
	 * @return the length of addition list
	 */
	public int countAdditions(){
		return this.additions.size();
	}

	/**
	 * get the current size of subtraction size
	 * @return the length of subtraction size
	 */
	public int countSubtractions(){
		return this.subtractions.size();
	}
	
	/**
	 * Intersection between two header space
	 * @param hs another header space
	 * @return current header space
	 * @throws InvalidOperationException the length of two header space doesn't match
	 */
	public HeaderSpace intersect(HeaderSpace hs) throws InvalidOperationException{
		if(this.length != hs.length)
			throw new InvalidOperationException(
					String.format("length of header space not match: %d vs %d!", 
							this.length, hs.length));
		List<TernaryArray> new_add = new LinkedList<TernaryArray>();
		for(TernaryArray t1 : this.additions){
			for(TernaryArray t2 : hs.additions){
				TernaryArray t3 = t1.copyIntersect(t2);
				if(t3.isEmpty() == false)
					new_add.add(t3);
			}
		}
		this.additions = new_add;
		for(TernaryArray t : hs.subtractions)
			this.subtractions.add(t);
		this.cleanup();
		return this;
	}
	
	/**
	 * @see #intersect(HeaderSpace)
	 * @param hs another header space
	 * @return the intersection
	 * @throws InvalidOperationException
	 */
	public HeaderSpace copyIntersect(HeaderSpace hs) throws InvalidOperationException{
		return this.clone().intersect(hs);
	}
	
	/**
	 * computing the subtraction operation in this header space
	 * @return the current object
	 */
	public HeaderSpace selfSubtract(){
		HeaderSpace tmp = new HeaderSpace(this.length);
		List<TernaryArray> subs = this.subtractions;
		this.subtractions = new LinkedList<TernaryArray>();
		for(TernaryArray t : subs){
			tmp.additions = t.complement();
			this.intersect(tmp);
		}
		return this;
	}
	/**
	 * Computing the complement of current header space
	 */
	public HeaderSpace complement(){
		if(this.subtractions.size() != 0)
			this.selfSubtract();
		if(this.additions.size() == 0){
			this.additions.add(TernaryArray.getAllX(this.length));
		}else{
			List<TernaryArray> tmp = this.additions;
			Iterator<TernaryArray> it = tmp.iterator();
			this.additions = it.next().complement();
			
			HeaderSpace hs = new HeaderSpace(this.length);
			while(it.hasNext()){
				hs.additions = it.next().complement();
				this.intersect(hs);
			}
		}
		return this;
	}
	
	/**
	 * computing the complement
	 * @see #complement()
	 * @return the complement
	 */
	public HeaderSpace copyComplement(){
		return this.clone().complement();
	}
	
	/**
	 * subtraction operation
	 * @param hs another operator
	 * @return the subtraction result
	 */
	public HeaderSpace subtract(HeaderSpace hs){
		HeaderSpace tmp = hs.copyComplement();
		return this.intersect(tmp);
	}
	/**
	 * subtraction
	 * @see #subtract(HeaderSpace)
	 * @param hs another header space
	 * @return the result
	 */
	public HeaderSpace copySubtract(HeaderSpace hs){
		return this.clone().subtract(hs);
	}

	/**
	 * Subtraction between header space and ternary array
	 * @param array the ternary array
	 * @return current object
	 */
	public HeaderSpace subtract(TernaryArray array) {
		if(this.length != array.getLength())
			throw new InvalidOperationException("the length doesn't match");
		this.subtractions.add(array);
		this.selfSubtract();
		return this;
	}
	
	/**
	 * Union operation between two header space 
	 * @param hs another header space
	 * @return the union result
	 */
	public HeaderSpace union(HeaderSpace hs){
		if(this.length != hs.length)
			throw new InvalidOperationException("length doesn't match");
		HeaderSpace hs1 = hs.clone().selfSubtract();
		this.additions.addAll(hs1.additions);
		return this;
	}
	
	/**
	 * @see #union(HeaderSpace)
	 * @param hs another header space 
	 * @return the union result
	 */
	public HeaderSpace copyUnion(HeaderSpace hs){
		return this.clone().union(hs);
	}
	/**
	 * Apply write action to current header space
	 * @param mask the write mask
	 * @param writer the write part of write action
	 * @return the rewrote header space
	 */
	public HeaderSpace applyWrite(TernaryArray mask, TernaryArray writer){
		for(TernaryArray ta : this.additions)
			ta.applyRewrite(mask, writer);
		for(TernaryArray ta : this.subtractions)
			ta.applyRewrite(mask, writer);
		return this;
	}
	/**
	 * @see #applyWrite(TernaryArray, TernaryArray)
	 * @param mask the mask part of write action
	 * @param writer the writer part of write action
	 * @return the result
	 */
	public HeaderSpace copyApplyWrite(TernaryArray mask, TernaryArray writer){
		return this.clone().applyWrite(mask, writer);
	}
	/**
	 * subset operation 
	 * @param hs another header space
	 * @return true if current header space is a subset of {@code hs}
	 */
	public boolean isSubsetOf(HeaderSpace hs){
		return this.copySubtract(hs).isEmpty();
	}
	/**
	 * decide current object is empty
	 * @return
	 */
	public boolean isEmpty(){
		if(this.subtractions.size() != 0)
			this.selfSubtract();
		return this.additions.size() == 0;
	}

	/**
	 * compressing this header space to less ternary array and clean all subtractions
	 * @return the compressed object
	 */
	public HeaderSpace cleanup(){
	    // clean all subtractions
	    this.selfSubtract();
	    // simplify
        boolean simplified = false;
        do {
            for(int i = 0; i < this.additions.size(); i++){
                for(int j = i + 1; j < this.additions.size(); j++){
                    if(this.additions.get(i).subset(this.additions.get(j))){
                        this.additions.remove(i);
                        simplified = true;
                    } else if(this.additions.get(j).subset(this.additions.get(i))) {
                        this.additions.remove(j);
                        simplified = true;
                    } else {
                        TernaryArray merge = TernaryArray.tryMerge(this.additions.get(i), this.additions.get(j));
                        if(merge != null) {
                            this.additions.add(merge);
                            // remove j first as j > i
                            this.additions.remove(j);
                            this.additions.remove(i);
                            simplified = true;
                        }
                    }
                }
            }
        } while (simplified);
		return this;
	}
	/**
	 * Return current addition list, don't modify it
	 * @return current addition list
	 */
	public List<TernaryArray> getAdditions(){
		return this.additions;
	}
	
	/**
	 * Return current subtraction list, don't modify it
	 * @return current subtraction list
	 */
	public List<TernaryArray> getSubtractions(){
		return this.subtractions;
	}
	
	@Override
	public HeaderSpace clone(){
		HeaderSpace hs = new HeaderSpace(this.length);
		hs.additions.addAll(this.additions.stream().map(TernaryArray::clone).collect(Collectors.toList()));
		hs.subtractions.addAll(this.subtractions.stream().map(TernaryArray::clone).collect(Collectors.toList()));
		return hs;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("HeaderSpace [");
		Iterator<TernaryArray> it = this.additions.iterator();
		while(it.hasNext()){
			TernaryArray ternaryArray = it.next();
			if(it.hasNext())
				sb.append(ternaryArray).append(" U ");
			else
				sb.append(ternaryArray);
		}
		it = this.subtractions.iterator();
		while(it.hasNext())
            sb.append(" - ").append(it.next());
		sb.append("]");
		return sb.toString();
	}
}
