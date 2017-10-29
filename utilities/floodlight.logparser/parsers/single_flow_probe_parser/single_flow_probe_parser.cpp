#include <parsers/single_flow_probe_parser/single_flow_probe_parser.h>
#include <cstdlib>

const std::string single_flow_probe_parser::PARSER_NAME = "single_flow_probe parser";

static const std::string SINGLE_FLOW_NODE_REGEX = "SingleFlowNode.*?datapathId=.*?:([0-9a-f]{2}),.*?ipv4_dst=([0-9.]+).*?dependsOnSize=0}}";
static const std::string SINGLE_FLOW_REGEX = "SingleFlow\\{flowId=([0-9]+).*?flowNodes=\\[((" + SINGLE_FLOW_NODE_REGEX + "(,\\W)?)+)\\]\\}";
static const std::string PROBE_CONTENT_REGEX = "(" + SINGLE_FLOW_NODE_REGEX + "(,\\W)?)+";
const std::regex single_flow_probe_parser::SINGLE_FLOW_NODE(SINGLE_FLOW_NODE_REGEX);
const std::regex single_flow_probe_parser::SINGLE_FLOW_NODE_EX( "(" + SINGLE_FLOW_NODE_REGEX + ")(,\\W)?(.*)" );
const std::regex single_flow_probe_parser::SINGLE_FLOW(SINGLE_FLOW_REGEX);
const std::regex single_flow_probe_parser::SINGLE_FLOW_EX("(" + SINGLE_FLOW_REGEX + "),\\W(.*)");
const std::regex single_flow_probe_parser::PROBE_CONTENT( PROBE_CONTENT_REGEX );
const std::regex single_flow_probe_parser::SINGLE_FLOW_PROBE(".*select\\Wprobes\\Wfor\\Wflow\\W(" + SINGLE_FLOW_REGEX + "),\\Wprobes\\Ware\\W\\[(" + PROBE_CONTENT_REGEX + ")\\]");

void single_flow_probe_parser::parse_flow(const std::string &flow_content)
{
	std::smatch sm;
	if (std::regex_match(flow_content, sm, SINGLE_FLOW))
	{
		int flow_id = std::atoi(sm[1].str().c_str());
		std::string nodes_content = sm[2].str();
		while (std::regex_match(nodes_content, sm, SINGLE_FLOW_NODE_EX))
		{
			std::string node_content = sm[1].str();
			nodes_content = sm[sm.size()-1].str();
			if (std::regex_match(node_content, sm, SINGLE_FLOW_NODE))
			{
				std::string dst = sm[2].str();
				if (NULL == this->result)
					this->result = new single_flow_probe(flow_id, dst);
				int dpid = std::strtol(sm[1].str().c_str(), NULL, 10);
				this->result->append_switch(dpid);
			}
		}
	}
}

void single_flow_probe_parser::parse_probes(const std::string &probes_content)
{
	std::smatch sm;
	std::string content = probes_content;
	while (std::regex_match(content, sm, SINGLE_FLOW_NODE_EX))
	{
		std::string node_content = sm[1].str();
		content = sm[sm.size()-1].str();
		if (std::regex_match(node_content, sm, SINGLE_FLOW_NODE))
		{
			int dpid = std::strtol(sm[1].str().c_str(), NULL, 10);
			this->result->append_probe(dpid);
		}
	}
}

single_flow_probe_parser::single_flow_probe_parser() : result(NULL)
{
	
}

const std::string& single_flow_probe_parser::get_name() const
{
	return PARSER_NAME;
}

parser::parse_result single_flow_probe_parser::parse_line(const std::string &line, int n)
{
	std::smatch sm;
	if (std::regex_match(line, sm, SINGLE_FLOW_PROBE))
	{
		std::string flow_content = sm[1].str();
		std::string probe_content = sm[8].str();
		this->parse_flow(flow_content);
		this->parse_probes(probe_content);
		return SUCCESS_AND_STOP;
	}
	return parser::SKIP;
}

const entity* single_flow_probe_parser::pop_parse_result()
{
	const entity* tmp = this->result;
	this->result = NULL;
	return tmp;
}

single_flow_probe_parser::~single_flow_probe_parser()
{
	if (NULL != this->result)
		delete this->result;
}

extern "C"
{
	parser* create_parser()
	{
		return new single_flow_probe_parser();
	}
}

