#ifndef __OPTION_PARSER_H
#define __OPTION_PARSER_H

#include <vector>
#include <string>

#include <boost/program_options.hpp>

namespace po = boost::program_options;

/**
 * ILP builder program options (parameters)
 */
class option_parser
{
public:
	option_parser(int argc, char* argv[]);
	
	virtual const std::vector<int>& get_max_tcams() const;
	virtual int get_max_run() const;
	virtual const std::string& get_input_file() const;
	virtual const std::string& get_model_output_file() const;
	virtual const std::string& get_solver_output_file() const;
	virtual std::string parse_options();
private:
	int argc;
	char** argv;
	
	std::vector<int> max_tcams;
	int max_run;
	std::string input_file;
	std::string model_output_file;
	std::string solver_output_file;

	po::variables_map vm;
	po::options_description opt_desc;

	void add_options();
	std::string validate_input_file();
};
#endif
