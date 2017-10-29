#include <ec/equivalent_class.h>
namespace ns_ec
{
	equivalent_class::equivalent_class() : rules(), path()
	{
		
	}

	const list<const pswitch*>& equivalent_class::get_path() const
	{
		return this->path;
	}

	const set<const rule*>& equivalent_class::get_rules(const ns_network::pswitch *sw) const
	{
		auto path_pos = this->path.begin();
		auto rule_pos = this->rules.begin();
		while(path_pos != path.end())
		{
			if (sw == *path_pos)
				return *rule_pos;
			path_pos++;
			rule_pos++;
		}
		return std::move(set<const rule*>());
	}

	const list< set<const rule*> >& equivalent_class::get_rules() const
	{
		return this->rules;
	}

	int equivalent_class::get_length() const
	{
		return this->path.size();
	}

	int equivalent_class::get_size() const
	{
		if(0 == this->rules.size())
			return 0;
		else
			return this->rules.front().size();
	}

	bool equivalent_class::add_next_node_info(const ns_network::pswitch *sw, const set<const ns_network::rule *> &rules)
	{
		if(find(path.begin(), path.end(), sw) == path.end())
		{
			this->path.push_back(sw);
			this->rules.push_back(rules);
			return true;
		}
		return false;
	}

	bool equivalent_class::insert_node_info_to_first(const ns_network::pswitch *sw, const set<const ns_network::rule *> &rules)
	{
		if(find(path.begin(), path.end(), sw) == path.end())
		{
			this->path.push_front(sw);
			this->rules.push_front(rules);
			return true;
		}
		return false;
	}
}
