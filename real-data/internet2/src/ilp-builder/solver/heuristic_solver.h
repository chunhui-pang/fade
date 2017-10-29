#ifndef __HEURISTIC_SOLUTION_H
#define __HEURISTIC_SOLUTION_H

#include <model/aggregated_flow.h>
#include <ostream>

class heuristic_solver
{
public:
	/**
	 * solve the problem. This function only provide flows, other information could be provided by the constructor
	 */
	virtual bool solve(const std::vector<aggregated_flow*>& flows, std::ostream& os) = 0;

	heuristic_solver();

	virtual ~heuristic_solver();
};

#endif
