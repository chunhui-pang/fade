#include <parsers/anomaly_parser/anomaly_parser.h>
#include <cstdlib>

const std::string anomaly_parser::PARSER_NAME = "anomaly parser";
const std::regex anomaly_parser::ANOMALY_REGEX(".*find\\Wanomaly\\Wof\\Wflow\\W([0-9]+).*\\Win\\Wrule.*datapathId=.*:([0-9a-f]{2}),.*ipv4_dst=([0-9.]+).*");

const std::string& anomaly_parser::get_name() const
{
	return PARSER_NAME;
}

anomaly_parser::anomaly_parser() : result(NULL)
{
	
}

parser::parse_result anomaly_parser::parse_line(const std::string &line, int counter)
{
	std::smatch sm;
	if (std::regex_match(line, sm, ANOMALY_REGEX))
	{
		int flow_id = std::atoi(sm[1].str().c_str());
		int dpid = std::strtol(sm[2].str().c_str(), NULL, 10);
		std::string dst = sm[3].str();
		this->result = new anomaly(dpid, flow_id, dst);
		return parse_result::SUCCESS_AND_STOP;
	}
	return parse_result::SKIP;
}

const entity* anomaly_parser::pop_parse_result()
{
	const entity* tmp = this->result;
	this->result = NULL;
	return tmp;
}

anomaly_parser::~anomaly_parser()
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
		return new anomaly_parser();
	}
}

