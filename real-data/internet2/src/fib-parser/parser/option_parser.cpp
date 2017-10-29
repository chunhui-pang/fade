#include <parser/option_parser.h>

namespace ns_parser
{
	using std::map;
	using std::pair;
	using std::string;
	
	option_parser* option_parser::inst = nullptr;
	option_parser::option_parser(option_parser_interface* target) : target(target)
	{
		
	}

	option_parser::~option_parser()
	{
		if(this->target != nullptr){
			delete this->target;
		}
	}

	option_parser* option_parser::get_instance(option_parser_interface* target)
	{
		if(option_parser::inst == nullptr){
			if(target == nullptr){
				return nullptr;
			} else {
				option_parser::inst = new option_parser(target);
			}
		}
		return option_parser::inst;
	}

	bool option_parser::print_error_or_help_msg(std::ostream &os)
	{
		// target is assumed to be non-null-pointer, see the constructor
		return this->target->print_error_or_help_msg(os);
	}

	void option_parser::print_configurations(std::ostream &os)
	{
		this->target->print_configurations(os);
	}

	bool option_parser::get_show_warnings()
	{
		return this->target->get_show_warnings();
	}

	bool option_parser::get_show_unknown_next_hops()
	{
		return this->target->get_show_unknown_next_hops();
	}

	bool option_parser::get_show_multiple_next_hops()
	{
		return this->target->get_show_multiple_next_hops();
	}

	bool option_parser::get_show_forwarding_loops()
	{
		return this->target->get_show_forwarding_loops();
	}

	bool option_parser::get_show_unknown_interfaces()
	{
		return this->target->get_show_unknown_interfaces();
	}

	const std::string& option_parser::get_output_slice_info()
	{
		return this->target->get_output_slice_info();
	}

	const std::string& option_parser::get_output_mininet_topo_config()
	{
		return this->target->get_output_mininet_topo_config();
	}

	const std::string& option_parser::get_output_minient_host_config()
	{
		return this->target->get_output_minient_host_config();
	}

	const std::string& option_parser::get_output_minient_rules_config()
	{
		return this->target->get_output_minient_rules_config();
	}

	const map<string, pair<string, string> >& option_parser::get_switch_configurations()
	{
		return this->target->get_switch_configurations();
	}
}
