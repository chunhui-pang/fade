#ifndef __OPTION_PARSER_H
#define __OPTION_PARSER_H

#include <parser/option_parser_interface.h>

namespace ns_parser
{
	/**
	 * A singleton decorator of class option_parser_interface
	 */
	class option_parser : public option_parser_interface
	{
	private:
		static option_parser* inst;
		option_parser_interface* target;
		option_parser(option_parser_interface* target);
	public:
		virtual ~option_parser();
		/**
		 * call this function with an option_parser_interface* at the first time
		 */
		static option_parser* get_instance(option_parser_interface* target = nullptr);
		virtual bool print_error_or_help_msg(std::ostream& os);
		virtual void print_configurations(std::ostream& os);

		virtual bool get_show_warnings();
		virtual bool get_show_unknown_next_hops();
		virtual bool get_show_multiple_next_hops();
		virtual bool get_show_forwarding_loops();
		virtual bool get_show_unknown_interfaces();
		virtual const std::string& get_output_slice_info();
		virtual const std::string& get_output_mininet_topo_config();
		virtual const std::string& get_output_minient_host_config();
		virtual const std::string& get_output_minient_rules_config();
		virtual const std::map<std::string, std::pair<std::string, std::string> >& get_switch_configurations();
	};
}

#endif
