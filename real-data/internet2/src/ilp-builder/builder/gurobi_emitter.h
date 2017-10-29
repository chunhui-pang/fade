#ifndef __GUROBI_EMITTER_H
#define __GUROBI_EMITTER_H

#include <ostream>
#include <string>
#include <model/ilp_model.h>

/**
 * emit gurobi optimization specification file
 */
class gurobi_emitter
{
public:
	gurobi_emitter(const ilp_model* model);
	bool generate_code(std::ostream& os);

private:
	const ilp_model* model;

	static const std::string START_MAX_GOAL;
	static const std::string START_MIN_GOAL;
	static const std::string START_CONSTRAINT;
	static const std::string START_BOUNDS;
	static const std::string START_INTEGER_VARIABLES;
	static const std::string START_BINARY_VARIABLES;
	static const std::string END_MODEL;

	bool generate_goal(std::ostream& os);
	bool generate_constraints(std::ostream& os);
	bool generate_equal_constraints(std::ostream& os);
	bool generate_bounds(std::ostream& os);
	bool generate_variables(std::ostream& os);
	bool generate_end_tag(std::ostream& os);
	
	bool genreate_constraint(std::ostream& os, const std::vector<double>& coefs, const std::vector<std::string>& vars, const double& b, const std::string& name = "", bool equal = false);
};

#endif
