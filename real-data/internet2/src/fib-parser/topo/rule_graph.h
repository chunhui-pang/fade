#ifndef __RULE_GRAPH_H
#define __RULE_GRAPH_H

#include <set>
#include <map>
#include <iostream>
#include <vector>

#include <network/rule.h>
#include <topo/topology.h>

namespace ns_topo{
	typedef unsigned int uint;

	using std::set;
	using std::map;
	using std::ostream;
	using std::vector;
	using std::pair;

	using ns_network::rule;

	class rule_graph
	{
		friend ostream& operator << (ostream& os, const rule_graph& graph);
    
	private:
		set<const rule*> rules;
		map<const rule*, set<const rule*> > next_hop_map;
		map<const rule*, set<const rule*> > prev_hop_map;
		set<const rule*> ingress_rules;
		set<const rule*> egress_rules;
    
	public:
		rule_graph();
		rule_graph(const rule_graph& graph);
		~rule_graph();

		rule_graph* parse_topology(const topology* topo);

		rule_graph* add_next_hop(const rule* prev_hop, const rule* next_hop);
		rule_graph* add_output_rule(const rule* rule);
    
		const set<const rule*> get_next_hops(const rule* rule) const;
		const set<const rule*> get_prev_hops(const rule* rule) const;
		const set<const rule*>& get_ingress_rules() const;
		const set<const rule*>& get_egress_rules() const;
	};

	ostream& operator << (ostream& os, const rule_graph& graph);
}
#endif
