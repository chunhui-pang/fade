#include <parsers/aggregated_flow_probe_parser/aggregated_flow_probe_parser.h>
#include <cstdlib>

const std::string aggregated_flow_probe_parser::PARSER_NAME = "aggregated_flow_probe parser";

static const std::string AGGREGATED_FLOW_NODE_REGEX = "AggregatedFlowNode.*?(.*?datapathId=.*?:([0-9a-f]{2}),.*?ipv4_dst=([0-9.]+).*?dependsOnSize=0}(,\\W)?)(,\\W)?+\\]\\}";
static const std::string AGGREGATED_FLOW_REGEX = "AggregatedFlow\\{flowId=([0-9]+).*?flowNodes=\\[((" + AGGREGATED_FLOW_NODE_REGEX + "(,\\W)?)+)\\]\\}";
static const std::string PROBE_CONTENT_REGEX = "(" + AGGREGATED_FLOW_NODE_REGEX + "(,\\W)?)+";
const std::regex aggregated_flow_probe_parser::AGGREGATED_FLOW_NODE(AGGREGATED_FLOW_NODE_REGEX);
const std::regex aggregated_flow_probe_parser::AGGREGATED_FLOW_NODE_EX( "(" + AGGREGATED_FLOW_NODE_REGEX + ")(,\\W)?(.*)" );
const std::regex aggregated_flow_probe_parser::AGGREGATED_FLOW(AGGREGATED_FLOW_REGEX);
const std::regex aggregated_flow_probe_parser::AGGREGATED_FLOW_EX("(" + AGGREGATED_FLOW_REGEX + "),\\W(.*)");
const std::regex aggregated_flow_probe_parser::PROBE_CONTENT( PROBE_CONTENT_REGEX );
const std::regex aggregated_flow_probe_parser::AGGREGATED_FLOW_PROBE(".*select\\Wprobes\\Wfor\\Wflow\\W(" + AGGREGATED_FLOW_REGEX + "),\\Wprobes\\Ware\\W\\[(" + PROBE_CONTENT_REGEX + ")\\]");

void aggregated_flow_probe_parser::parse_flow(const std::string &flow_content)
{
	std::smatch sm;
	if (std::regex_match(flow_content, sm, AGGREGATED_FLOW))
	{
		int flow_id = std::atoi(sm[1].str().c_str());
		std::string nodes_content = sm[2].str();
		while (std::regex_match(nodes_content, sm, AGGREGATED_FLOW_NODE_EX))
		{
			std::string node_content = sm[1].str();
			nodes_content = sm[sm.size()-1].str();
			if (std::regex_match(node_content, sm, AGGREGATED_FLOW_NODE))
			{
				std::string dst = sm[3].str();
				if (NULL == this->result)
					this->result = new aggregated_flow_probe(flow_id, dst);
				int dpid = std::strtol(sm[2].str().c_str(), NULL, 16);
				this->result->append_switch(dpid);
			}
		}
	}
}

void aggregated_flow_probe_parser::parse_probes(const std::string &probes_content)
{
	std::smatch sm;
	std::string content = probes_content;
	while (std::regex_match(content, sm, AGGREGATED_FLOW_NODE_EX))
	{
		std::string node_content = sm[1].str();
		content = sm[sm.size()-1].str();
		if (std::regex_match(node_content, sm, AGGREGATED_FLOW_NODE))
		{
			int dpid = std::strtol(sm[2].str().c_str(), NULL, 16);
			this->result->append_probe(dpid);
		}
	}
}

aggregated_flow_probe_parser::aggregated_flow_probe_parser() : result(NULL)
{
	
}

const std::string& aggregated_flow_probe_parser::get_name() const
{
	return PARSER_NAME;
}

parser::parse_result aggregated_flow_probe_parser::parse_line(const std::string &line, int n)
{
	std::smatch sm;
	if (std::regex_match(line, sm, AGGREGATED_FLOW_PROBE))
	{
		std::string flow_content = sm[1].str();
		std::string probe_content = sm[11].str();
		this->parse_flow(flow_content);
		this->parse_probes(probe_content);
		return SUCCESS_AND_STOP;
	}
	return parser::SKIP;
}

const entity* aggregated_flow_probe_parser::pop_parse_result()
{
	const entity* tmp = this->result;
	this->result = NULL;
	return tmp;
}

aggregated_flow_probe_parser::~aggregated_flow_probe_parser()
{
	if (NULL != this->result)
		delete this->result;
}

extern "C"
{
	parser* create_parser()
	{
		return new aggregated_flow_probe_parser();
	}
}

