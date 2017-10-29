#ifndef __PSWITCH_H
#define __PSWITCH_H

#include <set>
#include <map>
#include <string>
#include <iostream>
#include <string>

#include <network/rule.h>
#include <network/interface.h>
#include <trie/trie.hpp>

namespace ns_network {
	using std::set;
	using std::pair;
	using std::string;
	using std::map;
	using ns_trie::trie;
	using std::istream;
	using std::ostream;
	
	class pswitch
	{
	private:
		string name;
		set<interface*> interfaces;
		map<string, interface*> if_names; // name -> intf
		map<int, interface*> if_ids;		// id -> intf
		map<unsigned int, interface*> if_ips;		// local_addr -> intf
		map<pair<unsigned, unsigned>, interface*> destination2intf; // destination addr -> intf
    
		set<rule*> rules;
		map< pair<unsigned int, unsigned int>, rule* > rule_masks;
		trie<rule*> trie_rule;
    
	public:
		pswitch();
		pswitch(const pswitch& sw);
		~pswitch();
    
		string get_name() const;
		pswitch* set_name(string name);

		const set<interface*>& get_interfaces() const;
		const interface* get_interface_by_id(int id) const;
		const interface* get_interface_by_name(const string& name) const;
		const interface* get_interface_by_ip(const unsigned int& ip) const;
		/**
		 * dest_with_mask:  <dest, mask_len>
		 */
		const interface* get_interface_by_destination(const std::pair<unsigned, unsigned>& dest_with_mask);
    
		pswitch* add_interface(interface* intf);

		const set<rule*>& get_rules() const;
		pswitch* add_rule(rule* rule);

		set<const rule*> get_match_rules(const unsigned int& ip, const unsigned int& mask) const;
		set<const rule*> get_match_rules(const set< pair<unsigned int, unsigned int> >& prefixs) const;
    
	private:
		/**
		 * update the match of current rule r and matches of other rules
		 */
		void update_rule_match(rule* r);
	};

	bool operator < (const pswitch& lhs, const pswitch& rhs);
	bool operator == (const pswitch& left, const pswitch& right);
	ostream& operator << (ostream& os, const pswitch& lhs);

}
#endif
