#ifndef __BOOST_OPTION_PARSER_H
#define __BOOST_OPTION_PARSER_H

#include <string>
#include <vector>
#include <map>
#include <iostream>
#include <boost/program_options.hpp>
#include <boost/filesystem.hpp>
#include <boost/range/iterator_range.hpp>

#include <parser/option_parser_interface.h>

namespace ns_parser
{
	
	using std::string;
	using std::vector;
	using std::map;
	using std::pair;
	using std::ostream;
	namespace po = boost::program_options;
	namespace fs = boost::filesystem;

	class boost_option_parser : public option_parser_interface
	{
	private:
		static const string INTF_SUFFIX;
		static const string FIB_SUFFIX;

		static const string OPTION_DATA_DIR;
		static const string OPTION_HELP;
		static const string OPTION_SHOW_WARNING;
		static const string OPTION_SHOW_MULTIPLE_NEXT_HOPS;
		static const string OPTION_SHOW_UNKNOWN_NEXT_HOPS;
		static const string OPTION_SHOW_FORWARDING_LOOPS;
		static const string OPTION_SHOW_UNKNOWN_INTFACES;
		static const string OPTION_OUTPUT_SLICE_INFO;
		static const string DEFAULT_OUTPUT_SLICE_INFO;
		static const string OPTION_OUTPUT_MININET_CONFIG_PREFIX;
		static const string DEFAULT_OUTPUT_MININET_CONFIG_PREFIX;
		static const string MININET_TOPO_CONFIG_SUFFIX;
		static const string MININET_HOST_CONFIG_SUFFIX;
		static const string MININET_RULES_CONFIG_SUFFIX;

		int argc;
		char **argv;
		string err_msg;

		po::options_description desc;
		po::variables_map vm;

		string mininet_topo_config_file;
		string mininet_host_config_file;
		string mininet_rules_config_file;

		// real options
		map<string, pair<string,string> > switch_confs;
		bool show_warnings;
    
		void conflicting_options(const char* opt1, const char* opt2);
		void trigger_error(string msg = "");
		void add_options();
		void parse_options();
	public:
		boost_option_parser(int argc, char *argv[]);
		virtual ~boost_option_parser();
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
		virtual const map<string, pair<string, string> >& get_switch_configurations();
	};

}

#endif
