#include <parsers/rule_link_parser/rule_link_parser.h>
#include <cstdlib>
static const std::string FLOW_RULE_NODE_REGEX = "FlowRuleNode\\{datapathId=.*?:([0-9]{2}),.*ipv4_dst=([0-9.]+).*dependsOnSize=0\\}";
const std::string rule_link_parser::PARSER_NAME = "rule_link parser";
const std::regex rule_link_parser::RULE_LINK_REGEX(".*RuleGraphService.*the\\Wlink\\W" + FLOW_RULE_NODE_REGEX + "\\W--\\W" + FLOW_RULE_NODE_REGEX + ".*");
rule_link_parser::rule_link_parser() : result(NULL)
{
	
}

const std::string& rule_link_parser::get_name() const
{
	return PARSER_NAME;
}

parser::parse_result rule_link_parser::parse_line(const std::string &line, int line_counter)
{
	std::smatch sm;
	if (std::regex_match(line, sm, RULE_LINK_REGEX))
	{
		std::string dst = sm[2].str();
		int src_sw = std::strtol(sm[1].str().c_str(), NULL, 10);
		int dst_sw = std::strtol(sm[3].str().c_str(), NULL, 10);
		this->result = new rule_link(dst, src_sw, dst_sw);
		return parser::SUCCESS_AND_STOP;
	}
	return parser::SKIP;
}


const entity* rule_link_parser::pop_parse_result()
{
	entity* tmp = this->result;
	this->result = NULL;
	return tmp;
}

rule_link_parser::~rule_link_parser()
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
		return new rule_link_parser();
	}
}
