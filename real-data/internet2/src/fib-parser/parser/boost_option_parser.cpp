#include <stdexcept>
#include <iostream>
#include <iomanip>
#include <boost/algorithm/string.hpp>

#include <parser/boost_option_parser.h>

namespace ns_parser
{
	using std::cout;
	using std::endl;
	using std::string;
	using std::exception;
	using std::logic_error;

	const string boost_option_parser::FIB_SUFFIX = "-fib.xml";
	const string boost_option_parser::INTF_SUFFIX = ".intf";

	const string boost_option_parser::OPTION_DATA_DIR = "data-directory,d";
	const string boost_option_parser::OPTION_HELP = "help,h";
	const string boost_option_parser::OPTION_SHOW_WARNING = "show-warnings";
	const string boost_option_parser::OPTION_SHOW_MULTIPLE_NEXT_HOPS = "show-multiple-next-hops";
	const string boost_option_parser::OPTION_SHOW_UNKNOWN_NEXT_HOPS = "show-unknown-next-hops";
	const string boost_option_parser::OPTION_SHOW_FORWARDING_LOOPS = "show-forwarding-loops";
	const string boost_option_parser::OPTION_SHOW_UNKNOWN_INTFACES = "show-unknown-interfaces";
	const string boost_option_parser::OPTION_OUTPUT_SLICE_INFO = "output-slice-info";
	const string boost_option_parser::DEFAULT_OUTPUT_SLICE_INFO = "";
	const string boost_option_parser::OPTION_OUTPUT_MININET_CONFIG_PREFIX = "output-mininet-config-prefix";
	const string boost_option_parser::MININET_TOPO_CONFIG_SUFFIX = ".gml.dat";
	const string boost_option_parser::MININET_HOST_CONFIG_SUFFIX = "_host_conf.json";
	const string boost_option_parser::MININET_RULES_CONFIG_SUFFIX = "_rules.dat";
	const string boost_option_parser::DEFAULT_OUTPUT_MININET_CONFIG_PREFIX = "";


	boost_option_parser::boost_option_parser(int argc, char *argv[]) :
		argc(argc), argv(argv), err_msg(""),
		vm(), desc(),
		switch_confs(), show_warnings(false),
		mininet_topo_config_file(DEFAULT_OUTPUT_MININET_CONFIG_PREFIX),
		mininet_host_config_file(DEFAULT_OUTPUT_MININET_CONFIG_PREFIX),
		mininet_rules_config_file(DEFAULT_OUTPUT_MININET_CONFIG_PREFIX)
	{
		this->add_options();
		this->parse_options();
	}

	boost_option_parser::~boost_option_parser()
	{
    
	}

	void boost_option_parser::add_options()
	{
		po::options_description generic("Allowed Options");
		generic.add_options()
			(boost_option_parser::OPTION_HELP.c_str(), "produce help message")
			(boost_option_parser::OPTION_SHOW_WARNING.c_str(), po::value<bool>()->default_value(false), "show warnings or not (off/on)")
			(boost_option_parser::OPTION_SHOW_UNKNOWN_NEXT_HOPS.c_str(), po::value<bool>()->default_value(false), "show unkown next hop ip as we parsing FIB (off/on)")
			(boost_option_parser::OPTION_SHOW_UNKNOWN_INTFACES.c_str(), po::value<bool>()->default_value(false), "show unkown interfaces as we parsing interface files")
			(boost_option_parser::OPTION_SHOW_MULTIPLE_NEXT_HOPS.c_str(), po::value<bool>()->default_value(false), "show rules with multiple next hops (off/on)")
			(boost_option_parser::OPTION_SHOW_FORWARDING_LOOPS.c_str(), po::value<bool>()->default_value(false), "show forwarding loops as we building rule graph (off/on)")
			(boost_option_parser::OPTION_OUTPUT_SLICE_INFO.c_str(), po::value<std::string>()->default_value(DEFAULT_OUTPUT_SLICE_INFO), "output the slice info into a specific file")
			(boost_option_parser::OPTION_OUTPUT_MININET_CONFIG_PREFIX.c_str(), po::value<std::string>()->default_value(DEFAULT_OUTPUT_MININET_CONFIG_PREFIX), "output the topology and host config into file ${prefix}.gml.dat, ${prefix}_host_conf.json respectively.");
		
		po::options_description hidden("Hidden Options");
		hidden.add_options()
			(boost_option_parser::OPTION_DATA_DIR.c_str(), po::value<string>(), "the data direcotyr which contains fib files and interface files");

	
		this->desc.add(generic).add(hidden);
    
		po::positional_options_description p;
		p.add(boost_option_parser::OPTION_DATA_DIR.substr(0, OPTION_DATA_DIR.length()-2).c_str(), -1);

		po::store(po::command_line_parser(this->argc, this->argv).options(this->desc).positional(p).run(), this->vm);
		po::notify(this->vm);
	}

	void boost_option_parser::parse_options()
	{
		try{
			if(vm.count(OPTION_HELP.substr(0, OPTION_HELP.length()-2))){
				return;
			}
			if(vm.count(OPTION_DATA_DIR.substr(0, OPTION_DATA_DIR.length()-2)) != 1){
				this->trigger_error("data directory MUST be provided only once.");
				return;
			}

			const string& data_directory = vm[OPTION_DATA_DIR.substr(0, OPTION_DATA_DIR.length()-2)].as<string>();
			vector<string> fib_files;
			vector<string> intf_files;
			// iterate files in directory
			fs::path pt(data_directory);
			if(fs::is_directory(pt)){
				for(auto& entry : boost::make_iterator_range(fs::directory_iterator(pt), {})){
					string file_path = entry.path().string();
					if(file_path.rfind(FIB_SUFFIX) == file_path.size()-FIB_SUFFIX.size()){
						fib_files.push_back(file_path);
					}else if(file_path.rfind(INTF_SUFFIX) == file_path.size()-INTF_SUFFIX.size()){
						intf_files.push_back(file_path);
					}
				}
			}else{
				this->trigger_error("'" + data_directory + "' is not a valid directory.");
				return;
			}
			// switch name   ---- <interfaces, fib> 
			map<string, pair<string, string> > switch_conf;
			for(string intf : intf_files){
				int start_pos = (intf.find_last_of('/') == string::npos) ? 0 : (intf.find_last_of('/')+1);
				int end_pos = intf.size() - INTF_SUFFIX.size();
				string sw_name = intf.substr(start_pos, end_pos-start_pos);
				for(string fib : fib_files){
					start_pos = (fib.find_last_of('/') == string::npos) ? 0 : (fib.find_last_of('/')+1);
					end_pos = fib.size() - FIB_SUFFIX.size();
					if(boost::iequals(fib.substr(start_pos, end_pos-start_pos), sw_name)){
						this->switch_confs.insert(make_pair(sw_name, make_pair(intf, fib)));
						break;
					}
				}
			}
			if(this->switch_confs.size() == 0){
				this->trigger_error("'"+ data_directory + "' contains none complete switch configure file");
				return;
			}
			this->show_warnings = vm[OPTION_SHOW_WARNING].as<bool>();
		}catch(exception& e){
			this->trigger_error(e.what());
			return;
		}
	}

	bool boost_option_parser::print_error_or_help_msg(std::ostream& os)
	{
		if(this->err_msg.size() != 0){
			os << err_msg << endl;
			return true;
		} else if(this->vm.count(boost_option_parser::OPTION_HELP.substr(0, OPTION_HELP.length()-2)) != 0){
			os << desc << endl;
			return true;
		} else {
			return false;
		}
	}

	void boost_option_parser::print_configurations(std::ostream& os)
	{
		os << "Configurations:" << endl;
		os << "    show warnings: " << this->show_warnings << endl;
		os << "    switch and configure files: " << endl;
		unsigned int max_width[3] = {0,0,0};
		for(auto it = this->switch_confs.begin(); it != this->switch_confs.end(); it++){
			max_width[0] = max_width[0] > it->first.size() ? max_width[0] : it->first.size();
			max_width[1] = max_width[1] > it->second.first.size() ? max_width[1] : it->second.first.size();
			max_width[1] = max_width[1] > it->second.second.size() ? max_width[1] : it->second.second.size();
		}

		for(auto it = this->switch_confs.begin(); it != this->switch_confs.end(); it++){
			os << "        " << std::left << std::setw(max_width[0]+1) <<  (it->first+":") << "  "
			   << std::setw(max_width[1]) << it->second.first << "    "
			   << std::setw(max_width[2]) << it->second.second << endl;
		}
	}

	bool boost_option_parser::get_show_warnings()
	{
		return this->show_warnings;
	}

	bool boost_option_parser::get_show_unknown_next_hops()
	{
		if(this->vm.count(boost_option_parser::OPTION_SHOW_UNKNOWN_NEXT_HOPS) != 0){
			return this->show_warnings || this->vm[boost_option_parser::OPTION_SHOW_UNKNOWN_NEXT_HOPS].as<bool>();
		} else {
			return this->show_warnings;
		}
	}

	bool boost_option_parser::get_show_multiple_next_hops()
	{
		if(this->vm.count(boost_option_parser::OPTION_SHOW_MULTIPLE_NEXT_HOPS) != 0){
			return this->show_warnings || this->vm[boost_option_parser::OPTION_SHOW_MULTIPLE_NEXT_HOPS].as<bool>();
		} else {
			return this->show_warnings;
		}
	}

	bool boost_option_parser::get_show_unknown_interfaces()
	{
		if(this->vm.count(boost_option_parser::OPTION_SHOW_UNKNOWN_INTFACES) != 0){
			return this->show_warnings || this->vm[boost_option_parser::OPTION_SHOW_UNKNOWN_INTFACES].as<bool>();
		} else {
			return this->show_warnings;
		}
	}

	bool boost_option_parser::get_show_forwarding_loops()
	{
		if(this->vm.count(boost_option_parser::OPTION_SHOW_FORWARDING_LOOPS) != 0){
			return this->show_warnings || this->vm[boost_option_parser::OPTION_SHOW_FORWARDING_LOOPS].as<bool>();
		} else {
			return this->show_warnings;
		}
	}

	const std::string& boost_option_parser::get_output_slice_info()
	{
		if(this->vm.count(boost_option_parser::OPTION_OUTPUT_SLICE_INFO) != 0)
		{
			return this->vm[boost_option_parser::OPTION_OUTPUT_SLICE_INFO].as<std::string>();
		}
		else
		{
			return DEFAULT_OUTPUT_SLICE_INFO;
		}
	}

	const std::string& boost_option_parser::get_output_mininet_topo_config()
	{
		if(this->vm[boost_option_parser::OPTION_OUTPUT_MININET_CONFIG_PREFIX].as<std::string>().size() != 0)
		{
			this->mininet_topo_config_file = this->vm[boost_option_parser::OPTION_OUTPUT_MININET_CONFIG_PREFIX].as<std::string>() + MININET_TOPO_CONFIG_SUFFIX;
		}
		return this->mininet_topo_config_file;
	}

	const std::string& boost_option_parser::get_output_minient_host_config()
	{
		if(this->vm[boost_option_parser::OPTION_OUTPUT_MININET_CONFIG_PREFIX].as<std::string>().size() != 0)
		{
			this->mininet_host_config_file = this->vm[boost_option_parser::OPTION_OUTPUT_MININET_CONFIG_PREFIX].as<std::string>() + MININET_HOST_CONFIG_SUFFIX;
		}
		return this->mininet_host_config_file;
	}

	const std::string& boost_option_parser::get_output_minient_rules_config()
	{
		if(this->vm[boost_option_parser::OPTION_OUTPUT_MININET_CONFIG_PREFIX].as<std::string>().size() != 0)
		{
			this->mininet_rules_config_file = this->vm[boost_option_parser::OPTION_OUTPUT_MININET_CONFIG_PREFIX].as<std::string>() + MININET_RULES_CONFIG_SUFFIX;
		}
		return this->mininet_rules_config_file;
	}

	const map<string, pair<string, string> >& boost_option_parser::get_switch_configurations() 
	{
		return this->switch_confs;
	}


	void boost_option_parser::conflicting_options(const char* opt1, const char* opt2)
	{
		if ( (this->vm.count(opt1) && !this->vm[opt1].defaulted() ) && (this->vm.count(opt2) && !this->vm[opt2].defaulted()) )
			throw logic_error(string("Conflicting options '")  + opt1 + "' and '" + opt2 + "'.");
	}
	
	void boost_option_parser::trigger_error(string msg)
	{
		this->err_msg = msg;
	}
}
