#ifndef __ENTITY_H
#define __ENTITY_H

#include <iostream>

class entity
{
public:
	/**
	 * output the entity
	 */
	virtual void print_out(std::ostream& os) const = 0;

	/**
	 * deconstructor
	 */
	virtual ~entity();
};

#endif
