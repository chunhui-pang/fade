#ifndef __JUNIPER_RULE_PARSER_H
#define __JUNIPER_RULE_PARSER_H

#include <regex>
#include <network/pswitch.h>
#include <parser/rule_parser.h>
#include <parser/route_parser.h>

namespace ns_parser
{
	class juniper_rule_parser : public rule_parser
	{
	private:
		const static std::regex new_route_start;
		const static std::regex route_reusing;
		const static std::regex routing_table;
		const static std::string used_routing_table;

		int line_num;
		ns_network::rule* current;
		ns_network::pswitch* psw;
		route_parser* single_parser;

		void skip_header();
		bool parse_route();
	public:
		juniper_rule_parser(std::istream& is, ns_network::pswitch* sw);
		virtual ~juniper_rule_parser();
		virtual ns_network::rule* get_next();
	};
}
#endif
