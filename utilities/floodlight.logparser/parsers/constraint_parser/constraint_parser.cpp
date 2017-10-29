#include <parsers/constraint_parser/constraint_parser.h>
#include <cstdlib>

const std::string constraint_parser::PARSER_NAME = "constraint parser";

static const std::string STATS_REGEX = "stats\\(\\W([0-9]+)\\W\\)";
const std::regex constraint_parser::FLOW_REMOVED(".*receive\\WOFFlowRemoved\\Wmessage\\WOFFlowRemovedVer10.*ipv4_dst=([\\d.]+).*cookie=0x([0-9a-f]+).*from\\Wswitch\\W.*:([0-9a-f]+).*");
const std::regex constraint_parser::FLOW_STATS(".*retrieve\\Wflow\\Wrule\\Wwith\\Windex\\W([0-9]+).*packet\\Wcount\\W([0-9]+)");
const std::regex constraint_parser::STATS( STATS_REGEX );
const std::regex constraint_parser::STATS_EX( ".*?" + STATS_REGEX + "(.*)");
const std::regex constraint_parser::CONSTRAINT(".*constraint\\W.*?\\{flowId=([0-9]+).*?((" + STATS_REGEX + "(.*?))+)\\}\\],\\W.*result\\Wis\\W(true|false).*");

constraint_parser::constraint_parser() : result(NULL)
{
	
}

const std::string& constraint_parser::get_name() const
{
	return PARSER_NAME;
}

parser::parse_result constraint_parser::parse_line(const std::string &line, int n)
{
	std::smatch sm;
	if (std::regex_match(line, sm, FLOW_STATS))
	{
		// put statistics
		long cookie = std::atol(sm[1].str().c_str());
		long pkt = std::atol(sm[2].str().c_str());
		constraint::put_statistics(cookie, pkt);
		return SUCCESS_AND_CONTINUE;
	}
	else if (std::regex_match(line, sm, CONSTRAINT))
	{
		// output constraint and related statistics
		std::string constraints_content = sm[2].str();
		std::string result = sm[sm.size()-1].str();
		bool b_result = ("false" == result ? false : true);
		int flow_id = std::atoi(sm[1].str().c_str());
		this->result = new constraint(flow_id, b_result);
		while (std::regex_match(constraints_content, sm, STATS_EX))
		{
			long cookie = std::atol(sm[1].str().c_str());
			this->result->add_probe_cookie(cookie);
			constraints_content = sm[2].str();
		}
		return SUCCESS_AND_STOP;
	}
	else if (std::regex_match(line, sm, FLOW_REMOVED))
	{
		std::string dst = sm[1].str();
		long cookie = std::strtol(sm[2].str().c_str(), NULL, 16);
		long dpid = std::strtol(sm[3].str().c_str(), NULL, 10);
		constraint::put_cookie_dpid(cookie, dpid);
		constraint::put_cookie_dst(cookie, dst);
		// other parsers may need this also
		return SKIP;
	}
	return SKIP;
}

const entity* constraint_parser::pop_parse_result()
{
	entity* tmp = this->result;
	this->result = NULL;
	return tmp;
}

constraint_parser::~constraint_parser()
{
	if (NULL != this->result)
		delete this->result;
}

extern "C"
{
	parser* create_parser()
	{
		return new constraint_parser();
	}
}
