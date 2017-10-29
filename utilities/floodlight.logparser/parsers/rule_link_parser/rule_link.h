#ifndef __RULE_LINK_H
#define __RULE_LINK_H
#include <entity.h>
class rule_link : public entity
{
private:
	std::string dst;
	int src_sw;
	int dst_sw;
public:
	rule_link(const std::string& dst, int src_sw, int dst_sw);
		
	virtual void print_out(std::ostream& os) const;

	virtual ~rule_link();
};
#endif
