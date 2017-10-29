#include <parsers/topology_parser/topology_parser.h>
#include <cstdlib>

const std::string topology_parser::PARSER_NAME = "topology parser";
const std::regex topology_parser::TOPO_BEGIN(".*ConcreteTopology.*-----\\WTOPOLOGY\\WINFO\\W-----.*");
const std::regex topology_parser::TOPO_CONTENT("(.*ConcreteTopology)?.*([0-9a-f]{2}):([0-9]+)\\W---\\W.*([0-9a-f]{2}):([0-9]+).*");
const std::regex topology_parser::TOPO_END(".*ConcreteTopology.*-----\\WTOPOLOGY\\WINFO\\WEND\\W-----.*");

topology_parser::topology_parser() : result(NULL)
{
	
}

const std::string& topology_parser::get_name() const
{
	return PARSER_NAME;
}

parser::parse_result topology_parser::parse_line(const std::string &str, int n)
{
	std::smatch sm;
	if (std::regex_match(str, TOPO_BEGIN)) {
		this->result = new topology();
		return parser::SUCCESS_AND_CONTINUE;
	} else if (std::regex_match(str, sm, TOPO_CONTENT)) {
		int src = std::strtol(sm[2].str().c_str(), NULL, 10);
		int dst = std::strtol(sm[4].str().c_str(), NULL, 10);
		this->result->add_link(src, dst);
		return parser::SUCCESS_AND_CONTINUE;
	} else if (std::regex_match(str, TOPO_END)) {
		return parser::SUCCESS_AND_STOP;
	} else {
		return parser::SKIP;
	}
}


const entity* topology_parser::pop_parse_result()
{
	topology* tmp = this->result;
	this->result = NULL;
	return tmp;
}

topology_parser::~topology_parser()
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
		return new topology_parser();
	}
}
