#ifndef __TRIE_HPP
#define __TRIE_HPP

#include <cstddef>
#include <utility>
#include <vector>
#include <set>
#include <queue>

#include <trie/trie_node.hpp>

namespace ns_trie{
	using std::pair;
	using std::vector;
	using std::set;

	template<class NC>
	class trie {
	private:
		trie_node<NC>* root;

		int count_tree(trie_node<NC>* root) const;
		set< trie_node<NC>* > search_node_match(const pair<unsigned int, unsigned int>& prefix) const;

	public:
		trie();
		virtual ~trie();

		// construct a trie
		trie<NC>* insert_prefix(const pair<unsigned int, unsigned int>& prefix, NC content);

		// remove a prefix (if the preifx do not exists, nothing is remvoed)
		int remove_prefix(const pair<unsigned int, unsigned int>& prefix);

		// search in a trie
		vector< NC > search_match(const pair<unsigned int, unsigned int>& prefix) const;

		// search prefixes in a trie
		vector< NC > search_match(const set< pair<unsigned int, unsigned int> >& prefixs) const;

		// size
		int size() const;

	};

	template<class NC>
	trie<NC>::trie()
	{
		this->root = nullptr;
	}

	template<class NC>
	trie<NC>* trie<NC>::insert_prefix(const pair<unsigned int, unsigned int>& prefix, NC content)
	{
		if(nullptr == this->root){
			this->root = new trie_node<NC>();
		}

		unsigned int bit_test = 0x80000000;  // used to test if mask has ended
		unsigned int addr = prefix.first;
		unsigned int mask = prefix.second;

		trie_node<NC>* pc = this->root;
		while((bit_test & mask) != 0){ // mask has not ended
			typename trie_node<NC>::slot slot = ((bit_test & addr) == 0) ? trie_node<NC>::BYTE_ZERO : trie_node<NC>::BYTE_ONE;
			if(nullptr == pc->get_pos(slot)){
				trie_node<NC>* node = new trie_node<NC>();
				node->set_prefix_from_parent(pc);
				pc->set_pos(slot, node);
			}
			pc = pc->get_pos(slot);
			bit_test = (bit_test >> 1);
		}
		// mark it as a leaf node
		pc->set_node_content(content);
		pc->mark_as_leaf_node();
		return this;
	}

	template<class NC>
	int trie<NC>::remove_prefix(const pair<unsigned int, unsigned int> &prefix)
	{
		unsigned int bit_test = 0x80000000;  // used to test if mask has ended
		unsigned int addr = prefix.first;
		unsigned int mask = prefix.second;

		trie_node<NC>* pc = this->root;
		trie_node<NC>* pre = nullptr;
		typename trie_node<NC>::slot slot;
		while((bit_test & mask) != 0){ // mask has not ended
			slot = ((bit_test & addr) == 0) ? trie_node<NC>::BYTE_ZERO : trie_node<NC>::BYTE_ONE;
			if(nullptr == pc->get_pos(slot)){
				// nothing is found
				return 0;
			}
			pre = pc;
			pc = pc->get_pos(slot);
			bit_test = (bit_test >> 1);
		}
		// not a node
		if(pc->is_leaf_node() == false)
			return 0;
		if(nullptr == pc->get_pos(trie_node<NC>::BYTE_ZERO) && nullptr == pc->get_pos(trie_node<NC>::BYTE_ONE)){
			if(pre == nullptr){ // root node
				pc->unmark_leaf_node();
			} else {
				pre->set_pos(slot, nullptr);
				delete pc;
			}
		} else {
			pc->unmark_leaf_node();
		}
		return 1;
	}

	template <class NC>
	set< trie_node<NC>* > trie<NC>::search_node_match(const pair<unsigned int, unsigned int> &prefix) const
	{
		set< trie_node<NC>* > matches;
		if(nullptr == this->root){
			return std::move(matches);
		}
		trie_node<NC>* pc = this->root;
		unsigned int bit_test = 0x80000000; // test bit
		unsigned int mask = prefix.second;
		unsigned int addr = prefix.first;
		
		trie_node<NC>* last_leaf = nullptr;
		while((bit_test & mask) != 0){
			if(nullptr != pc && pc->is_leaf_node()){
				last_leaf = pc;
			}
			typename trie_node<NC>::slot slot = ((bit_test & addr) == 0) ? trie_node<NC>::BYTE_ZERO : trie_node<NC>::BYTE_ONE;
			if(nullptr != pc->get_pos(slot)){
				pc = pc->get_pos(slot);
			} else {
				// have no children
				if(nullptr != last_leaf){
					matches.insert(last_leaf);
				}
				return std::move(matches);
			}
			bit_test = (bit_test >> 1);
		}
		std::queue<trie_node<NC>*> nodes;
		nodes.push(pc);
		pc->update_full_state();
		while(!nodes.empty()){
			trie_node<NC>* node = nodes.front();
			nodes.pop();
			trie_node<NC>* left_ptr = node->get_pos(trie_node<NC>::BYTE_ZERO);
			trie_node<NC>* right_ptr = node->get_pos(trie_node<NC>::BYTE_ONE);
			if(node->is_leaf_node() && !node->is_full_node()){
				// add a result
				matches.insert(node);
			}
			if(nullptr != left_ptr){
				nodes.push(left_ptr);
			}
			if(nullptr != right_ptr){
				nodes.push(right_ptr);
			}
		}
		return std::move(matches);

	}

		
	template<class NC>
	vector<NC> trie<NC>::search_match(const pair<unsigned int, unsigned int>& prefix) const
	{
		vector< NC > matches;
		set< trie_node<NC>* > match_nodes = this->search_node_match(prefix);
		for(auto node : match_nodes){
			matches.push_back(node->get_node_content());
		}
		return std::move(matches);
	}

	template <class NC>
	vector<NC> trie<NC>::search_match(const set<pair<unsigned int, unsigned int> > &prefixs) const
	{
		vector<NC> results;
		set< trie_node<NC>* > match_nodes;
		for(auto prefix : prefixs){
			set< trie_node<NC>* > local_nodes = this->search_node_match(prefix);
			match_nodes.insert(local_nodes.begin(), local_nodes.end());
		}
		for(auto node : match_nodes){
			results.push_back(node->get_node_content());
		}
		return std::move(results);
	}

	template<class NC>
	int trie<NC>::size() const
	{
		return count_tree(this->root);
	}

	template<class NC>
	int trie<NC>::count_tree(trie_node<NC>* root) const
	{
		if(nullptr == root){
			return 0;
		}
		int num = root->is_leaf_node() ? 1 : 0;
		return num + count_tree(root->get_pos(trie_node<NC>::BYTE_ZERO)) + count_tree(root->get_pos(trie_node<NC>::BYTE_ONE));
	}

	template<class NC>
	trie<NC>::~trie()
	{
		std::queue<trie_node<NC>*> nodes;
		if(nullptr != this->root){
			nodes.push(this->root);
		}
		while(!nodes.empty()){
			trie_node<NC>* node = nodes.front();
			nodes.pop();
			if(nullptr != node->get_pos(trie_node<NC>::BYTE_ZERO)){
				nodes.push(node->get_pos(trie_node<NC>::BYTE_ZERO));
			}
			if(nullptr != node->get_pos(trie_node<NC>::BYTE_ONE)){
				nodes.push(node->get_pos(trie_node<NC>::BYTE_ONE));
			}
			delete node;
		}
	}

}

#endif
