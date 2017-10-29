#include <parsers/topology_parser/topology.h>
#include <iterator>

bool topology::add_link(int src, int dst)
{
	if (0 == this->links.count(src))
	{
		this->links.insert(std::make_pair(src, std::vector<int>()));
	}
	this->links.at(src).push_back(dst);
	return true;
}

void topology::print_out(std::ostream &os) const
{
	os << "topology: " << std::endl;
	for( std::map<int,std::vector<int> >::const_iterator it = this->links.begin(); it != this->links.end(); it++)
	{
		os << it->first << " --> ";
		std::vector<int>::const_iterator it1 = it->second.begin();
		while (it1 != it->second.end())
		{
			if (it1 != it->second.begin())
			{
				os << ", ";
			}
			os << (*it1);
			it1++;
		}
		os << std::endl;
	}
}

topology::~topology()
{
	// do nothing
}
