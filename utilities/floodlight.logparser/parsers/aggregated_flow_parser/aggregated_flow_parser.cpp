#include <parsers/aggregated_flow_parser/aggregated_flow_parser.h>
#include <cstdlib>
#include <iostream>

const std::string aggregated_flow_parser::PARSER_NAME = "aggregated_flow parser";

static const std::string FLOW_RULE_NODE_REGEX = "FlowRuleNode\\{.*?datapathId=.*?:([0-9]{2}),.*?ipv4_dst=([0-9.\\/]+).*?outPort=([0-9]+).*?dependsOnSize=0\\}";
static const std::string AGGREGATED_FLOW_NODE_REGEX = "AggregatedFlowNode\\{.*?ruleNodes=\\[((" + FLOW_RULE_NODE_REGEX + "(,\\W)?)+)\\]\\}";
static const std::string AGGREGATED_FLOW_REGEX = ".*?AggregatedFlow\\{.*?flowNodes=\\[((" + AGGREGATED_FLOW_NODE_REGEX + "(,\\W)?)+)\\]\\}.*";
const std::regex aggregated_flow_parser::DUMP_BEGIN(".*-----\\WAGGREGATED\\WFLOW\\WSELECTOR\\WINFO\\W-----.*");
const std::regex aggregated_flow_parser::DUMP_END(".*-----\\WAGGREGATED\\WFLOW\\WSELECTOR\\WINFO\\WEND\\W-----.*");
const std::regex aggregated_flow_parser::FLOW_RULE_NODE(FLOW_RULE_NODE_REGEX);
const std::regex aggregated_flow_parser::FLOW_RULE_NODE_EX( "(" + FLOW_RULE_NODE_REGEX + ")(,\\W)?(.*)");
const std::regex aggregated_flow_parser::AGGREGATED_FLOW_NODE(AGGREGATED_FLOW_NODE_REGEX);
const std::regex aggregated_flow_parser::AGGREGATED_FLOW_NODE_EX( "(" + AGGREGATED_FLOW_NODE_REGEX + ")(,\\W)?(.*)");
const std::regex aggregated_flow_parser::AGGREGATED_FLOW(AGGREGATED_FLOW_REGEX);

aggregated_flow_parser::aggregated_flow_parser() : result(NULL)
{
	
}

bool aggregated_flow_parser::parse_flow_node(const std::string &flow_node_content, int line_counter)
{
	std::smatch sm;
	if(std::regex_match(flow_node_content, sm, AGGREGATED_FLOW_NODE))
	{
		std::string rule_nodes_content = sm[1].str();
		int dpid = 0;
		int port = 0;
		while(std::regex_match(rule_nodes_content, sm, FLOW_RULE_NODE_EX))
		{
			/* actually, dpid are decimal numbers */
			int c_dpid = std::strtol(sm[2].str().c_str(), NULL, 10);
			int c_port = std::atoi(sm[4].str().c_str());
			if (dpid != 0 && (c_dpid != dpid || c_port != port))
			{
				std::cerr << "Line " << line_counter << ": wrong aggregated flow, switches not common: " << c_dpid << " vs " << dpid << std::endl;
				return false;
			}
			else if(dpid == 0)
			{
				dpid = c_dpid;
				port = c_port;
			}
			std::string dst = sm[3].str();
			this->result->add_dst(dst);
			rule_nodes_content = sm[6].str();
		}
		this->result->add_switch_node(dpid, port);
		return true;
	}
	else
	{
		return false;
	}
}

const std::string& aggregated_flow_parser::get_name() const
{
	return PARSER_NAME;
}

parser::parse_result aggregated_flow_parser::parse_line(const std::string &line, int n)
{
	std::smatch sm;
	if (std::regex_match(line, DUMP_BEGIN))
	{
		this->result = new aggregated_flow();
		return SUCCESS_AND_CONTINUE;
	}
	else if(std::regex_match(line, DUMP_END))
	{
		return SUCCESS_AND_STOP;
	}
	else if(std::regex_match(line, sm, AGGREGATED_FLOW) && NULL != this->result)
	{
		this->result->start_new_flow();
		std::string flow_nodes_content = sm[1].str();
		while(std::regex_match(flow_nodes_content, sm, AGGREGATED_FLOW_NODE_EX))
		{
			std::string flow_node_content = sm[1].str();
			flow_nodes_content = sm[sm.size()-1].str();
			if(!this->parse_flow_node(flow_node_content, n))
				break;
		}
		return SUCCESS_AND_CONTINUE;
	}
	return SKIP;
}

const entity* aggregated_flow_parser::pop_parse_result()
{
	const entity* tmp = this->result;
	this->result = NULL;
	return tmp;
}

aggregated_flow_parser::~aggregated_flow_parser()
{
	if (NULL != this->result)
		delete this->result;
}

extern "C"
{
	parser* create_parser()
	{
		return new aggregated_flow_parser();
	}
}
