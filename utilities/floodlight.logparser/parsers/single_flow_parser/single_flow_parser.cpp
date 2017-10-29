#include <parsers/single_flow_parser/single_flow_parser.h>
#include <cstdlib>
#include <iostream>

static const std::string SINGLE_FLOW_NODE_REGEX = "SingleFlowNode.*?datapathId=.*?:([0-9a-f]{2}),.*?ipv4_dst=([0-9.]+).*?dependsOnSize=0}}";
static const std::string SINGLE_FLOW_REGEX = "SingleFlow\\{.*?flowNodes=\\[((" + SINGLE_FLOW_NODE_REGEX + "(,\\W)?)+)\\]\\}";
const std::string single_flow_selector_parser::PARSER_NAME = "single_flow parser";
const std::regex single_flow_selector_parser::SINGLE_FLOW_BEGIN(".*SingleFlowSelector.*-----\\WSINGLE\\WFLOW\\WSELECTOR\\WINFO\\W-----.*");
const std::regex single_flow_selector_parser::SINGLE_FLOW_END(".*SingleFlowSelector.*-----\\WSINGLE\\WFLOW\\WSELECTOR\\WINFO\\WEND\\W-----.*");
const std::regex single_flow_selector_parser::SINGLE_FLOW_NODE(SINGLE_FLOW_NODE_REGEX);
const std::regex single_flow_selector_parser::SINGLE_FLOW_NODE_EX("(" + SINGLE_FLOW_NODE_REGEX + ")(,\\W)?(.*)");
const std::regex single_flow_selector_parser::SINGLE_FLOW(SINGLE_FLOW_REGEX);
const std::regex single_flow_selector_parser::SINGLE_FLOW_EX("(" + SINGLE_FLOW_REGEX + "),\\W(.*)");
const std::regex single_flow_selector_parser::SINGLE_FLOW_CONTENT(".*SingleFlowSelector.*?((" + SINGLE_FLOW_REGEX + ",\\W)+)");


single_flow_selector_parser::single_flow_selector_parser() : result(NULL)
{
	
}

const std::string& single_flow_selector_parser::get_name() const
{
	return PARSER_NAME;
}

parser::parse_result single_flow_selector_parser::parse_line(const std::string &line, int line_counter)
{
	std::smatch sm;
	if (std::regex_match(line, SINGLE_FLOW_BEGIN))
	{
		this->result = new single_flow();
		return parser::SUCCESS_AND_CONTINUE;
	}
	else if (std::regex_match(line, sm, SINGLE_FLOW_CONTENT))
	{
		std::string flows_content = sm[1].str();
		do {
			std::regex_match(flows_content, sm, SINGLE_FLOW_EX);
			std::string flow_content = sm[1].str();
			this->parse_single_flow(flow_content);
			flows_content = sm[sm.size()-1].str();
		} while(flows_content.size() != 0);
		return parser::SUCCESS_AND_CONTINUE;
	}
	else if (std::regex_match(line, SINGLE_FLOW_END))
	{
		return parser::SUCCESS_AND_STOP;
	}
	else
	{
		return parser::SKIP;
	}
}

const entity* single_flow_selector_parser::pop_parse_result()
{
	entity* tmp = this->result;
	this->result = NULL;
	return tmp;
}

single_flow_selector_parser::~single_flow_selector_parser()
{
	
}

void single_flow_selector_parser::parse_single_flow(const std::string &flow_content)
{
	std::smatch sm;
	std::smatch sm_node;
	std::regex_match(flow_content, sm, SINGLE_FLOW);
	std::string nodes_content = sm[1].str();
	std::string dst;
	std::vector<int> path;
	do {
		std::regex_match(nodes_content, sm, SINGLE_FLOW_NODE_EX);
		std::string node_content = sm[1].str();
		std::regex_match(node_content, sm_node, SINGLE_FLOW_NODE);
		dst = sm_node[2].str();
		int dpid = std::strtol(sm_node[1].str().c_str(), NULL, 10);
		path.push_back(dpid);
		nodes_content = sm[sm.size()-1];
	} while (nodes_content.size() != 0);
	this->result->add_flow(dst, path);
}

extern "C"
{
	parser* create_parser()
	{
		return new single_flow_selector_parser();
	}
}
