#ifndef __TOPOLOGY_H
#define __TOPOLOGY_H

#include <set>
#include <map>

#include <network/pswitch.h>
#include <network/interface.h>

namespace ns_topo {
	
	using ns_network::pswitch;
	using ns_network::interface;
	using std::string;
		
	class topology
	{
	private:
		std::set<pswitch*> switches; // cache
		std::map<string, pswitch*> switch_names;
		std::map<interface*, interface*> links;
		std::map<unsigned int, interface*> port_ips; // cache
    
	public:
		topology();
		topology(const topology& topo);
		~topology();

		const std::set<pswitch*>& get_switches() const;
		pswitch* get_switch(string name);
		topology* add_switch(pswitch* sw);

		const std::map<interface*, interface*>& get_links() const;
		topology* add_link(interface* pre_hop, interface* next_hop);

		const interface* get_interface_by_ip(unsigned int ip) const;

		// update all cached objects according to switch_names and links
		void update_indices();
	};

}
#endif
