#include <parsers/localization_parser/localization_parser.h>
#include <cstdlib>

const std::string localization_parser::PARSER_NAME = "localization parser";
const std::regex localization_parser::LOCALIZATION_REGEX(".*start\\Wto\\Wdo\\Wlocalization\\Wfor\\Wflow.*flowId=([0-9]+).*");

localization_parser::localization_parser() : result(NULL)
{
	
}

const std::string& localization_parser::get_name() const
{
	return PARSER_NAME;
}

parser::parse_result localization_parser::parse_line(const std::string &str, int n)
{
	std::smatch sm;
	if (std::regex_match(str, sm, LOCALIZATION_REGEX))
	{
		int flow_id = std::atoi(sm[1].str().c_str());
		this->result = new localization(flow_id);
		return parse_result::SUCCESS_AND_STOP;
	}
	return parse_result::SKIP;
}

const entity* localization_parser::pop_parse_result()
{
	const entity* tmp = result;
	this->result = NULL;
	return tmp;
}

localization_parser::~localization_parser()
{
	if (NULL != this->result)
	{
		delete this->result;
	}
}

extern "C"
{
	parser* create_parser()
	{
		return new localization_parser();
	}
}
