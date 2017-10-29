#include <utility>
#include <iostream>
#include <algorithm>
#include <vector>
#include <stdexcept>

#include <network/pswitch.h>
#include <common/util.h>
#include <parser/option_parser.h>

namespace ns_network {
	using ns_common::str2ip;
	using ns_parser::option_parser;
	using std::vector;
	using std::cout;
	using std::endl;
	using std::out_of_range;
	using std::logic_error;
	using std::make_pair;

	pswitch::pswitch() : name(), interfaces(), if_names(), if_ids(), if_ips(), destination2intf(), rules(), rule_masks(), trie_rule()
	{

	}
  
	pswitch::pswitch(const pswitch& sw)
	{
		this->name = sw.name;
		for(auto it = sw.interfaces.begin(); it != sw.interfaces.end(); it++){
			interface* intf = new interface(*(*it));
			this->add_interface(intf);
		}
		for(auto it = sw.rules.begin(); it != sw.rules.end(); it++){
			rule* r = new rule(**it);
			this->add_rule(r);
		}
	}

	pswitch::~pswitch()
	{
		for(auto it = this->interfaces.begin(); it != this->interfaces.end(); it++)
			delete (*it);
		for(auto it = this->rules.begin(); it != this->rules.end(); it++)
			delete (*it);
	}

	string pswitch::get_name() const
	{
		return this->name;
	}

	pswitch* pswitch::set_name(string name)
	{
		this->name = name;
		return this;
	}

	const std::set<interface*>& pswitch::get_interfaces() const
	{
		return this->interfaces;
	}

	const interface* pswitch::get_interface_by_id(int id) const
	{
		return this->if_ids.at(id);
	}

	const interface* pswitch::get_interface_by_name(const string& name) const
	{
		return this->if_names.at(name);
	}

	const interface* pswitch::get_interface_by_ip(const unsigned int& ip) const
	{
		if(this->if_ips.count(ip) == 0){
			return nullptr;
		} else {
			return this->if_ips.at(ip);
		}
		// for(auto it = this->interfaces.begin(); it != this->interfaces.end(); it++){
		// 	for(auto p_ip = (*it)->get_addrs().begin(); p_ip != (*it)->get_addrs().end(); p_ip++){
		// 		unsigned int mask = ( 0xFFFFFFFF << (32-p_ip->mask_len) );
		// 		if( (ip & mask) == p_ip->destination )
		// 			return *it;
		// 	}
		// }
		// return this->if_ips.at(ip);
	}

	const interface* pswitch::get_interface_by_destination(const std::pair<unsigned int, unsigned int> &dest_with_mask)
	{
		if(this->destination2intf.count(dest_with_mask) == 0)
			return nullptr;
		return this->destination2intf.at(dest_with_mask);
	}

	pswitch* pswitch::add_interface(interface* intf)
	{
		intf->set_switch(this);
		if(this->interfaces.insert(intf).second){
			this->if_names.insert(std::make_pair(intf->get_name(), intf));
			this->if_ids.insert(std::make_pair(intf->get_id(), intf));
			for(auto it = intf->get_addrs().begin(); it != intf->get_addrs().end(); it++)
			{
				this->if_ips.insert(std::make_pair(it->local_addr, intf));
				std::pair<unsigned, unsigned> dest_key = std::make_pair(it->destination, it->mask_len);
				if(this->destination2intf.count(dest_key) != 0)
				{
					if(option_parser::get_instance()->get_show_warnings())
						std::cout  << "duplicate destination address found: " << ns_common::ip2str(it->destination, 0xFFFFFFFF << (32-it->mask_len)) << std::endl;
				}
				else
				{
					this->destination2intf.insert(std::make_pair(std::make_pair(it->destination, it->mask_len), intf));
				}
			}
		}else{
			std::cout << "ignore duplicated interface: " << intf->get_name() << std::endl;
		}
		return this;
	}

	const std::set<rule*>& pswitch::get_rules() const
	{
		return this->rules;
	}

	pswitch* pswitch::add_rule(rule* rule)
	{
		rule->set_switch(this);
		if(false == this->rules.insert(rule).second){
			std::cout << "fail to insert duplicate rule '" << (*rule) << "'" << std::endl;
			return NULL;
		}
		// update current matches of current rule
		this->update_rule_match(rule);
		// insert it into the trie 
		this->trie_rule.insert_prefix(rule->get_match(), rule);
		// update match-rule index
		this->rule_masks.insert(make_pair(rule->get_match(), rule));
		return this;
	}

	set<const rule*> pswitch::get_match_rules(const unsigned int &ip, const unsigned int& mask) const
	{
		// set<const rule*> matches;
		// for(auto it = this->rules.begin(); it != this->rules.end(); it++){
		// 	if( (ip & ((*it)->get_mask())) == ((*it)->get_prefix() & (*it)->get_mask()) ){
		// 		matches.insert(*it);
		// 	}
		// }
		vector<rule*> local = this->trie_rule.search_match(std::make_pair(ip, mask));
		return set<const rule*>(local.begin(), local.end());
	}

	set<const rule*> pswitch::get_match_rules(const set< pair<unsigned int, unsigned int> >& prefixs) const
	{
		vector<rule*> local = this->trie_rule.search_match(prefixs);
		return set<const rule*>(local.begin(), local.end());
	}


	void pswitch::update_rule_match(rule* r)
	{
		r->reset_local_match();
		vector<rule*> matches = this->trie_rule.search_match(make_pair(r->get_prefix(), r->get_mask()));
		for(auto it = matches.begin(); it != matches.end(); it++){
			if((*it)->get_mask() < r->get_mask()){ // parent nodes
				(*it)->subtract_local_match(r->get_default_match());
			} else if((*it)->get_mask() > r->get_mask()){ // children
				r->subtract_local_match((*it)->get_default_match());
			} 
		}
	}

	bool operator < (const pswitch& left, const pswitch& right)
	{
		return left.get_name() < right.get_name();
	}

	bool operator == (const pswitch& left, const pswitch& right)
	{
		return left.get_name() == right.get_name();
	}

	ostream& operator << (ostream& os, const pswitch& sw)
	{
		os << "name='" << sw.get_name() + "', " << sw.get_interfaces().size() << " interfaces, " << sw.get_rules().size() << " rules";
		return os;
	}
}
