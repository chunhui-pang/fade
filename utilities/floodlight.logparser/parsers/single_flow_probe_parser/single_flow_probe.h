#ifndef __PROBE_H
#define __PROBE_H

#include <vector>
#include <entity.h>

class single_flow_probe : public entity
{
private:
	int flow_id;
	std::string dst;
	std::vector<int> dpids;
	std::vector<bool> is_probe;
	
public:
	single_flow_probe(int flow_id, const std::string& dst);

	void append_switch(int dpid);

	bool append_probe(int dpid);

	virtual void print_out(std::ostream& os) const;

	virtual ~single_flow_probe();
};
#endif
