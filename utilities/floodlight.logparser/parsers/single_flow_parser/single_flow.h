#ifndef __SINGLE_FLOW_H
#define __SINGLE_FLOW_H

#include <entity.h>
#include <vector>
#include <map>
#include <string>

typedef std::vector<int> flow_path;

class single_flow : public entity
{
private:
	std::map<std::string, std::vector< flow_path > > flows;

	void print_flow_path(std::ostream& os, flow_path path) const;
	
public:
	bool add_flow(const std::string& dst, flow_path path);
	
	single_flow();

	virtual void print_out(std::ostream& os) const;

	virtual ~single_flow();
};
#endif
