package net.floodlightcontroller.applications.util.hsa;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;


/**
 * @author chunhui <chunhui.pang@outlook.com>
 * @date 2016/11/07
 *
 * Ternary array, borrowing from HSA. However, the length of a ternary array must be times of 4
 *
 * Ternary array is an array which support three elements: 0, 1 and *, in which '*' means '0' or '1'.
 * Ternary array can be viewed as a collection, and we implement join, intersect, difference, and complement operation on it
 *
 */
public class TernaryArray implements Cloneable {
	private byte[] array;
	private boolean empty;
	
	private TernaryArray(int len, boolean allocate){
		if(len % 4 != 0)
			throw new InvalidOperationException("length must be times of 4");
		if(allocate == false)
			this.array = null;
		else
			this.array = new byte[len/4];
		this.empty = true;
	}
	
	public TernaryArray(int len){
		this(len, true);
	}
	/**
	 * Intersect between two object, return current object.
	 * In other words, current object is modified
	 * @param a1 another object
	 * @return the intersection
	 */
	public TernaryArray intersect(TernaryArray a1){
		if(array.length != a1.array.length)
			throw new InvalidOperationException("length not match!");
		if(this.empty || a1.empty){
			this.empty = true;
			return this;
		}
		for(int i = 0; i < array.length; i++){
			array[i] &= a1.array[i];
			if((array[i] & 0x03) == 0 || (array[i] & 0x0c) == 0 || (array[i] & 0x30) == 0 || (array[i] & 0xc0) == 0){
				this.empty = true;
				return this;
			}
		}
		return this;
	}
	
	/**
	 * The same with {@link #intersect(TernaryArray)} but returning a independent object
	 * @param a1 another ternary array
	 * @return independent object represent of the intersection
	 */
	public TernaryArray copyIntersect(TernaryArray a1){
		return this.clone().intersect(a1);
	}
	
	/**
	 * complement operation
	 * @return complement of current object. all represented by independent objects
	 */
	public List<TernaryArray> complement(){
		List<TernaryArray> arrays = new LinkedList<TernaryArray>();
		if(this.empty){
			arrays.add(getAllX(this.array.length*4));
			return arrays;
		}
		for(int i = 0; i < array.length; i++){
			for(int j = 0; j < 4; j++){
				if(((array[i] >> 2*j) & 0x03) == 0x01){
					TernaryArray allx = getAllX(this.array.length*4);
					allx.array[i] = (byte) (((0xFE << 2*j) & 0xff) | ((0xFF >> (8-2*j)) & 0xff));
					arrays.add(allx);
				} else if(((array[i] >> 2*j) & 0x03) == 0x02){
					TernaryArray allx = getAllX(this.array.length*4);
					allx.array[i] = (byte) (((0xFD << 2*j) & 0xff) | ((0xFF >> (8-2*j))&0xff));
	                arrays.add(allx);
				}
			}
		}
		return arrays;
	}
	
	/**
	 * subtraction operation
	 * @param a1 another object
	 * @return the subtraction result. Represented by independent object
	 */
	public List<TernaryArray> difference(TernaryArray a1){
		if(this.array.length != a1.array.length)
			throw new InvalidOperationException("length not match");
		if(this.empty || a1.empty)
			return Arrays.asList(this.clone());
		List<TernaryArray> arrays = a1.complement();
		Iterator<TernaryArray> it = arrays.iterator();
		while(it.hasNext()){
			if(it.next().intersect(this).empty == true)
				it.remove();
		}
		return arrays;
	}
	
	/**
	 * Deciding if an object is in a array.
	 * @param ta array
	 * @return true or false
	 */
	public boolean containIn(List<TernaryArray> ta){
        return ta.contains(this);
	}
	
	/**
	 * Deciding if current object is the subset of another object. <br />
     * Note, we assume {@code a1) is simplified and current ternary array cannot span over multiple other ternary arrays
     *
	 * @param a1 another object
	 * @return true or false
	 */
	public boolean subset(TernaryArray a1){
		if(array.length != a1.array.length)
			throw new InvalidOperationException("length not match");
		if(this.empty)
			return true;
		if(a1.empty)
			return false;
		for(int i = 0; i < array.length; i++){
			byte cmp = (byte) (array[i] ^ a1.array[i]);
	        if (cmp != 0 && (cmp & a1.array[i]) != cmp)
	            return false;
		}
	    return true;
	}
	
	/**
	 * Calculate the "byte and" logic as general boolean values.
     * The value table as follows: ( 0 & 1 = 0, 1 & 1 = 1, 0 & * = 0, 1 & * = 1)
	 *     00 01 10 11
	 * 00  00 01 00 01
	 * 01  01 01 01 01
	 * 10  00 01 10 10
	 * 11  01 01 10 11
	 * @param a1 another array
	 * @return the byte and result
	 */
	public TernaryArray byteAnd(TernaryArray a1){
		if(this.array.length != a1.array.length)
			throw new InvalidOperationException("length not match");
		if(this.empty || a1.empty)
			throw new InvalidOperationException("cannot apply byte and operation on empty object");
		for(int i = 0; i < array.length; i++)
	        array[i] = (byte) ((array[i] & a1.array[i] & 0xaa) | ((array[i] | a1.array[i])&0x55)); 
		return this;
	}
	
	/**
	 * The same to {@link #byteAnd(TernaryArray)} but returning an independent object
	 * @param a1 another object 
	 * @return the byte and 
	 */
	public TernaryArray copyByteAnd(TernaryArray a1){
		return this.clone().byteAnd(a1);
	}
	
	/**
	 * value table
	 *     00 01 10 11
	 * 00  00 00 10 10
	 * 01  00 01 10 11
	 * 10  10 10 10 10
	 * 11  10 11 10 11
	 * @param a1 another object
	 * @return the result
	 */
	public TernaryArray byteOr(TernaryArray a1){
		if(this.array.length != a1.array.length)
			throw new InvalidOperationException("length not match");
		if(this.empty || a1.empty)
			throw new InvalidOperationException("cannot apply byte or operation on empty object");
		for(int i = 0; i < array.length; i++)
	        array[i] = (byte) ((array[i] & a1.array[i] & 0x55) | ((array[i] | a1.array[i])&0xaa)); 
		return this;
	}
	/**
	 * @see #byteOr(TernaryArray)
	 * @param a1 another Ternary array with the same length
	 * @return the or operation result
	 */
	public TernaryArray copyByteOr(TernaryArray a1){
		return this.clone().byteOr(a1);
	}
	
	/**
	 * value table
	 * 	00 01 10 11
	 *  00 10 01 11
	 * @return the not operation
	 */
	public TernaryArray byteNot(){
		if(this.empty)
			throw new InvalidOperationException("cannot apply byte and operation on empty object");
		for(int i = 0; i < array.length; i++)
	        array[i] = (byte) (((array[i] << 1) & 0xaa) | ((array[i] >> 1) & 0x55));
		return this;
	}
	
	/**
	 * @see #byteNot()
	 * @return the not result
	 */
	public TernaryArray copyByteNot(){
		return this.clone().byteNot();
	}

	/**
	 * generate mask and match from current object.
	 * A value {@code val} match this object when {@code (val & mask) == match}
	 * @return Pair<mask,match>
	 */
	public Pair<byte[], byte[]> generateMaskAndMatch(){
		byte[] mask = new byte[array.length/2];
		byte[] match = new byte[array.length/2];
		if(this.empty){
			Arrays.fill(mask, (byte)0x00);
			Arrays.fill(match, (byte)0xff);
		}else{
			Arrays.fill(mask, (byte)0x00);
			Arrays.fill(match, (byte)0x00);
			for(int i = 0; i < array.length; i++){
				for(int j = 0; j < 4; j++){
					int val = (array[i] >> 2*j);
					int index = (4*i+j)/8;
					int offset = (4*i+j)%8;
					if((val & 0x03) == 0x01){
						mask[index] = (byte) (mask[index] | (0x01 << offset));
						match[index] = (byte) (match[index] & (~(0x01 << offset)));
					}else if((val & 0x03) == 0x02){
						mask[index] = (byte) (mask[index] | (0x01 << offset));
						match[index] = (byte) (match[index] | (0x01 << offset));
					}else if((val & 0x03) == 0x03){
						mask[index] = (byte) (mask[index] & (~(0x01 << offset)));
						match[index] = (byte) (match[index] & (~(0x01 << offset)));
					}else{
						mask[index] = (byte) (mask[index] & (~(0x01 << offset)));
						match[index] = (byte) (match[index] | (0x01 << offset));
					}
				}
			}
		}
		return new ImmutablePair<>(mask, match);
	}
	
	/**
	 * Apply rewrite action to current ternary array
	 * @param mask the mask part of the rewrite action
	 * @param writer the writer part of the rewrite action
	 * @return the rewrote ternary array
	 */
	public TernaryArray applyRewrite(TernaryArray mask, TernaryArray writer){
		return this.byteAnd(mask).byteOr(writer);
	}
	
	/**
	 * @see #applyRewrite(TernaryArray, TernaryArray)
	 * @param mask the mask part of the write action
	 * @param writer the write part of the write action
	 * @return ternary array after rewrote
	 */
	public TernaryArray copyApplyRewrite(TernaryArray mask, TernaryArray writer){
		return this.clone().applyRewrite(mask, writer);
	}
	/**
	 * check if this array has bit 'x'
	 * @return true if has 'x'
	 */
	public boolean hasNoX(){
		if(this.empty)
			return false;
		for(int i = 0; i < array.length; i++){
			for(int j = 0; j < 4; j++){
				if(((array[i] >> 2*j) & 0x03) == 0x03)
					return false;
			}
		}
		return true;
	}

	/**
	 * empty or not
	 * @return true if empty
	 */
	public boolean isEmpty(){
		return this.empty;
	}
	
	/**
	 * get the length of this object
	 * @return the length
	 */
	public int getLength(){
		return array.length*4;
	}
	/**
	 * construct an array of length {@code len} fill with 'x'
	 * @param len the length of the array, in bit
	 * @return the Ternary Array
	 */
	public static TernaryArray getAllX(int len){
		TernaryArray t = new TernaryArray(len, true);
		Arrays.fill(t.array, (byte)0xff);
		t.empty = false;
		return t;
	}
	
	/**
	 * construct an array of length {@code len} filling with '1'
	 * @param len the length
	 * @return the ternary array
	 */
	public static TernaryArray getAllOne(int len){
		TernaryArray t = new TernaryArray(len, true);
		Arrays.fill(t.array, (byte)0xaa);
		t.empty = false;
		return t;
	}
	
	/**
	 * construct an array of length {@code len} filling with '0'
	 * @param len the length
	 * @return the ternary array
	 */
	public static TernaryArray getAllZero(int len){
		TernaryArray t = new TernaryArray(len, false);
		t.array = new byte[len/4];
		Arrays.fill(t.array, (byte)0x55);
		t.empty = false;
		return t;
	}

	/**
	 * generate ternary array from ternary string.
	 * Ternary string support four chars: '0', '1', 'x', 'z'
	 * @param val the ternary string
	 * @return the ternary array
	 */
	public static TernaryArray of(String val){
		TernaryArray t = new TernaryArray(val.length(), false);
		t.array = new byte[val.length()/4];
		t.empty = false;
		for(int i = 0; i < t.array.length; i++){
			for(int j = 0; j < 4; j++){
				if(val.charAt(i*4+j) == '0'){
					t.array[i] = (byte) (t.array[i] | (0x01 << 2*j));
				}else if(val.charAt(i*4+j) == '1'){
					t.array[i] = (byte) (t.array[i] | (0x02 << 2*j));
				}else if(val.charAt(i*4+j) == 'x'){
					t.array[i] = (byte) (t.array[i] | (0x03 << 2*j));
				}else if(val.charAt(i*4+j) == 'z'){
					t.empty = true;
				}else{
					throw new InvalidOperationException("unreconginized character in "
				+val+"! only '0', '1', 'z', 'x' supported");
				}
			}
		}
		return t;
	}
	
	/**
	 * generate ternary array with length of {@code len} from long value {@code val}.
	 * the every bit of {@code val} is recognized as '0' or '1' strictly.
	 * @param val the long value
	 * @param len the length
	 * @return the ternary array
	 */
	public static TernaryArray of(long val, int len){
		TernaryArray t = new TernaryArray(len, true);
		t.empty = false;
		for(int i = 0; i < t.array.length; i++){
			for(int j = 0; j < 4; j++){
				int index = len/4-1-i;
				if(((val >> (i*4+j)) & 0x01) == 0x01){
					t.array[index] = (byte) ((t.array[index] | (0x02 << (6-j*2))));
				}else{
					t.array[index] = (byte) ((t.array[index] | (0x01 << (6-2*j))));
				}
			}
		}
		return t;
	}
	
	/**
	 * set specified bit in this ternary array
	 * @param index the index of this bit
	 * @param pos the position of this bit
	 * @param val the value
	 * @return current object
	 */
	private TernaryArray setBit(int index, int pos, char val){
		if(index >= this.array.length || pos >= 4 || pos < 0 || 
				(val != '0' && val != '1' && val != 'x' && val != 'z'))
			throw new InvalidOperationException("invalid parameter!");
		int bit = (val == '0' ? 0x01 : (val == '1' ? 0x02 : (val == 'x' ? 0x03 : 0x00)));
		this.array[index] = (byte) (this.array[index] & (~ (0x03 << 2*pos)) | (bit << 2*pos));
		for(int i = 0; i < array.length; i++)
			if((array[i] & 0x03) == 0 || (array[i] & 0x0c) == 0 || (array[i] & 0x30) == 0 || (array[i] & 0xc0) == 0){
				this.empty = true;
				break;
			}
		return this;
	}
	
	/**
	 * set specified bit in this ternary array
	 * @param index the position of the bit
	 * @param val the bit value
	 * @return current object
	 */
	public TernaryArray setBit(int index, char val){
		return setBit(index/4, index%4, val);
	}
	
	/**
	 * get the specified bit
	 * @param index the bit position in the array
	 * @param pos the bit position
	 * @return the bit
	 */
	private char getBit(int index, int pos){
		final String indices = "z01x";
		if(index < 0 || index >= array.length || pos < 0 || pos >= 4)
			throw new InvalidOperationException("invalid parameter");
		return indices.charAt((array[index] >> 2*pos) & 0x03);
	}
	
	/**
	 * set bytes arbitrarily
	 * @param startPos the start position
	 * @param len the length of this operation
	 * @param val the value to be set
	 * @return the current object
	 */
	public TernaryArray setBytes(int startPos, int len, long val){
		if(startPos < 0 || startPos + len > array.length )
			throw new InvalidOperationException("wrong parameter");
		for(int i = startPos; i < startPos + len; i++){
			array[i] = (byte) ((val >> 8*i)&0xff);
		}
		return this;
	}
	
	/**
	 * get the specified bit
	 * @param pos the bit position
	 * @return the bit
	 */
	public char getBit(int pos){
		return getBit(pos/4, pos%4);
	}
	/**
	 * Retrieve the arbitrary bytes of current object.
	 * This function can only be used if you are familar with the implementation 
	 * @return the bytes of current object
	 */
	public byte[] getBytes(){
		return this.array;
	}

	public static TernaryArray tryMerge(TernaryArray a1, TernaryArray a2){
	    if(a1.getLength() != a2.getLength()) { return null; }
        if(a1.isEmpty()) { return a2.clone(); }
        if(a2.isEmpty()) { return a1.clone(); }
        // both non empty
        boolean findMerge = false;
        TernaryArray a3 = new TernaryArray(a1.getLength());
        for(int i = 0; i < a1.array.length; i++){
	        byte diff = (byte)(a1.array[i] ^ a2.array[i]);
            if(diff == 0xC0 || diff == 0x30 || diff == 0x0C || diff == 0x03) {
                if(!findMerge) {
                    findMerge = true;
                } else {
                    return null;
                }
                a3.array[i] = (byte)(a1.array[i] | diff);
            } else if(diff == 0x00) {
                a3.array[i] = a1.array[i];
            } else {
                return null;
            }
        }
        return a3;
    }
	@Override
	public TernaryArray clone(){
		TernaryArray t = new TernaryArray(this.array.length*4, false);
		t.array = this.array.clone();
		t.empty = this.empty;
		return t;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		if(empty == true)
			return 1231;
		result = prime * result + Arrays.hashCode(array);
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		TernaryArray other = (TernaryArray) obj;
		if(this.empty && other.empty)
			return true;
		if (!Arrays.equals(array, other.array))
			return false;
		if (empty != other.empty)
			return false;
		return true;
	}

	@Override
	public String toString() {
		if(this.empty)
			return "TernaryArray [empty]";
		StringBuffer sb = new StringBuffer();
		for(int i = 0; i < array.length; i++){
			for(int j = 0; j < 4; j++){
				if(((array[i] >> 2*j) & 0x03) == 0x00){
					sb.append('z');
				}else if(((array[i] >> 2*j) & 0x03) == 0x01){
					sb.append('0');
				}else if(((array[i] >> 2*j) & 0x03) == 0x02){
					sb.append('1');
				}else{
					sb.append('x');
				}
			}
		}
		return "TernaryArray [" + sb.toString() + "]";
	}

	public static void main(String[] args){
		System.out.println(of(0xcd23, 16));
		TernaryArray value =   TernaryArray.of("xxxxxxxxxxxxxxxx010000000011100100010100110000xx");
		TernaryArray mask =    TernaryArray.of("000000000000000011111111111111111111111111111111");
		TernaryArray rewrite = TernaryArray.of("000000000000000000000000000000000000000000000000");
		//System.out.println(mask.clone().byteNot().byteAnd(rewrite));
		System.out.println(of("0101"));
		System.out.println(value);
		System.out.println(mask.clone().byteAnd(value));
		System.out.println(value.clone().byteAnd(mask));
		System.out.println(value.clone().byteAnd(mask).byteOr(rewrite));
	}
}
