#ifndef __OPTION_PARSER_INTERFACE_H
#define __OPTION_PARSER_INTERFACE_H

#include <iostream>
#include <map>
#include <utility>

namespace ns_parser
{
	class option_parser_interface
	{
	public:
		virtual ~option_parser_interface();
		virtual bool print_error_or_help_msg(std::ostream& os) = 0;
		virtual void print_configurations(std::ostream& os) = 0;

		virtual bool get_show_warnings() = 0;
		virtual bool get_show_unknown_next_hops() = 0;
		virtual bool get_show_multiple_next_hops() = 0;
		virtual bool get_show_forwarding_loops() = 0;
		virtual bool get_show_unknown_interfaces() = 0;
		virtual const std::string& get_output_slice_info() = 0;
		virtual const std::string& get_output_mininet_topo_config() = 0;
		virtual const std::string& get_output_minient_host_config() = 0;
		virtual const std::string& get_output_minient_rules_config() = 0;
		virtual const std::map<std::string, std::pair<std::string, std::string> >& get_switch_configurations() = 0;
	};
}
#endif
