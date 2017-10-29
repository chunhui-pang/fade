#include <iostream>
#include <iterator>
#include <algorithm>
#include <utility>
#include <stdexcept>

#include <ec/switch_rule_analyzer.h>
#include <parser/option_parser.h>

namespace ns_ec
{
	using std::cerr;
	using std::endl;

		
	switch_rule_analyzer::switch_rule_analyzer(const pswitch* psw, const rule_graph* rg) : psw(psw), rg(rg), root(nullptr), slices(), paths(), ecs()
	{
		this->init_rules_node_root();
	}

	switch_rule_analyzer::rules_node::rules_node(const pswitch* psw, rules_node* prev_node) : psw(psw), rules(), leaf(), path(), next_switchs(), prev_node(prev_node)
	{
		this->path.push_back(psw);
	}

	switch_rule_analyzer::rules_node::~rules_node()
	{
		for(auto it = this->next_switchs.begin(); it != this->next_switchs.end(); it++)
			delete it->second;
	}

	void switch_rule_analyzer::rules_node::report_loop_error(const rule* r) const
	{
		if(ns_parser::option_parser::get_instance()->get_show_forwarding_loops()){
			cerr << "loop detected on " << (*r) << ", current path is: [";
			for(const pswitch* s : this->path){
				cerr << s->get_name() << ", ";
			}
			cerr << this->psw->get_name() << "]" <<endl;
		}
	}

	void switch_rule_analyzer::rules_node::add_rule(const ns_network::rule *rule)
	{
		this->rules.insert(rule);
	}

	void switch_rule_analyzer::rules_node::deduce_all_levels(const ns_topo::rule_graph *rg)
	{
		for(auto it = this->rules.begin(); it != this->rules.end(); it++){
			set<const rule*> next_hops = rg->get_next_hops(*it);
			// set<const rule*> next_hops = rg->get_next_hops(*it);
			if(next_hops.size() == 0)
			{
				this->leaf.insert(*it);
			}
			else
			{
				for(const rule* r : next_hops)
				{
					const pswitch* sw = r->get_switch();
					if(this->next_switchs.count(sw) == 0)
					{
						rules_node* nxt = new rules_node(sw, this);
						bool has_loop = false;
						nxt->path.clear();
						for(const pswitch* s : this->path)
						{
							if(sw == s) 	// loop detected
								has_loop = true;
							nxt->path.push_back(s);
						}
						if(has_loop)
						{
							nxt->report_loop_error(r);
							delete nxt;
							continue;
							// stop here
						}
						else
						{
							nxt->path.push_back(sw);
							this->next_switchs.insert(std::make_pair(sw, nxt));
						}
					}
					this->next_switchs.at(sw)->add_rule(r);
				}
			}
		}
		// deduce incursive
		for(auto it = this->next_switchs.begin(); it != this->next_switchs.end(); it++){
			it->second->deduce_all_levels(rg);
		}
	}

	set<const rule*> switch_rule_analyzer::rules_node::get_leaf() const
	{
		return this->leaf;
	}

	void switch_rule_analyzer::rules_node::collect_leaves(list< set<const ns_network::rule *> > &slices, list< list<const pswitch*> >& paths, const rule_graph* rg,  vector<equivalent_class*>& ecs) const
	{
		if(this->leaf.size() != 0){
			/* traceback and create ec */
			equivalent_class* ec = new equivalent_class();
			const rules_node* tmp = this;
			set<const rule*> tmp_rules = tmp->leaf;
			do {
				if(!ec->insert_node_info_to_first(tmp->psw, tmp_rules))
				{
					cerr << "loops detected in this ec, quit!";
					delete ec;
					ec = nullptr;
					break;
				}
				tmp = tmp->prev_node;
				if(nullptr != tmp)
				{
					set<const rule*> prev_rules;
					for(auto it = tmp_rules.begin(); it != tmp_rules.end(); it++)
					{
						const set<const rule*> pres = rg->get_prev_hops(*it);
						for(auto pit = pres.begin(); pit != pres.end(); pit++)
						{
							if (tmp->rules.count(*pit) != 0)
								prev_rules.insert(*pit);
						}
					}
					tmp_rules = prev_rules;
				}
			} while(nullptr != tmp);
			if(nullptr != ec)
				ecs.push_back(ec);
			slices.push_back(this->leaf);
			paths.push_back(this->path);
		}
		// iterating over all next hops
		for(auto it = this->next_switchs.begin(); it != this->next_switchs.end(); it++){
			it->second->collect_leaves(slices, paths, rg, ecs);
		}
	}

	void switch_rule_analyzer::analyze()
	{
		this->root->deduce_all_levels(this->rg);
		this->root->collect_leaves(this->slices, this->paths, this->rg, this->ecs);
	}
	
	const list< set<const rule*> >& switch_rule_analyzer::get_result_slices() const
	{
		return this->slices;
	}

	const list< list<const pswitch*> >& switch_rule_analyzer::get_result_paths() const
	{
		return this->paths;
	}

	const vector<equivalent_class*>& switch_rule_analyzer::get_equivalent_classes() const
	{
		return this->ecs;
	}

	void switch_rule_analyzer::init_rules_node_root()
	{
		this->root = new rules_node(this->psw);
		const set<const rule*>& ingress = this->rg->get_ingress_rules();
		for(const rule* r : ingress){
			if(r->get_switch() == this->psw){
				this->root->add_rule(r);
			}
		}
	}

	switch_rule_analyzer::~switch_rule_analyzer()
	{
		if (nullptr != root)
			delete root;
		for (auto it = ecs.begin(); it != ecs.end(); it++)
			delete (*it);
	}
}
