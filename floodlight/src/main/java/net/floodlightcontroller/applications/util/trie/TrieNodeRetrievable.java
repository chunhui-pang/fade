package net.floodlightcontroller.applications.util.trie;

/**
 * An interface which is used to generate a {@link TrieNodeRetriever},
 * by which the object can be inserted into a trie tree
 *
 * @author chunhui (chunhui.pang@outlook.com)
 * @param <T> the tree node key type
 */
public interface TrieNodeRetrievable<T> {
	public TrieNodeRetriever<T> getRetriever();
}
