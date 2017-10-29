#ifndef __SWITCH_RULE_ANALYZER_H
#define __SWITCH_RULE_ANALYZER_H

#include <network/pswitch.h>
#include <network/rule.h>
#include <topo/rule_graph.h>
#include <topo/topology.h>
#include <ec/equivalent_class.h>
#include <set>
#include <list>
#include <map>
#include <vector>

namespace ns_ec
{
	using ns_network::pswitch;
	using ns_network::rule;
	using ns_topo::rule_graph;
	using ns_topo::topology;
	using std::set;
	using std::list;
	using std::multimap;
	using std::map;
	using std::vector;

	class switch_rule_analyzer
	{
	private:
		class rules_node
		{
		private:
			const pswitch* psw;
			set<const rule*> rules;
			set<const rule*> leaf;
			list<const pswitch*> path;
			map<const pswitch*, rules_node*> next_switchs;
			rules_node* prev_node; // backtrack

			void report_loop_error(const rule* r) const;
		public:
			rules_node(const pswitch* psw, rules_node* prev_node=NULL);
			~rules_node();
			void add_rule(const rule* rule);
			void deduce_all_levels(const rule_graph* rg);
			set<const rule*> get_leaf() const;
			void collect_leaves(list< set<const rule*> >& slides, list< list<const pswitch*> >& paths, const rule_graph* rg, vector<equivalent_class*>& ecs) const;
		};

		const pswitch* psw;
		const rule_graph* rg;
		rules_node* root;
		list< set<const rule*> > slices;
		list< list<const pswitch*> > paths;
		vector< equivalent_class* > ecs;

		void init_rules_node_root();

	public:
		switch_rule_analyzer(const pswitch* psw, const rule_graph* rg);
		~switch_rule_analyzer();
		void analyze();
		const list< set<const rule*> >& get_result_slices() const;
		const list< list<const pswitch*> >& get_result_paths() const;
		const vector<equivalent_class*>& get_equivalent_classes() const;
	};
}
#endif
