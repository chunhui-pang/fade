package net.floodlightcontroller.applications.appmodule.rulegraph.rule;

import net.floodlightcontroller.applications.appmodule.rulegraph.IRuleNode;
import net.floodlightcontroller.applications.util.hsa.TernaryArray;
import net.floodlightcontroller.applications.util.trie.TrieNode;
import net.floodlightcontroller.applications.util.trie.TrieNodeRetriever;
import org.projectfloodlight.openflow.types.OFPort;

/**
 * Implements {@link FlowRuleNode} as a trie node,
 * by which we can organize all nodes as a trie tree, and provide efficient search operation(exactly search or search matches).
 * However, as header space is actually an array, we ony support a "coarse-grained" header space, i.e., ternary array,
 * and the header space is subset of the ternary array.
 *
 */
public class RuleTrieNodeRetriever implements TrieNodeRetriever<Object> {
    private OFPort inPort;
    private TernaryArray canMatch;
    private int currentPos; /* pos == 0 ? inPort : canMatch[pos-1] */

    public RuleTrieNodeRetriever(IRuleNode node){
        this.inPort = node.getInPort();
        this.canMatch = node.getMatchHS();
        this.currentPos = 0;
    }

    @Override
    public TrieNode<Object> getNextNode() {
        if(currentPos > canMatch.getBytes().length){
            return null;
        }
        final Object key = (currentPos == 0 ? this.inPort : this.canMatch.getBytes()[currentPos-1]);
        currentPos++;
        return new RuleTrieNode(key);
    }
}

