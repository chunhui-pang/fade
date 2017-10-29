#include <string>
#include <iostream>
#include <stdexcept>
#include <network/interface.h>
#include <parser/option_parser.h>
#include <parser/juniper_rule_parser.h>

namespace ns_parser
{
	using std::istream;
	using std::string;
	using std::cout;
	using std::endl;
	using std::regex;
	using std::smatch;
	using std::out_of_range;
	using std::logic_error;
	using ns_network::rule;
	using ns_network::pswitch;
	using ns_network::interface;

	const regex juniper_rule_parser::new_route_start = regex("\\W*([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}(\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})?)/([0-9]{1,2})\\W+.*");
	const regex juniper_rule_parser::route_reusing = regex("\\W*"
														   "("
														   "([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})|"    // next hop ip
														   "([0-9a-fA-F]{1,2}:[0-9a-fA-F]{1,2}:[0-9a-fA-F]{1,2}:[0-9a-fA-F]{1,2}:[0-9a-fA-F]{1,2}:[0-9a-fA-F]{1,2})|"  // next hop mac
														   "bcst|deny|dscd|hold|idxd|indr|locl|mcrt|mdsc|mgrp|recv|rjct|rslv|ucst|ulst|"    // next hop type
														   "clon|dest|iddn|ifcl|ifdn|ignr|intf|perm|user|"    // route type
														   "comp"    // others
														   ")\\W+.*");
	const regex juniper_rule_parser::routing_table = regex("Routing\\Wtable:\\W(\\w*).*");
	const string juniper_rule_parser::used_routing_table = "default.inet";
	
	juniper_rule_parser::juniper_rule_parser(istream& is, pswitch* psw) : rule_parser(is), psw(psw), line_num(0)
	{
		this->single_parser = new route_parser();
		this->skip_header();
	}

	juniper_rule_parser::~juniper_rule_parser()
	{
		if(this->single_parser != nullptr){
			delete this->single_parser;
		}
	}

	void juniper_rule_parser::skip_header()
	{
		const int header_len = 6;
		string line;
		while(this->line_num < header_len){
			getline(is, line);
			this->line_num++;
		}
	}

	rule* juniper_rule_parser::get_next()
	{
		string line;
		smatch match;
		while(!getline(is, line).eof()){
			line_num++;
			if(regex_match(line, new_route_start)){ // destination
				this->single_parser->reset();
				this->single_parser->parse_str(line);
			}else if(regex_match(line, route_reusing)){
				this->single_parser->parse_str(line);
			}else if(regex_match(line, match, routing_table)){
				if(match[1].str() != used_routing_table){
					// parse finished
					return nullptr;
				}
			} else {  // match failed!, stop parsing
				if(option_parser::get_instance()->get_show_warnings()){
					cout << this->psw->get_name() << "(" << line_num << "): parse lines '" << line << "' fail!" << endl;
				}
			}
			if(this->single_parser->is_parse_finished()){
				if("user" == this->single_parser->get_route_type() && "locl" != this->single_parser->get_next_hop_type()){
					if(this->parse_route()){
						return this->current;
					}
				}
			}
		}
		return nullptr;
	}

	bool juniper_rule_parser::parse_route()
	{
		unsigned int prefix, mask, next_hop_ip;
		int next_hop;
		rule* r = nullptr;
		try{
			prefix = this->single_parser->get_destination().first;
			mask = (0xFFFFFFFF << (32-this->single_parser->get_destination().second));
			if("dest" == this->single_parser->get_route_type() || "intf" == this->single_parser->get_route_type()){
				try{
					const interface* intf = this->psw->get_interface_by_name(this->single_parser->get_next_hop_intf());
					next_hop = intf->get_id();
					next_hop_ip = intf->get_addrs().at(0).local_addr;
				}catch(const out_of_range& ex){
					throw ex;
				}catch(const logic_error &ex){ // only interface name is assigned
					const interface* intf = this->psw->get_interface_by_ip(this->single_parser->get_next_hop_ip());
					next_hop = intf->get_id();
					next_hop_ip = this->single_parser->get_next_hop_ip();
				}
			}else if("dscd" == this->single_parser->get_next_hop_type() || "deny" == this->single_parser->get_next_hop_type() || "rjct" == this->single_parser->get_next_hop_type()){ // only
				next_hop = rule::DISCARD;
			}else if("locl" == this->single_parser->get_next_hop_type()){
				const interface* intf = this->psw->get_interface_by_ip(this->single_parser->get_next_hop_ip());
				next_hop = intf->get_id();
				next_hop_ip = this->single_parser->get_next_hop_ip();
			}else{
				const interface* intf = this->psw->get_interface_by_name(this->single_parser->get_next_hop_intf());
				next_hop = intf->get_id();
				next_hop_ip = this->single_parser->get_next_hop_ip();
			}
		}catch(const out_of_range &ex){
			try{
				if(option_parser::get_instance()->get_show_warnings())
					cout <<  "Cannot find interface '" << this->single_parser->get_next_hop_intf() << "' on switch '" << this->psw->get_name() << "'..." << endl;
			}catch(logic_error& ex){
				if(option_parser::get_instance()->get_show_warnings()){
					unsigned int ip = this->single_parser->get_next_hop_ip();
					cout << "Cannot find interface '" << ((ip >> 24)&0xFF) << "." << ((ip>>16)&0xFF) << "." << ((ip>>8)&0xFF) << "." << (ip & 0xFF)
						 << "' on switch '" << this->psw->get_name() << "'..." << endl;
				}
			}
			return false;
		}
		this->current = new rule(this->psw, prefix, mask, next_hop, next_hop_ip);
		return true;
	}

}
