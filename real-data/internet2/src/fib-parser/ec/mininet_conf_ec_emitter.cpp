#include <ec/mininet_conf_ec_emitter.h>
#include <network/interface.h>
#include <common/util.h>
#include <cstdlib>
#include <iomanip>
#include <vector>
#include <sstream>

namespace ns_ec
{

	bool operator < (const mininet_conf_ec_emitter::send_info& left, const mininet_conf_ec_emitter::send_info& right)
	{
		if (left.source_switch_id != right.source_switch_id)
			return left.source_switch_id < right.source_switch_id;
		if (left.destination_switch_id != right.destination_switch_id)
			return left.destination_switch_id < right.destination_switch_id;
		return left.prefix < right.prefix;
	}
	const std::string mininet_conf_ec_emitter::host_info::DELIMITER = "    ";
	mininet_conf_ec_emitter::host_info::host_info(const std::string& name, const std::string& attach_switch, unsigned attach_port,
												  const std::pair<unsigned, unsigned>& ip, unsigned mac,
												  const std::map<unsigned, unsigned>& arp_table,
												  const std::set<unsigned> delegate_ips, const std::multimap<unsigned, unsigned>& send_dsts)
		: name(name), attach_switch(attach_switch), attach_port(attach_port),
		  ip(ip), mac(mac), arp_table(arp_table),
		  delegate_ips(delegate_ips), send_dsts(send_dsts)
	{
	}

	mininet_conf_ec_emitter::host_info::host_info() : name(), attach_switch(), attach_port(0), ip(std::make_pair(0,0)), mac(0), arp_table(), delegate_ips(), send_dsts()
	{
	}

	mininet_conf_ec_emitter::host_info& mininet_conf_ec_emitter::host_info::set_name(const std::string &name)
	{
		this->name = name;
		return *this;
	}
	mininet_conf_ec_emitter::host_info& mininet_conf_ec_emitter::host_info::set_attach_switch(const std::string &attach_switch)
	{
		this->attach_switch = attach_switch;
		return *this;
	}
	mininet_conf_ec_emitter::host_info& mininet_conf_ec_emitter::host_info::set_attach_port(unsigned int attach_port)
	{
		this->attach_port = attach_port;
		return *this;
	}
	mininet_conf_ec_emitter::host_info& mininet_conf_ec_emitter::host_info::set_ip(const std::pair<unsigned int, unsigned int> &ip)
	{
		this->ip = ip;
		return *this;
	}
	std::pair<unsigned, unsigned> mininet_conf_ec_emitter::host_info::get_ip()
	{
		return this->ip;
	}
	mininet_conf_ec_emitter::host_info& mininet_conf_ec_emitter::host_info::set_mac(unsigned int mac)
	{
		this->mac = mac;
		return *this;
	}
	unsigned mininet_conf_ec_emitter::host_info::get_mac()
	{
		return this->mac;
	}
	mininet_conf_ec_emitter::host_info& mininet_conf_ec_emitter::host_info::add_arp_entry(unsigned int ip, unsigned int mac)
	{
		this->arp_table.insert(std::make_pair(ip, mac));
		return *this;
	}
	mininet_conf_ec_emitter::host_info& mininet_conf_ec_emitter::host_info::add_delegate_ips(unsigned int ip)
	{
		this->delegate_ips.insert(ip);
		return *this;
	}
	mininet_conf_ec_emitter::host_info& mininet_conf_ec_emitter::host_info::add_send_dst(unsigned int fake_src, unsigned int dst)
	{
		for(auto it = this->send_dsts.find(fake_src); it != this->send_dsts.upper_bound(fake_src); it++)
			if(it->second == dst)
				return *this;
		this->send_dsts.insert(std::make_pair(fake_src, dst));
		return *this;
	}

	std::ostream& operator << (std::ostream& os, const mininet_conf_ec_emitter::host_info& host_info)
	{
		std::ios::fmtflags original_flag(os.flags());
		const std::string delimiter = mininet_conf_ec_emitter::host_info::DELIMITER;
		os << delimiter << "{" << std::endl;
		os << delimiter << delimiter << std::left << std::setw(15) << "\"name\":" << '"' << host_info.name << "\"," << std::endl;
		os << delimiter << delimiter << std::left << std::setw(15) << "\"attach_switch\":" << '"' << host_info.attach_switch << "\"," << std::endl;
		os << delimiter << delimiter << std::left << std::setw(15) << "\"attach_port\":" << '"' << host_info.attach_port << "\"," << std::endl;
		os << delimiter << delimiter << std::left << std::setw(15) << "\"ip\":" << '"' << ns_common::ip2str(host_info.ip.first, host_info.ip.second) << "\"," << std::endl;
		os << delimiter << delimiter << std::left << std::setw(15) << "\"mac\":" << '"' << ns_common::to_mac(host_info.mac) << "\"," << std::endl;
		{// arp table
			os << delimiter << delimiter << std::left << std::setw(15) << "\"arp_table\":" << '{' << std::endl;
			auto entry_it = host_info.arp_table.begin();
			while(entry_it != host_info.arp_table.end())
			{
				std::ostringstream oss;
				oss << '"' << ns_common::ip2str(entry_it->first) << "\":";
				os << delimiter << delimiter << delimiter << std::left << std::setw(20) << oss.str() << '"' << ns_common::to_mac(entry_it->second) << '"';
				entry_it++;
				if (entry_it != host_info.arp_table.end())
					os << ',';
				os << std::endl;
			}
			os << delimiter << delimiter << "}," << std::endl;
		}
		{// delegate_ips
			os << delimiter << delimiter << std::left << std::setw(15) << "\"delegate_ips\":" << '[';
			auto ip_it = host_info.delegate_ips.begin();
			while(ip_it != host_info.delegate_ips.end())
			{
				os << '"' << ns_common::ip2str(*ip_it) << '"';
				ip_it++;
				if(ip_it != host_info.delegate_ips.end())
					os << ", ";
			}
			os << "]," << std::endl;
		}
		{// send_dsts
			os << delimiter << delimiter << std::left << std::setw(15) << "\"send_dsts\":" << '{' << std::endl;
			auto dst_it = host_info.send_dsts.begin();
			while(dst_it != host_info.send_dsts.end())
			{
				std::ostringstream oss;
				oss << '"' << ns_common::ip2str(dst_it->first) << "\":";
				os << delimiter << delimiter << delimiter << std::left << std::setw(20) << oss.str() << '[';
				auto upper_bound = host_info.send_dsts.upper_bound(dst_it->first);
				while(dst_it != upper_bound)
				{
					os << '"' << ns_common::ip2str(dst_it->second) << '"';
					dst_it++;
					if(dst_it != upper_bound)
						os << ", ";
				}
				os << "] ";
				if(dst_it != host_info.send_dsts.end())
					os << ',';
				os << std::endl;
			}
			os << delimiter << delimiter << '}' << std::endl;
		}
		os << delimiter << "}";
		os.flags(original_flag);
		return os;
	}

	
	mininet_conf_ec_emitter::mininet_conf_ec_emitter(const std::string& topo_file, const std::string& host_file, const std::string& rule_file,
													 const ns_topo::topology* topo, const ns_topo::rule_graph* rule_graph) :
		topo_conf(topo_file), host_conf(host_file), rule_conf(rule_file), topo(topo), rule_graph(rule_graph), has_error(false),
		switch2id(), id2switch(), switch2next_ports(), link2port_id(), fake_links(),
		switch2send_infos(),
		next_mac(1), /* 00:00:00:00:00:01 */
		next_ip(167772161) /* 10.0.0.1 */
	{
		this->has_error = topo_conf.fail() || host_conf.fail() || rule_conf.fail();
		this->has_error || (this->has_error = !this->generate_switch_ids());
		// this->has_error || (this->has_error = !this->write_topo_2_topo_conf());
	}

	bool mininet_conf_ec_emitter::emit(ns_ec::equivalent_class *ec)
	{
		if (this->has_error || this->topo_conf.fail() || this->host_conf.fail())
		{
			this->has_error = true;
			return false;
		}
		if(nullptr == (ec = this->do_filter(ec)))
			return true;
		// TODO: analyze the ecs and write host conf
		std::list<const ns_network::pswitch*>::const_iterator path_it = ec->get_path().begin();
		std::vector<const ns_network::rule*> cur_rules;
		std::vector<ns_network::rule_match> cur_matches;
		for(auto it = ec->get_rules(*path_it).begin(); it != ec->get_rules(*path_it).end(); it++)
		{
			cur_rules.push_back(*it);
			this->rules.insert(*it);
			cur_matches.push_back((*it)->get_local_match());
		}
		auto prev_path_it = path_it;
		while(++path_it != ec->get_path().end())
		{
			int prev_sw_id = this->switch2id.at(*prev_path_it);
			int cur_sw_id = this->switch2id.at(*path_it);
			this->fake_links.insert(std::make_pair(prev_sw_id, cur_sw_id));
			prev_path_it = path_it;
			
			std::set<const ns_network::rule*> node_rules = ec->get_rules(*path_it);
			int original_size = cur_rules.size();
			for(int i = 0; i < original_size; i++)
			{
				std::set<const ns_network::rule*> nxt_rules = this->rule_graph->get_next_hops(cur_rules.at(i));
				for(auto next_rule_it = nxt_rules.begin(); next_rule_it != nxt_rules.end(); next_rule_it++)
				{
					if(node_rules.count(*next_rule_it) != 0)
					{
						// update cur rules and matches
						this->rules.insert(*next_rule_it);
						cur_rules.push_back(*next_rule_it);
						ns_network::rule_match nxt_match(cur_matches.at(i));
						nxt_match = nxt_match.intersect_match((*next_rule_it)->get_local_match());
						cur_matches.push_back(nxt_match);
					}
				}
			}
			// remove the previous rules and matches
			std::vector<const ns_network::rule*>::iterator rule_it = cur_rules.begin();
			std::vector<ns_network::rule_match>::iterator match_it = cur_matches.begin();
			cur_rules.erase(rule_it, rule_it + original_size);
			cur_matches.erase(match_it, match_it + original_size);
		}
		// if we could save infomations?
		int source_switch_id, destination_switch_id;
		if(this->switch2id.count(ec->get_path().front()) == 0)
		{
			std::cerr << "switch " << ec->get_path().front()->get_name() << " is not in management." << std::endl;
			return false;
		}
		source_switch_id = this->switch2id.at(ec->get_path().front());
		if(this->switch2id.count(ec->get_path().back()) == 0)
		{
			std::cerr << "switch " << ec->get_path().back()->get_name() << " is not in management." << std::endl;
			return false;
		}
		destination_switch_id = this->switch2id.at(ec->get_path().back());
		// retrieve all matches and save it
		for(auto match_it = cur_matches.begin(); match_it != cur_matches.end(); match_it++)
		{
			std::set< std::pair<unsigned int, unsigned int> > prefixes = match_it->get_as_prefix();
			for(auto prefix_it = prefixes.begin(); prefix_it != prefixes.end(); prefix_it++)
				this->switch2send_infos.at(source_switch_id).insert(send_info(source_switch_id, destination_switch_id, *prefix_it));
		}
		return true;
	}

	bool mininet_conf_ec_emitter::generate_switch_ids()
	{
		int nxt_id = 1;
		for(auto it = this->topo->get_switches().begin(); it != this->topo->get_switches().end(); it++)
		{
			this->switch2id.insert(std::make_pair(*it, nxt_id));
			this->id2switch.insert(std::make_pair(nxt_id, *it));
			this->switch2next_ports.insert(std::make_pair(nxt_id, 1));
			this->switch2send_infos.insert(std::make_pair(nxt_id, std::set<send_info>()));
			nxt_id++;
		}
		return true;
	}

	bool mininet_conf_ec_emitter::write_topo_2_topo_conf()
	{
		if(this->has_error)
			return false;
		for(auto it = this->fake_links.begin(); it != this->fake_links.end(); it++)
		{
			int left_id = it->first;
			int right_id = it->second;
			if(left_id < right_id)
				continue;
			
			// request new port
			int left_port = this->request_new_port(left_id);
			int right_port = this->request_new_port(right_id);
			
			// assure we can insert entries
			if(this->link2port_id.count(left_id) == 0)
				this->link2port_id.insert(std::make_pair(left_id, std::map<int, int>()));
			if(this->link2port_id.count(right_id) == 0)
				this->link2port_id.insert(std::make_pair(right_id, std::map<int,int>()));
			if(this->link2port_id.at(left_id).count(right_id) != 0 || this->link2port_id.at(right_id).count(left_id) != 0)
			{
				std::cerr << "there are multiple links between some pair of switches, please check and resolve it." << std::endl;
				return false;
			}	

			// execute the insertion
			this->link2port_id.at(left_id).insert(std::make_pair(right_id, left_port));
			this->link2port_id.at(right_id).insert(std::make_pair(left_id, right_port));
		}
		// write into file
		this->topo_conf << "Internet2" << std::endl;
		this->topo_conf << this->topo->get_switches().size() << ' ' << this->fake_links.size() << std::endl;
		for(int id = 1; id <= this->topo->get_switches().size(); id++)
		{
			std::map<int, int> next_hops;
			if(this->link2port_id.count(id) != 0)
				next_hops = this->link2port_id.at(id);
			for(int nxt_id = 1; nxt_id <= this->topo->get_switches().size(); nxt_id++)
			{
				this->topo_conf << (next_hops.count(nxt_id) != 0 ? next_hops.at(nxt_id) : 0);
				if (nxt_id != this->topo->get_switches().size())
					this->topo_conf << ' ';
				else
					this->topo_conf << std::endl;
			}
		}
		// write an another empty line
		this->topo_conf << std::endl;
		return true;
	}

	bool mininet_conf_ec_emitter::write_rule_2_rule_conf(const ns_network::rule *rule)
	{
		if(this->has_error)
			return false;
		if (this->switch2id.count(rule->get_switch()) == 0)
		{
			std::cerr << "switch " << rule->get_switch()->get_name() << " is not in management." << std::endl;
			return false;
		}
		int sw_id = this->switch2id.at(rule->get_switch());
		std::string switch_name = "s" + std::to_string(sw_id);
		const ns_network::interface* nxt_intf = this->topo->get_interface_by_ip(rule->get_next_hop_ip());
		int nxt_port = -1;
		if(nullptr != nxt_intf)
			{
				if(this->switch2id.count(nxt_intf->get_switch()) == 0)
					{
						std::cerr << "switch " << nxt_intf->get_switch()->get_name() << " is not in management." << std::endl;
						return false;
					}
				int nxt_sw_id = this->switch2id.at(nxt_intf->get_switch());
				if(this->link2port_id.at(sw_id).count(nxt_sw_id) == 0)
					{
						std::cerr << "No link found from switch " << rule->get_switch()->get_name() << " to switch " << nxt_intf->get_switch()->get_name() << "." << std::endl;
						return false;
					}
				nxt_port = this->link2port_id.at(sw_id).at(nxt_sw_id);
			}
		this->rule_conf << std::left << std::setw(8) << ("s" + std::to_string(sw_id));
		this->rule_conf << std::left << std::setw(20) << ns_common::ip2str(rule->get_prefix(), rule->get_mask());
		if(-1 == nxt_port)
			this->rule_conf << std::left << std::setw(5) << "drop" << std::endl;
		else
			this->rule_conf << std::left << std::setw(5) << nxt_port << std::endl;
		return true;
	}

	int mininet_conf_ec_emitter::request_new_port(int sw_id)
	{
		return this->switch2next_ports.at(sw_id)++;
	}

	unsigned mininet_conf_ec_emitter::request_new_ip()
	{
		return this->next_ip++;
	}

	unsigned mininet_conf_ec_emitter::request_new_mac()
	{
		return this->next_mac++;
	}

	std::set< std::pair<unsigned int, unsigned int> > mininet_conf_ec_emitter::get_prefixes_by_src_and_dst_switch(int src_id, int dst_id)
	{
		std::set< std::pair<unsigned int, unsigned int> > result;
		for(auto info_it = this->switch2send_infos.at(src_id).begin(); info_it != this->switch2send_infos.at(src_id).end(); info_it++)
		{
			if(dst_id == info_it->destination_switch_id)
				mininet_conf_ec_emitter::nonoverlap_insert(result, info_it->prefix);
		}
		return std::move(result);
	}

	void mininet_conf_ec_emitter::nonoverlap_insert(std::set<std::pair<unsigned int, unsigned int> > &values, const std::pair<unsigned int, unsigned int> &val)
	{
		bool overlap = false;
		for(auto it = values.begin(); it != values.end(); it++)
		{
			if (it->second >= val.second && (it->first & val.second) == (val.first & val.second))
			{
				// *it < val   (split val, and insert seperately)
				ns_network::rule_match rm;
				rm.add_prefix(val);
				rm.subtract_prefix(*it);
				for(auto prefix_it = rm.get_as_prefix().begin(); prefix_it != rm.get_as_prefix().end(); prefix_it++)
					values.insert(*prefix_it);
				overlap = true;
				break;
			}
			else if(it->second < val.second && (it->first & it->second) == (val.first & it->second))
			{
				// *it > val  (split *it, do no insert)
				ns_network::rule_match rm;
				rm.add_prefix(*it);
				rm.subtract_prefix(val);
				values.erase(it);
				for(auto prefix_it = rm.get_as_prefix().begin(); prefix_it != rm.get_as_prefix().end(); prefix_it++)
					values.insert(*prefix_it);
				values.insert(val);
				overlap = true;
				break;
			}
		}
		if(!overlap)
			values.insert(val);
	}

	bool mininet_conf_ec_emitter::write_host_conf()
	{
		/* send_sw_id,  recv_sw_id, destination_ip */
		std::map<int, std::set<std::pair<int,unsigned int> > > send_ip;
		/* recv_sw_id, recv_ip */
		std::map<int, std::set<unsigned int> > recv_ip;
		for(int id = 1; id <= this->switch2id.size(); id++)
		{
			send_ip.insert(std::make_pair(id, std::set<std::pair<int,unsigned int> >()));
			recv_ip.insert(std::make_pair(id, std::set<unsigned int>()));
		}
		for(int send_id = 1; send_id <= this->switch2id.size(); send_id++)
		{
			for(int recv_id = 1; recv_id <= this->switch2id.size(); recv_id++)
			{
				std::set< std::pair<unsigned int, unsigned int> > sends = this->get_prefixes_by_src_and_dst_switch(send_id, recv_id);
				for(auto prefix_it = sends.begin(); prefix_it != sends.end(); prefix_it++)
				{
					// randomly choose target ip
					unsigned choosed_ip;
					bool inserted = false;
					do
					{
						choosed_ip = ns_common::random_choose_ip_from_prefix(*prefix_it);
						inserted = send_ip.at(send_id).insert(std::make_pair(recv_id, choosed_ip)).second;
					}
					while(!inserted);
					recv_ip.at(recv_id).insert(choosed_ip);
				}
			}
		}
		// generate host config, each switch is attached with one ip, and it sends packet fake with all host
		std::set<host_info*> hosts;
		std::map<int, host_info*> switch2host_info;
		for(auto sw_it = this->switch2id.begin(); sw_it != this->switch2id.end(); sw_it++)
		{
			int sw_id = sw_it->second;
			host_info* info = nullptr;
			if(switch2host_info.count(sw_id) == 0)
			{
				info = new host_info();
				info->set_mac(this->request_new_mac()); /* generate mac */
				switch2host_info.insert(std::make_pair(sw_id, info));
			}
			else
				info = switch2host_info.at(sw_id);
			unsigned host_ip;
			if(recv_ip.at(sw_id).size() != 0)
				host_ip = *(recv_ip.at(sw_id).begin());
			else
				host_ip = this->request_new_ip();
			info->set_name("h" + std::to_string(sw_id))
				.set_attach_switch("s" + std::to_string(sw_id))
				.set_attach_port(this->request_new_port(sw_id))
				.set_ip(std::make_pair(host_ip, 0xFFFFFFFF));
			for(auto recv_ip_it = recv_ip.at(sw_id).begin(); recv_ip_it != recv_ip.at(sw_id).end(); recv_ip_it++)
				/* delegate fake IP (response with faked arp reply message) */
				info->add_delegate_ips(*recv_ip_it);
			for(auto send_ip_it = send_ip.at(sw_id).begin(); send_ip_it != send_ip.at(sw_id).end(); send_ip_it++)
			{
				info->add_send_dst(info->get_ip().first, send_ip_it->second);
				if(switch2host_info.count(send_ip_it->first) == 0)
				{
					// create new host info for it
					host_info* tmp = new host_info();
					tmp->set_mac(this->request_new_mac());
					switch2host_info.insert(std::make_pair(send_ip_it->first, tmp));
				}
				unsigned target_mac = switch2host_info.at(send_ip_it->first)->get_mac();
				info->add_arp_entry(send_ip_it->second, target_mac);
			}
			hosts.insert(info);
		}
		this->host_conf << "[" << std::endl;
		auto host_it = hosts.begin();
		while(host_it != hosts.end())
		{
			this->host_conf << **host_it;
			host_it++;
			if(host_it != hosts.end())
				this->host_conf << ",";
			this->host_conf << std::endl;
		}
		this->host_conf << "]" << std::endl;
		for(auto it = hosts.begin(); it != hosts.end(); it++)
			delete *it;
		return true;
	}
	
	mininet_conf_ec_emitter::~mininet_conf_ec_emitter()
	{
		// TODO: simplify the send_infos and set up emit host config
		this->write_topo_2_topo_conf();
		this->write_host_conf();
		for(auto it = this->rules.begin(); it != this->rules.end(); it++)
			this->write_rule_2_rule_conf(*it);
		topo_conf.close();
		host_conf.close();
		rule_conf.close();
	}
		
}
