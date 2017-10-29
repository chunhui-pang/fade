package net.floodlightcontroller.applications.util.trie;

/**
 *
 * This class defines a trie node interface, and a value stored in trie should be composed of a list of trie nodes.
 * Trie Nodes must support intersection, subsection operations.
 *
 * @author chunhui (chunhui.pang@outlook.com)
 * @param <K> the key of the trie tree
 */
public interface TrieNode <K>{
    /**
     * Check if current object and {@code right} have any intersection
     */
    public boolean hasIntersection(TrieNode<K> right);

    /**
     * Check if current object and {@code right} is equal
     */
    public boolean isEqual(TrieNode<K> right);
    
    /**
     * Check if current node is the subset of node {@code right}
     * @param right another node
     * @return true if current node is the subset of the other node
     */
    public boolean subsetOf(TrieNode<K> right);
}
