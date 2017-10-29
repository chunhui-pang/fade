package net.floodlightcontroller.applications.util.trie;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import net.floodlightcontroller.test.FloodlightTestCase;
import org.apache.commons.collections.CollectionUtils;
import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Set;
import java.util.TreeSet;

/**
 * A test trie node
 *
 * @author chunhui (chunhui.pang@outlook.com)
 */
class SimpleTernary implements TrieNodeRetrievable<Character>, Comparable {
	private char[] chars;
	public SimpleTernary(char a, char b, char c){
		chars = new char[]{a, b, c};
	}
	@Override
	public String toString(){
		return new String(chars);
	}
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(chars);
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
		SimpleTernary other = (SimpleTernary) obj;
		if (!Arrays.equals(chars, other.chars))
			return false;
		return true;
	}
	@Override
	public TrieNodeRetriever<Character> getRetriever() {
		return new TrieNodeRetriever<Character>() {
			private int curPos = 0;
			@Override
			public TrieNode<Character> getNextNode() {
					if(curPos >= chars.length)
						return null;
					return new CharNode(chars[curPos++]);
			}
		};
	}
	
	private class CharNode implements TrieNode<Character>{
		private char c;
		public CharNode(char c){
			this.c = c;
		}
		@Override
		public boolean hasIntersection(TrieNode<Character> right) {
			if(right instanceof CharNode == false)
				return false;
			char c1 = ((CharNode)right).c;
			return c1 == 'x' || c == 'x' || c1 == c;
		}

		@Override
		public boolean isEqual(TrieNode<Character> right) {
			if(right instanceof CharNode == false)
				return false;
			char c1 = ((CharNode)right).c;
			return c == c1;
		}
		@Override
		public boolean subsetOf(TrieNode<Character> right) {
			if(right instanceof CharNode == false)
				return false;
			char c1 = ((CharNode)right).c;
			return c1 == 'x' || c1 == c;
		}
		
	}

    @Override
    public int compareTo(Object o) {
        if(o instanceof SimpleTernary){
            SimpleTernary other = (SimpleTernary)o;
            for(int i = 0; i < this.chars.length; i++){
                if(other.chars[i]  > this.chars[i]) {
                    return 1;
                } else if(other.chars[i] < this.chars[i]) {
                    return -1;
                }
            }
            return 0;
        } else {
            return 0;
        }
    }
}

/**
 * Trie test
 * @author chunhui (chunhui.pang@outlook.com)
 */
public class TrieTest extends FloodlightTestCase {
    @Test
	public void doTest(){
        SimpleTernary[] ternaries = {
                new SimpleTernary('0', '0', '0'),
                new SimpleTernary('x', 'x', '0'),
                new SimpleTernary('x', 'x', '1'),
                new SimpleTernary('x', 'x', 'x')
        };
		Trie<SimpleTernary, Character> trie = new Trie<SimpleTernary, Character>();
        Assert.assertFalse(trie.remove(ternaries[0]));

        Arrays.stream(ternaries).forEach(trie::insert);
        Assert.assertTrue(trie.remove(ternaries[1]));
		Assert.assertTrue(trie.remove(ternaries[2]));
		Assert.assertTrue(trie.remove(ternaries[3]));

		trie.insert(ternaries[3]);
		trie.insert(ternaries[2]);
		trie.insert(ternaries[1]);
        Set<SimpleTernary> res = new TreeSet<>(trie.getIntersectedRules(ternaries[0]));
        Set<SimpleTernary> exp = new TreeSet<>(Arrays.asList(ternaries[0], ternaries[1], ternaries[3]));
        Assert.assertTrue(CollectionUtils.isEqualCollection(res, exp));

        res = new TreeSet<>(trie.getIntersectedRules(new SimpleTernary('x','x','x')));
        exp = new TreeSet<>(Arrays.asList(ternaries));
        Assert.assertTrue(CollectionUtils.isEqualCollection(res, exp));
	}
}
