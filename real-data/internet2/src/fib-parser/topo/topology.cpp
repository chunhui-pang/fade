#include <utility>
#include <topo/topology.h>

namespace ns_topo {
	topology::topology() : switches(), switch_names(), links(), port_ips()
	{
    
	}

	topology::~topology()
	{
		for(auto it = this->switches.begin(); it != this->switches.end(); it++)
			delete *it;
	}

	topology::topology(const topology& topo)
	{
		for(auto it = this->switches.begin(); it != this->switches.end(); it++){
			pswitch* ps = new pswitch(**it);
			this->add_switch(ps);
		}
		// update links
		this->update_indices();
	}

	const std::set<pswitch*>& topology::get_switches() const
	{
		return this->switches;
	}

	pswitch* topology::get_switch(string name)
	{
		return this->switch_names.at(name);
	}

	topology* topology::add_switch(pswitch* sw)
	{
		if(true == this->switches.insert(sw).second && true == this->switch_names.insert(std::make_pair(sw->get_name(), sw)).second){
			for(auto it_intf = sw->get_interfaces().begin(); it_intf != sw->get_interfaces().end(); it_intf++){
				for(auto p_ip = (*it_intf)->get_addrs().begin(); p_ip != (*it_intf)->get_addrs().end(); p_ip++){
					this->port_ips.insert(std::make_pair(p_ip->local_addr, *it_intf));
				}
			}
			// update links
			for(auto it_intf = sw->get_interfaces().begin(); it_intf != sw->get_interfaces().end(); it_intf++)
			{
				for(auto p_ip = (*it_intf)->get_addrs().begin(); p_ip != (*it_intf)->get_addrs().end(); p_ip++)
				{
					std::pair<unsigned, unsigned> target_destination = std::make_pair(p_ip->destination, p_ip->mask_len);
					for(auto sw_it = this->switches.begin(); sw_it != this->switches.end(); sw_it++)
					{
						if(*sw_it == sw)
							continue;
						const interface* nxt_intf = (*sw_it)->get_interface_by_destination(target_destination);
						if(nullptr != nxt_intf)
						{
							this->add_link(*it_intf, const_cast<interface*>(nxt_intf));
							break;
						}
					}
				}
			}
		}else{
			this->switches.erase(sw);
			std::cout << "ingore duplicate switch: " << *sw << std::endl;
		}
		return this;
	}

	const std::map<interface*, interface*>& topology::get_links() const
	{
		return this->links;
	}

	topology* topology::add_link(interface* pre_hop, interface* next_hop)
	{
		this->links.insert(std::make_pair(pre_hop, next_hop));
		return this;
	}

	const interface* topology::get_interface_by_ip(unsigned int ip) const
	{
		if(this->port_ips.count(ip) == 0){
			return nullptr;
		} else {
			return this->port_ips.at(ip);
		}
	}

	/** 
	 * update indices according to switches and links
	 */
	void topology::update_indices()
	{
		// clear switch_names and port_ips
		this->switch_names.clear();
		this->port_ips.clear();

		// reconstruct switch_names and port_ips
		for(auto it = this->switches.begin(); it != this->switches.end(); it++){
			this->switch_names.insert(std::make_pair((*it)->get_name(), *it));
			for(auto p_intf = (*it)->get_interfaces().begin(); p_intf != (*it)->get_interfaces().end(); p_intf++){
				for(auto p_ip = (*p_intf)->get_addrs().begin(); p_ip != (*p_intf)->get_addrs().end(); p_ip++)
					this->port_ips.insert(std::make_pair(p_ip->local_addr, *p_intf));
			}
		}
	}
}
