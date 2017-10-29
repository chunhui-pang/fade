#include <iostream>
#include <sstream>
#include <iomanip>

#include <network/pswitch.h>
#include <common/util.h>
#include <topo/rule_graph.h>
#include <parser/option_parser.h>

namespace ns_topo {
	using std::make_pair;
	using std::cout;
	using std::endl;
	using std::setw;
	using std::right;
	using std::ostringstream;
	
	using ns_parser::option_parser;
	using ns_common::ip2str;

	rule_graph::rule_graph() : rules(), next_hop_map( ), prev_hop_map( ), ingress_rules( ), egress_rules()
	{
    
	}

	rule_graph::rule_graph(const rule_graph& graph) : rules(), next_hop_map(graph.next_hop_map), prev_hop_map(graph.prev_hop_map), ingress_rules(graph.ingress_rules), egress_rules(graph.egress_rules)
	{
	}

	rule_graph::~rule_graph()
	{
		// do nothing
	}

	rule_graph* rule_graph::parse_topology(const ns_topo::topology *topo)
	{
		for(auto p_sw = topo->get_switches().begin(); p_sw != topo->get_switches().end(); p_sw++){
			const set<rule*>& rules = (*p_sw)->get_rules();
			for(auto p_rule = rules.begin(); p_rule != rules.end(); p_rule++){
				unsigned int next_hop_ip = (*p_rule)->get_next_hop_ip();
				if(0 == next_hop_ip){ // filter rule
					this->add_output_rule(*p_rule);
				}else{
					const interface* next_hop_interface = topo->get_interface_by_ip(next_hop_ip);
					if(next_hop_interface == nullptr){
						if(option_parser::get_instance()->get_show_unknown_next_hops()){
							cout << "switch '" << (*p_sw)->get_name() << "': next hop ip " << ip2str(next_hop_ip) << " not found..." << endl;
						}
						this->add_output_rule(*p_rule);
						continue;
					}
					const pswitch* next_hop_switch = next_hop_interface->get_switch();
					set<const rule*> matches = next_hop_switch->get_match_rules((*p_rule)->get_local_match().get_as_prefix());
					if(matches.size() > 1 && option_parser::get_instance()->get_show_multiple_next_hops()){
						int len = 0;
						for(auto p_match = matches.begin(); p_match != matches.end(); p_match++){
							if(p_match == matches.begin()){
								ostringstream oss;
								oss << "multiple match: " << **p_rule << " -> ";
								len = oss.str().size();
								cout << oss.str() << **p_match << endl;
							}else{
								cout << setw(len) << right << " -> " << **p_match << endl;
							}

						}
					}
					for(auto p_match = matches.begin(); p_match != matches.end(); p_match++)
						this->add_next_hop(*p_rule, *p_match);
				}
			}
		}
		return this;
	}

	rule_graph* rule_graph::add_next_hop(const rule *prev_hop, const rule *next_hop)
	{
		if(0 == this->next_hop_map.count(prev_hop))
			this->next_hop_map.insert(make_pair(prev_hop, set<const rule*>()));
		if(false == this->next_hop_map.at(prev_hop).insert(next_hop).second){
			cout << "insert duplicate next hop: " << *prev_hop << " -> " << *next_hop << endl;
			return NULL;
		}
		if(0 == this->prev_hop_map.count(next_hop))
			this->prev_hop_map.insert(make_pair(next_hop, set<const rule*>()));
		if(false == this->prev_hop_map.at(next_hop).insert(prev_hop).second){
			this->next_hop_map.at(prev_hop).erase(next_hop);
			cout << "insert duplicate next hop: " << *prev_hop << " -> " << *next_hop << endl;
			return NULL;
		}
		this->rules.insert(prev_hop);
		this->rules.insert(next_hop);
		if(0 != this->egress_rules.count(prev_hop))
			this->egress_rules.erase(prev_hop);
		if(0 == this->prev_hop_map.count(prev_hop))
			this->ingress_rules.insert(prev_hop);
		if(0 != this->ingress_rules.count(next_hop))
			this->ingress_rules.erase(next_hop);
		if(0 == this->next_hop_map.count(next_hop))
			this->egress_rules.insert(next_hop);
    
		return this;
	}

	rule_graph* rule_graph::add_output_rule(const rule *rule)
	{
		this->rules.insert(rule);
		this->egress_rules.insert(rule);
		if(0 == this->prev_hop_map.count(rule))
			this->ingress_rules.insert(rule);
		return this;
	}

	const set<const rule*> rule_graph::get_next_hops(const rule* rule) const
	{
		if(this->next_hop_map.count(rule) == 0){
			return {};
		} else {
			return this->next_hop_map.at(rule);
		}
	}

	const set<const rule*> rule_graph::get_prev_hops(const rule *rule) const
	{
		if(this->prev_hop_map.count(rule) == 0){
			return {};
		} else {
			return this->prev_hop_map.at(rule);
		}
	}

	const set<const rule*>& rule_graph::get_ingress_rules() const
	{
		return this->ingress_rules;
	}

	const set<const rule*>& rule_graph::get_egress_rules() const
	{
		return this->egress_rules;
	}


	ostream& operator << (ostream& os, const rule_graph& graph)
	{
		int n_dep = 0;
		for(auto it = graph.next_hop_map.begin(); it != graph.next_hop_map.end(); it++){
			n_dep += it->second.size();
		}
		os << "rule graph ["
		   << graph.rules.size() << " rules, " << graph.ingress_rules.size() << " ingress rules, " << graph.egress_rules.size() << " egress rules, "
		   << n_dep << " next hop dependencies]";
		return os;
	}
}
