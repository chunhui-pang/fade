package net.floodlightcontroller.applications.appmodule.rulegraph.rule;

import net.floodlightcontroller.applications.util.trie.TrieNode;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * This class implement a trie node with a rule node's header space match
 * @author chunhui  (chunhui.pang@outlook.com)
 * @see RuleTrieNodeRetriever
 * @see FlowRuleNode
 */
class RuleTrieNode implements TrieNode<Object> {
    private Object key;

    RuleTrieNode(Object key){
        this.key = key;
    }

    @Override
    public boolean hasIntersection(TrieNode<Object> right) {
        if(!(right instanceof RuleTrieNode)){
            return false;
        }
        /* check OFPort */
        if((this.key == null && ((RuleTrieNode) right).key instanceof OFPort) ||
                (this.key instanceof OFPort && ((RuleTrieNode) right).key == null) ||
                (this.key instanceof OFPort && ((RuleTrieNode) right).key instanceof OFPort && this.key.equals(((RuleTrieNode) right).key))){
            return true;
        }
        /* check byte in header space */
        if(this.key instanceof Byte && ((RuleTrieNode) right).key instanceof Byte){
            byte b1 = (byte) ((byte) this.key & (byte) (((RuleTrieNode) right).key));
            if ((b1 & 0x03) == 0 || (b1 & 0x0c) == 0 || (b1 & 0x30) == 0 || (b1 & 0xc0) == 0)
                return false;
            return true;
        }
        return false;
    }

    @Override
    public boolean isEqual(TrieNode<Object> right) {
        if(!(right instanceof RuleTrieNode)){
            return false;
        }
        if(this.key instanceof OFPort && ((RuleTrieNode) right).key instanceof OFPort){
            return this.key.equals(((RuleTrieNode) right).key);
        } else if(this.key instanceof Byte && ((RuleTrieNode) right).key instanceof Byte){
            return (byte)this.key == (byte) ((RuleTrieNode) right).key;
        } else {
            /* null == null ? */
            return this.key == ((RuleTrieNode) right).key;
        }
    }

    @Override
    public boolean subsetOf(TrieNode<Object> right) {
        if(!(right instanceof RuleTrieNode)){
            return false;
        }
        if(this.key instanceof OFPort){
            return ((RuleTrieNode) right).key == null || this.key.equals(((RuleTrieNode) right).key);
        } else if(this.key instanceof Byte){
            byte b1 = (byte)this.key, b2 = (byte) ((RuleTrieNode) right).key;
            byte cmp = (byte) (b1 ^ b2);
            if (cmp != 0 && (cmp & b2) != cmp)
                return false;
            else
                return true;
        } else {
            return false;
        }
    }
}