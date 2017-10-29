package net.floodlightcontroller.applications.appmodule.rulegraph.rule;

import net.floodlightcontroller.applications.util.hsa.TernaryArray;
import net.floodlightcontroller.applications.util.trie.TrieNode;
import net.floodlightcontroller.applications.util.trie.TrieNodeRetrievable;
import net.floodlightcontroller.applications.util.trie.TrieNodeRetriever;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * Makes ternary array can be matched by trie tree
 * @author chunhui (chunhui.pang@outlook.com)
  */
public class TernaryArrayTrieNodeRetrievable implements TrieNodeRetrievable<Object> {
        private final OFPort inPort;
        private final TernaryArray canMatch;

    @Override
    public TrieNodeRetriever<Object> getRetriever() {
        return new TrieNodeRetriever<Object>() {
            private int currentPos = 0;
            @Override
            public TrieNode<Object> getNextNode() {
                if(currentPos > canMatch.getBytes().length){
                    return null;
                }
                final Object key = (currentPos == 0 ? inPort : canMatch.getBytes()[currentPos-1]);
                currentPos++;
                return new RuleTrieNode(key);
            }
        };
    }

    public TernaryArrayTrieNodeRetrievable(OFPort port, TernaryArray array){
            this.inPort = port;
            this.canMatch = array;
        }
}
