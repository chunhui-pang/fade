#include <sstream>
#include <cstdlib>
#include <stdexcept>

#include <parser/route_parser.h>
#include <common/util.h>

namespace ns_parser {
	using ns_common::str2ip;
	const std::regex route_parser::regexs[8] = {
		std::regex("([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})(.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})?/([0-9]{1,2})"),
		std::regex("clon|dest|iddn|ifcl|ifdn|ignr|intf|perm|user"),
		std::regex("[0-9]+"),
		std::regex("([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})|([0-9a-fA-F]{1,2}:[0-9a-fA-F]{1,2}:[0-9a-fA-F]{1,2}:[0-9a-fA-F]{1,2}:[0-9a-fA-F]{1,2}:[0-9a-fA-F]{1,2})"),
		std::regex("bcst|deny|dscd|hold|idxd|indr|locl|mcrt|mdsc|mgrp|recv|rjct|rslv|ucst|ulst"),
		std::regex("[0-9]+"),
		std::regex("[0-9]+"),
		std::regex(".+")
	};

	route_parser::route_parser() : parsed(8, false)
	{
		
	}

	route_parser::route_parser(const route_parser& route)
	{
		this->parsed = route.parsed;
		this->destination = route.destination;
		this->route_type = route.route_type;
		this->route_ref = route.route_ref;
		this->next_hop_ip = route.next_hop_ip;
		this->next_hop_type = route.next_hop_type;
		this->index = route.index;
		this->next_hop_ref = route.next_hop_ref;
		this->next_hop_intf = route.next_hop_intf;
	}

	route_parser::~route_parser()
	{
    
	}

	bool route_parser::parse_str(const std::string& str)
	{
		// TODO: add implementation
		std::istringstream iss(str);
		std::string option_str;
		// determine how to parse current line according to the 1st option
		iss >> option_str;

		std::smatch match;
		if(get_destination(option_str)){
			// possible line content are:
			// destination;      route type;    route Reference;  next hop(*);   next hop type;  index;  next hop ref;  next hop interface(*)
			iss >> option_str;
			if(!get_route_type(option_str))
				return false;
	
			iss >> option_str;
			if(!get_route_ref(option_str))
				return false;
	
			iss >> option_str;
			if(get_next_hop_ip(option_str))
				iss >> option_str;
			if(!get_next_hop_type(option_str))
				return false;

			iss >> option_str;
			if(!get_index(option_str))
				return false;

			iss >> option_str;
			if(!get_next_hop_ref(option_str))
				return false;

			if(!iss.eof()){
				iss >> option_str;
				return get_next_hop_intf(option_str); // this function is optional
			}else{
				return true;
			}
		}else if(get_next_hop_ip(option_str)){
			// possible line content:
			// next hop; next hop type;   index;  next hop ref; next hop interface
			iss >> option_str;
			if(!get_next_hop_type(option_str))
				return false;

			iss >> option_str;
			if(!get_index(option_str))
				return false;

			iss >> option_str;
			if(!get_next_hop_ref(option_str))
				return false;

			iss >> option_str;
			return get_next_hop_intf(option_str);
		}else{
			return false;
		}
		return true;
	}

	void route_parser::reset()
	{
		this->parsed = std::vector<bool>(8, false);
	}

	const std::pair<unsigned int, unsigned int> route_parser::get_destination() const
	{
		if(this->parsed[DESTINATION])
			return this->destination;
		throw std::logic_error("'destination' haven't been parsed yet!");
	}

	const std::string& route_parser::get_route_type() const
	{
		if(this->parsed[ROUTE_TYPE])
			return this->route_type;
		throw std::logic_error("'route type' haven't been parsed yet!");
	}

	int route_parser::get_route_ref() const
	{
		if(this->parsed[ROUTE_REFERENCE])
			return this->route_ref;
		throw std::logic_error("'route reference' haven't been parsed yet!");
	}

	unsigned int route_parser::get_next_hop_ip() const
	{
		if(this->parsed[NEXT_HOP])
			return this->next_hop_ip;
		throw std::logic_error("'next hop ip' haven't been parsed yet!");
	}

	const std::string& route_parser::get_next_hop_type() const
	{
		if(this->parsed[NEXT_HOP_TYPE])
			return this->next_hop_type;
		throw std::logic_error("'next hop type' haven't been parsed yet!");
	}

	int route_parser::get_index() const
	{
		if(this->parsed[INDEX])
			return this->index;
		throw std::logic_error("'index' haven't been parsed yet!");
	}

	int route_parser::get_next_hop_ref() const
	{
		if(this->parsed[REFERENCE])
			return this->next_hop_ref;
		throw std::logic_error("'next hop reference' haven't been parsed yet!");
	}

	const std::string& route_parser::get_next_hop_intf() const
	{
		if(this->parsed[NEXT_HOP_INTERFACE])
			return this->next_hop_intf;
		throw std::logic_error("'next hop interface' haven't been parsed yet!");
	}

	bool route_parser::is_parse_finished() const
	{
		try{
			if("dest" == this->get_route_type() || "intf" == this->get_route_type())
				return (this->parsed[DESTINATION]) && ( (this->parsed[NEXT_HOP]) || (this->parsed[NEXT_HOP_INTERFACE]) );
			if("dscd" == this->get_next_hop_type() || "deny" == this->get_next_hop_type() || "rjct" == this->get_next_hop_type())
				return (this->parsed[DESTINATION]);
			if("locl" == this->get_next_hop_type())
				return (this->parsed[DESTINATION]) && (this->parsed[NEXT_HOP]);
			return (this->parsed[route_parser::DESTINATION]) && (this->parsed[route_parser::NEXT_HOP_INTERFACE]) && (this->parsed[route_parser::NEXT_HOP]);
		}catch(std::logic_error& ex){
			return false;
		}
	}

	bool route_parser::is_destination_parsed() const
	{
		return this->parsed[DESTINATION];
	}


	bool route_parser::get_destination(const std::string& opt_str)
	{
		std::smatch match;
		if(std::regex_match(opt_str, match, route_parser::regexs[route_parser::DESTINATION])){
			this->destination.first = str2ip(match[1].str().c_str());
			this->destination.second = atoi(match[3].str().c_str());
			// provides compatibility with destinations like 64.57.28.241.146.57.253.37.47/72, but do not recognize these entries
			this->parsed[DESTINATION] = ("" == match[2].str());
			return true;
		}else{
			return false;
		}
	}

	bool route_parser::get_route_type(const std::string& opt_str)
	{
		if(std::regex_match(opt_str, route_parser::regexs[route_parser::ROUTE_TYPE])){
			this->route_type = opt_str;
			this->parsed[ROUTE_TYPE] = true;
			return true;
		}
		return false;
	}

	bool route_parser::get_route_ref(const std::string& opt_str)
	{
		if(std::regex_match(opt_str, route_parser::regexs[route_parser::ROUTE_REFERENCE])){
			this->route_ref = atoi(opt_str.c_str());
			this->parsed[ROUTE_REFERENCE] = true;
			return true;
		}
		return false;
	}

	bool route_parser::get_next_hop_ip(const std::string& opt_str)
	{
		if(std::regex_match(opt_str, route_parser::regexs[route_parser::NEXT_HOP])){
			if(opt_str.find(':') != std::string::npos){
				// this field is filled with MAC address
			}else{
				this->next_hop_ip = str2ip(opt_str.c_str());
				this->parsed[NEXT_HOP] = true;
			}
			return true;
		}
		return false;
	}

	bool route_parser::get_next_hop_type(const std::string& opt_str)
	{
		if(std::regex_match(opt_str, route_parser::regexs[route_parser::NEXT_HOP_TYPE])){
			this->next_hop_type = opt_str;
			this->parsed[NEXT_HOP_TYPE] = true;
			return true;
		}
		return false;
	}

	bool route_parser::get_index(const std::string& opt_str)
	{
		if(std::regex_match(opt_str, route_parser::regexs[route_parser::INDEX])){
			this->index = atoi(opt_str.c_str());
			this->parsed[INDEX] = true;
			return true;
		}
		return false;
	}

	bool route_parser::get_next_hop_ref(const std::string& opt_str)
	{
		if(std::regex_match(opt_str, route_parser::regexs[route_parser::REFERENCE])){
			this->next_hop_ref = atoi(opt_str.c_str());
			this->parsed[REFERENCE] = true;
			return true;
		}
		return false;
	}

	bool route_parser::get_next_hop_intf(const std::string& opt_str)
	{
		if(std::regex_match(opt_str, route_parser::regexs[route_parser::NEXT_HOP_INTERFACE])){
			this->next_hop_intf = opt_str;
			this->parsed[NEXT_HOP_INTERFACE] = true;
			return true;
		}
		return false;
	}

}
