#ifndef __TOPOLOGY_H
#define __TOPOLOGY_H

#include <entity.h>
#include <map>
#include <vector>

class topology : public entity
{
private:
	std::map<int, std::vector<int> > links;
public:
	bool add_link(int src, int dst);
	
	virtual void print_out(std::ostream& os) const;

	virtual ~topology();
};
#endif
