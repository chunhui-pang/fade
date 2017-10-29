#ifndef __ROUTE_PARSER_H
#define __ROUTE_PARSER_H

#include <utility>
#include <regex>
#include <vector>

namespace ns_parser {
	/**
	 * Utility for rule_parser: parse single line once a time
	 */
	class route_parser
	{
	public:
		route_parser();
		route_parser(const route_parser& route);
		~route_parser();

		bool parse_str(const std::string& str);
		void reset();
		const std::pair<unsigned int, unsigned int> get_destination() const;
		const std::string& get_route_type() const;
		int get_route_ref() const;
		unsigned int get_next_hop_ip() const;
		const std::string& get_next_hop_type() const;
		int get_index() const;
		int get_next_hop_ref() const;
		const std::string& get_next_hop_intf() const;
		bool is_parse_finished() const;
		bool is_destination_parsed() const;
    
	private:
		enum route_option {DESTINATION=0, ROUTE_TYPE=1, ROUTE_REFERENCE=2, NEXT_HOP=3, NEXT_HOP_TYPE=4, INDEX=5, REFERENCE=6, NEXT_HOP_INTERFACE=7};
		const static std::regex regexs[8];
    
		std::vector<bool> parsed; /* indicating whether a property is parsed, the indices are equal to enum value */
		std::pair<unsigned int, unsigned int> destination;
		std::string route_type;
		int route_ref;
		unsigned int next_hop_ip;
		std::string next_hop_type;
		int index;
		int next_hop_ref;
		std::string next_hop_intf;

		bool get_destination(const std::string& opt_str);
		bool get_route_type(const std::string& opt_str);
		bool get_route_ref(const std::string& opt_str);
		bool get_next_hop_ip(const std::string& opt_str);
		bool get_next_hop_type(const std::string& opt_str);
		bool get_index(const std::string& opt_str);
		bool get_next_hop_ref(const std::string& opt_str);
		bool get_next_hop_intf(const std::string& opt_str);
	};
}
#endif

 
