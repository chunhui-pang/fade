#ifndef __MININET_CONF_EC_EMITTER_H
#define __MININET_CONF_EC_EMITTER_H

#include <fstream>
#include <map>
#include <set>
#include <ec/ec_emitter.h>
#include <topo/topology.h>
#include <topo/rule_graph.h>


namespace ns_ec
{
	/**
	 * emit mininet config file for python scripts.
	 * @see $projectRoot/experiment-script/testcases/generalaggregatedtest.py
	 */
	
	class mininet_conf_ec_emitter : public ec_emitter
	{
	private:
		std::ofstream topo_conf;
		std::ofstream host_conf;
		std::ofstream rule_conf;
		const ns_topo::topology* topo;
		const ns_topo::rule_graph* rule_graph;

		std::map<const ns_network::pswitch*, int> switch2id;
		std::map<int, const ns_network::pswitch*> id2switch;
		std::map<int, int> switch2next_ports;
		std::set<const ns_network::rule*> rules;
		/* we cannot access the real link, so we just "create" links from rule graph */
		std::set< std::pair<int, int> > fake_links; 
		std::map<int, std::map<int, int> > link2port_id;
		unsigned int next_mac;
		unsigned int next_ip;
		
		struct send_info
		{
			int source_switch_id;
			int destination_switch_id;
			std::pair<unsigned int, unsigned int> prefix;
			send_info(int source, int dest, const std::pair<unsigned int, unsigned int>& prefix) : source_switch_id(source), destination_switch_id(dest), prefix(prefix){}
			friend bool operator < (const send_info& left, const send_info& right);
		};
		friend bool operator < (const send_info& left, const send_info& right);

		std::map<int, std::set<send_info> > switch2send_infos;
		bool has_error;

		class host_info
		{
		private:
			std::string name;
			std::string attach_switch;
			unsigned attach_port;
			std::pair<unsigned,unsigned> ip;
			unsigned mac;
			std::map<unsigned, unsigned> arp_table;
			std::set<unsigned> delegate_ips;
			std::multimap<unsigned, unsigned> send_dsts;
			static const std::string DELIMITER;
		public:
			host_info(const std::string& name, const std::string& attach_switch, unsigned attach_port,
					  const std::pair<unsigned, unsigned>& ip, unsigned mac, const std::map<unsigned, unsigned>& arp_table,
					  const std::set<unsigned> delegate_ips, const std::multimap<unsigned, unsigned>& send_dsts);
			host_info();
			host_info& set_name(const std::string& name);
			host_info& set_attach_switch(const std::string& attach_switch);
			host_info& set_attach_port(unsigned attach_port);
			host_info& set_ip(const std::pair<unsigned, unsigned>& ip);
			std::pair<unsigned, unsigned> get_ip();
			host_info& set_mac(unsigned mac);
			unsigned get_mac();
			host_info& add_arp_entry(unsigned ip, unsigned mac);
			host_info& add_delegate_ips(unsigned ip);
			host_info& add_send_dst(unsigned fake_src, unsigned dst);

			friend std::ostream& operator << (std::ostream& os, const host_info& host_info);
		};
		friend std::ostream& operator << (std::ostream& os, const host_info& host_info);
		
		bool generate_switch_ids();

		bool write_rule_2_rule_conf(const ns_network::rule* rule);

		bool write_topo_2_topo_conf();

		/**
		 * request a new port id when adds links
		 */
		int request_new_port(int sw_id);
		unsigned request_new_ip();
		unsigned request_new_mac();
		std::set< std::pair<unsigned int, unsigned int> > get_prefixes_by_src_and_dst_switch(int src_id, int dst_id);

		bool write_host_conf();
		
		static void nonoverlap_insert(std::set< std::pair<unsigned int, unsigned int> >& values, const std::pair<unsigned int, unsigned int>& val);

		
	public:
		mininet_conf_ec_emitter(const std::string& topo_file, const std::string& host_file, const std::string& rule_file,
								const ns_topo::topology* topo, const ns_topo::rule_graph* rule_graph);

		virtual bool emit(equivalent_class* ec);
		
		~mininet_conf_ec_emitter();
	};
	bool operator < (const mininet_conf_ec_emitter::send_info& left, const mininet_conf_ec_emitter::send_info& right);
	std::ostream& operator << (std::ostream& os, const mininet_conf_ec_emitter::host_info& host_info);
}

#endif
