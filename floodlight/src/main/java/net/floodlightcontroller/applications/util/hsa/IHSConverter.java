package net.floodlightcontroller.applications.util.hsa;


import org.apache.commons.lang3.tuple.Pair;
import org.projectfloodlight.openflow.protocol.OFFactory;

import java.util.List;

public interface IHSConverter<T> {

	/**
	 * Convert {@code obj} to ternary array
	 * @param obj the object to be converted
	 * @return the ternary array
	 */
	 TernaryArray parse(T obj);

	/**
	 * Parsing rewrite operation from the {@code obj}
	 * @param obj the original object 
	 * @return a pair with the left one denote the mask, and the right one deonte the rewriter
	 */
	Pair<TernaryArray, TernaryArray> parseRewriter(T obj);

	/**
	 * Convert {@code obj} to header space
	 * @param obj the object
	 * @return header space
	 */
	 HeaderSpace parseHeaderSpace(T obj);

	/**
	 * Convert a ternary array back to object
	 * @param array the ternary array
	 * @param ofFactory the openflow factory
	 * @return the object
	 */
	T read(TernaryArray array, OFFactory ofFactory);

	/**
	 * Convert a header space back to objects
	 * @param hs the header space
     * @param ofFactory the openflow factory
	 * @return objects
	 */
	List<T> read(HeaderSpace hs, OFFactory ofFactory);
}
