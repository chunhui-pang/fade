#include <parser/option_parser.h>
#include <stdexcept>
#include <sstream>
#include <fstream>

option_parser::option_parser(int argc, char* argv[]) : argc(argc), argv(argv), max_tcams(), max_run(0), input_file(""), model_output_file(""), solver_output_file(""), vm(), opt_desc()
{
	this->add_options();
}

const std::vector<int>& option_parser::get_max_tcams() const
{
	return this->max_tcams;
}

int option_parser::get_max_run() const
{
	return this->max_run;
}

const std::string& option_parser::get_input_file() const
{
	return this->input_file;
}

const std::string& option_parser::get_model_output_file() const
{
	return this->model_output_file;
}

const std::string& option_parser::get_solver_output_file() const
{
	return this->solver_output_file;
}

void option_parser::add_options()
{
	po::options_description generic("Allowed Options");
	generic.add_options()
		("help,h", "produce help message")
		("max-tcam,t", po::value<std::string>()->default_value("300"), "maximum tcam for every switch, order by switch name. If only one maximum tcam is given, it will be repeated")
		("max-run,r", po::value<int>()->default_value(5), "maximum run")
		("model-output-file,m", po::value<std::string>()->default_value(""), "output generated model to a given file")
		("solver-output-file,s", po::value<std::string>()->default_value(""), "output solution to a given file");
	
	po::options_description hidden("Hidden Options");
	hidden.add_options()
		("flow-file,f", po::value<std::string>(), "the input file describes aggregated flows");
	this->opt_desc.add(generic).add(hidden);

	po::positional_options_description p;
	p.add("flow-file", 1);
	
	po::store(po::command_line_parser(this->argc, this->argv).options(this->opt_desc).positional(p).run(), this->vm);
	po::notify(this->vm);
}

std::string option_parser::parse_options()
{
	try{
		if(this->vm.count("help") != 0){
			std::ostringstream oss;
			oss << this->opt_desc;
			return oss.str();
		}
		if(this->vm.count("flow-file") == 0){
			return "flow file must be specified";
		}
		this->max_run = this->vm["max-run"].as<int>();
		if(this->max_run <= 0){
			return "Only accept positive maximum run";
		}
		this->input_file = this->vm["flow-file"].as<std::string>();
		this->model_output_file = this->vm["model-output-file"].as<std::string>();
		this->solver_output_file = this->vm["solver-output-file"].as<std::string>();
		std::string tcams = this->vm["max-tcam"].as<std::string>();
		std::istringstream iss(tcams);
		int tmp;
		while(!iss.eof()){
			iss >> tmp;
			if(tmp <= 0){
				return "Only accept positive maximum tcam";
			}
			this->max_tcams.push_back(tmp);
		}
		std::string validate_msg = this->validate_input_file();
		if(validate_msg.length() != 0){
			return std::move(validate_msg);
		}
	} catch (std::exception& e) {
		return e.what();
	}
	return "";
}

std::string option_parser::validate_input_file()
{
	std::ifstream test(this->input_file);
	if(!test){
		return "invalid input flow file";
	} else {
		return "";
	}
}
