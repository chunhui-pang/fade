package net.floodlightcontroller.applications.util.trie;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Stack;

/**
 * A trie tree implementation.
 *
 * <ul>
 *     <li>{@link TrieNode} represent a node of the tree, values stored in the trie should have a list of nodes</li>
 *     <li>{@link TrieNodeRetrievable} is a interface by which a object can return a {@link TrieNodeRetriever} to retrieve {@link TrieNode}</li>
 * </ul>
 *
 * @author chunhui (chunhui.pang@outlook.com)
 *
 * @param <T> The stored Object
 * @param <K> key type stored in every level
 */
public class Trie<T extends TrieNodeRetrievable<K>, K> {
    private class Node {
    	private TrieNode<K> key;
    	private List<Node> nextLevel;
    	private T obj;
		public Node(TrieNode<K> key, List<Trie<T, K>.Node> nextLevel, T obj) {
			super();
			this.key = key;
			this.nextLevel = nextLevel;
			this.obj = obj;
		}
		public TrieNode<K> getKey() {
			return key;
		}
		public List<Node> getNextLevel() {
			return nextLevel;
		}
		public T getObj() {
			return obj;
		}	
    }
    private List<T> objs;
    private List<Node> rootLevel;
    
    public Trie(){
    	this.objs = new LinkedList<T>();
    	this.rootLevel = new LinkedList<Node>();
    }

    /**
     * return if this node is inserted
     * @param node
     * @return true if this node doesn't exist and false if exists
     */
    public boolean insert(T node){
    	if(this.objs.contains(node))
    		return false;
    	TrieNodeRetriever<K> retriever = node.getRetriever();
    	TrieNode<K> trieNode = null;
    	List<Node> level = rootLevel;
    	while( (trieNode = retriever.getNextNode()) != null){
    		boolean find = false;
    		Iterator<Node> it = level.iterator();
    		while(it.hasNext()){
    			Node next = it.next();
    			if(next.getKey().isEqual(trieNode)){
    				level = next.getNextLevel();
    				find = true;
    				break;
    			}
    		}
    		if(find == false)
    			break;
    	}
    	if(trieNode == null){
    		return false;
    	}else{
    		Node n = null;
    		do{
    			n = new Node(trieNode, new LinkedList<Trie<T,K>.Node>(), null);
    			level.add(n);
    			level = n.getNextLevel();
    		}while((trieNode = retriever.getNextNode()) != null);
    		n.obj = node;
    		this.objs.add(node);
    		return true;
    	}
    }

    /**
     * remove a node from the trie tree
     * @param node the node to be removed
     * @return true if this node exists
     */
    public boolean remove(T node){
    	if(this.objs.contains(node) == false)
    		return false;
    	Stack<Node> stack = new Stack<Trie<T,K>.Node>();
    	TrieNodeRetriever<K> retriever = node.getRetriever();
    	TrieNode<K> trieNode = null;
    	List<Node> level = rootLevel;
    	while((trieNode = retriever.getNextNode()) != null){
    		boolean find = false;
    		Iterator<Node> it = level.iterator();
    		while(it.hasNext()){
    			Node next = it.next();
    			if(next.key.isEqual(trieNode)){
    				stack.push(next);
    				level = next.getNextLevel();
    				find = true;
    				break;
    			}
    		}
    		if(find == false)
    			break;
    	}
    	if(trieNode != null)
    		return false;
    	Node next = stack.pop();
    	Node pre = null;
    	while(stack.isEmpty() == false){
    		pre = stack.pop();
    		pre.getNextLevel().remove(next);
    		if(pre.getNextLevel().size() == 0){
        		next = pre;
    		}else{
    			break;
    		}
    	}
    	if(stack.size() == 0)
    		rootLevel.remove(next);
    	this.objs.remove(node);
    	return true;
    }

    /**
     * Retrieving all rules which have intersection with the given rule
     * @param node a representation of rule
     * @return a set of rules
     */
    public List<T> getIntersectedRules(TrieNodeRetrievable<K> node){
    	List<Node> queue = new LinkedList<Trie<T,K>.Node>();
    	List<Node> tmp = new LinkedList<Trie<T,K>.Node>();
    	List<T> result = new ArrayList<T>(tmp.size());
    	TrieNodeRetriever<K> retriever = node.getRetriever();
    	queue.addAll(rootLevel);
    	TrieNode<K> tnode = null;
    	while((tnode = retriever.getNextNode()) != null){
    		tmp.clear();
    		for(Node n : queue){
    			if(n.getKey().hasIntersection(tnode)){
    				if(n.getObj() != null)
    					result.add(n.getObj());
    				else
    					tmp.addAll(n.getNextLevel());
    			}
    		}
    		List<Node> swp = queue;
    		queue = tmp;
    		tmp = swp;
    	}
    	return result;
    }
    /**
     * Retrieving all rules which are subset of the given rule
     * @param node a representation of rule
     * @return a set of rules
     */
    public List<T> getSubsetRules(TrieNodeRetrievable<K> node){
    	List<Node> queue = new LinkedList<Trie<T,K>.Node>();
    	List<Node> tmp = new LinkedList<Trie<T,K>.Node>();
    	List<T> result = new ArrayList<T>(tmp.size());
    	TrieNodeRetriever<K> retriever = node.getRetriever();
    	queue.addAll(rootLevel);
    	TrieNode<K> tnode = null;
    	while((tnode = retriever.getNextNode()) != null){
    		tmp.clear();
    		for(Node n : queue){
    			if(n.getKey().subsetOf(tnode)){
    				if(n.getObj() != null)
    					result.add(n.getObj());
    				else
    					tmp.addAll(n.getNextLevel());
    			}
    		}
    		List<Node> swp = queue;
    		queue = tmp;
    		tmp = swp;
    	}
    	return result;
    }
    /**
     * Retrieving a reference of the given node
     * @param node the node
     * @return the reference
     */
    public T getReference(TrieNodeRetrievable<K> node){
    	List<T> intersects = this.getIntersectedRules(node);
    	if(intersects.size() != 1){
    		return null;
		} else {
    		/* if this node is equals to the target */
    		if(intersects.get(0).equals(node)){
    			return intersects.get(0);
			} else {
    			return null;
			}
		}
    }
    public List<T> getNodes(){
    	return this.objs;
    }
}
