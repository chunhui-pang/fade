#ifndef __EQUIVALENT_CLASS_H
#define __EQUIVALENT_CLASS_H

#include <list>
#include <set>
#include <network/rule.h>
#include <network/pswitch.h>

namespace ns_ec
{
	using std::list;
	using std::set;
	using ns_network::rule;
	using ns_network::pswitch;
	
	class equivalent_class
	{
	private:
		list< set<const rule*> > rules;
		list<const pswitch*> path;
		
	public:
		equivalent_class();
		const list<const pswitch*>& get_path() const;
		const set<const rule*>& get_rules(const pswitch* sw) const;
		const list< set<const rule*> >& get_rules() const;
		int get_length() const;
		int get_size() const;

		bool add_next_node_info(const pswitch* sw, const set<const rule*>& rules);
		bool insert_node_info_to_first(const pswitch* sw, const set<const rule*>& rules);
	};
}

#endif
