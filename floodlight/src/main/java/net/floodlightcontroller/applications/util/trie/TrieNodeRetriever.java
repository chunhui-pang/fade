package net.floodlightcontroller.applications.util.trie;

/**
 * A interface to build trie tree
 *
 * @author chunhui (chunhui.pang@outlook.com)
 * @param <K> key node type
 */
public interface TrieNodeRetriever<K> {
	public TrieNode<K> getNextNode();
}
