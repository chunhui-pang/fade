/**
 * trie_node.h:  define new class trie_node
 * This class represents for a new trie_node.
 * A leaf node (is_leaf_node()) indicate an end of a prefix.
 * However, a leaf node can also have children. 
 * 
 * Trie_node is a binary tree for the sake of easy implementation
 * Memory management if enforced by class trie
 */
#ifndef __TRIE_NODE_HPP
#define __TRIE_NODE_HPP

#include <cstddef>
#include <utility>
#include <initializer_list>
#include <algorithm>

namespace ns_trie{
	using std::pair;

	template<class NC>
	class trie_node {
	private:
		// 0, 1, *
		trie_node* next_level[2];
		// the parent node's prefix
		pair<unsigned int, unsigned int> prefix;
		// is leaf node
		bool leaf_node;
		/**
		 * is subtree constructs a full space? 
		 * e.g., 00*, 01*, 10*, 11* construct a full space ***. However, 00* and 01* cannot 
		 */
		bool full_node;
		NC node_content;
	
	public:
		enum slot { BYTE_ZERO = 0, BYTE_ONE};

		// constructor
		trie_node();
		trie_node(const pair<unsigned int, unsigned int>& prefix);

		// setter and getter
		trie_node<NC>* set_pos(slot slot, trie_node<NC>* node);
		trie_node<NC>* get_pos(slot slot) const;

		// prefix manupulation
		trie_node<NC>* set_prefix(const pair<unsigned int, unsigned int>& prefix);
		trie_node<NC>* set_prefix_from_parent(const trie_node<NC>* parent);
		pair<unsigned int, unsigned int> get_prefix() const;

		// content manipulation
		trie_node<NC>* set_node_content(NC nc);
		NC get_node_content() const;

		// leaf?
		bool is_leaf_node() const;
		trie_node<NC>* mark_as_leaf_node();
		trie_node<NC>* unmark_leaf_node();
		/** node full state manipulation
		 *  we DO NOT maintain full state AUTOMATICALLY, please call function update_full_state() to implicitly update the full state
		 */
		void update_full_state();
		trie_node<NC>* mark_as_full_node();
		trie_node<NC>* unmark_full_node();
		bool is_full_node() const;

		// clone
		trie_node<NC>* clone() const;
	
	};


	template<class NC>
	trie_node<NC>::trie_node()
	{
		auto init = std::initializer_list< trie_node<NC>* > ({nullptr, nullptr});
		std::copy(init.begin(), init.end(), this->next_level);
		this->prefix = std::make_pair(0, 0);
		this->leaf_node = false;
	}

	template<class NC>
	trie_node<NC>::trie_node(const pair<unsigned int, unsigned int>& prefix)
	{
		auto init = std::initializer_list< trie_node<NC>* > ({nullptr, nullptr});
		std::copy(init.begin(), init.end(), this->next_level);
		this->prefix = prefix;
		this->leaf_node = false;
	}

	template<class NC>
	trie_node<NC>* trie_node<NC>::set_pos(slot slot, trie_node<NC>* node)
	{
		this->next_level[slot] = node;
		return this;
	}

	template<class NC>
	trie_node<NC>* trie_node<NC>::get_pos(slot slot) const
	{
		return this->next_level[slot];
	}

	template<class NC>
	trie_node<NC>* trie_node<NC>::set_prefix(const pair<unsigned int, unsigned int>& prefix)
	{
		this->prefix = prefix;
		return this;
	}

	template<class NC>
	trie_node<NC>* trie_node<NC>::set_prefix_from_parent(const trie_node<NC>* parent)
	{
		slot slot = (parent->next_level[BYTE_ZERO] == this) ? BYTE_ZERO : BYTE_ONE;
		unsigned int mask, addr;
		mask = parent->prefix.second == 0 ? 0x80000000 : (unsigned int)(((int)parent->prefix.second) >> 1);
		switch(slot){
		case BYTE_ZERO:
			this->prefix = std::make_pair(parent->prefix.first, mask);
			break;
		case BYTE_ONE:
			addr = parent->prefix.first;
			addr = addr | (mask & (~parent->prefix.second));
			this->prefix = std::make_pair(addr, mask);
			break;
		}
		return this;
	}

	template<class NC>
	pair<unsigned int, unsigned int> trie_node<NC>::get_prefix() const
	{
		return this->prefix;
	}

	template<class NC>
	trie_node<NC>* trie_node<NC>::set_node_content(NC nc)
	{
		this->node_content = nc;
		return this;
	}

	template<class NC>
	NC trie_node<NC>::get_node_content() const
	{
		return this->node_content;
	}

	template<class NC>
	bool trie_node<NC>::is_leaf_node() const
	{
		return this->leaf_node;
	}

	template<class NC>
	trie_node<NC>* trie_node<NC>::mark_as_leaf_node()
	{
		this->leaf_node = true;
		return this;
	}

	template<class NC>
	void trie_node<NC>::update_full_state()
	{
		if(this->next_level[slot::BYTE_ZERO] != nullptr){
			this->next_level[slot::BYTE_ZERO]->update_full_state();
		}
		if(this->next_level[slot::BYTE_ONE] != nullptr){
			this->next_level[slot::BYTE_ONE]->update_full_state();
		}
		this->full_node = (this->next_level[slot::BYTE_ZERO] != nullptr && this->next_level[slot::BYTE_ZERO]->full_node == true) &&
			(this->next_level[slot::BYTE_ONE] != nullptr && this->next_level[slot::BYTE_ONE]->full_node == true);
	}

	template<class NC>
	trie_node<NC>* trie_node<NC>::mark_as_full_node()
	{
		this->full_node = true;
		return this;
	}

	template<class NC>
	trie_node<NC>* trie_node<NC>::unmark_full_node()
	{
		this->full_node = false;
		return this;
	}

	template<class NC>
	bool trie_node<NC>::is_full_node() const
	{
		return this->full_node;
	}

	template<class NC>
	trie_node<NC>* trie_node<NC>::unmark_leaf_node()
	{
		this->leaf_node = false;
		return this;
	}

	template<class NC>
	trie_node<NC>* trie_node<NC>::clone() const
	{
		if(nullptr == this){
			return nullptr;
		}

		trie_node<NC>* target = new trie_node<NC>();
		target->prefix = this->prefix;
		target->leaf_node = this->leaf_node;

		for(int i = 0; i < sizeof(this->next_level)/sizeof(this->next_level[0]); i++){
			if(this->next_level[i] != nullptr){
				target->next_level[i] = this->next_level[i]->clone();
			} else {
				target->next_level[i] = this->next_level[i];
			}
		}
		return target;
	}
}

#endif
