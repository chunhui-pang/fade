#ifndef __LOCALIZATION_H
#define __LOCALIZATION_H

#include <entity.h>
class localization : public entity
{
private:
	int flow_id;
	
public:
	localization(int flow_id);

	virtual void print_out(std::ostream& os) const;

	virtual ~localization();
};
#endif
