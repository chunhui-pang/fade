#ifndef __ILP_MODEL_H
#define __ILP_MODEL_H

#include <vector>
#include <set>
#include <string>

/**
 * A ilp model representation 
 */
class ilp_model
{
public:
	ilp_model();
	enum VAR_TYPE {CONTINUOUS_VAR, INTEGER_VAR, BINARY_VAR};
	/* maximize / minimize */
	void maximize();
	void minimize();
	/* add continuous var */
	int add_var(const std::string& var);
	int add_var(const std::vector<std::string>& vars);
	/* add interger var */
	int add_integer_var(const std::string& var);
	int add_integer_var(const std::vector<std::string>& vars);
	/* add binary var */
	int add_binary_var(const std::string& var);
	int add_binary_var(const std::vector<std::string>& vars);
	
	/* add goal */
	void set_model_goal(const std::vector<double>& coefficient);

	/* add constraint */
	int add_constraint(const std::vector<double>& coefficient, const double& b);
	int add_constraint(const std::vector< std::vector<double> >& coefficients, const std::vector<double>& bs);
	int add_equal_constraint(const std::vector<double>& coefficient, const double& b);
	int add_equal_constraint(const std::vector< std::vector<double> >& coefficients, const std::vector<double>& be);

	/* readers */
	bool is_maxmize_model() const;	
	const std::vector<std::string>& get_vars() const;
	const std::vector<ilp_model::VAR_TYPE>& get_var_types() const;
	const std::vector<double>& get_c() const;
	const std::vector< std::vector<double> >& get_A() const;
	const std::vector<double>& get_b() const;
	const std::vector< std::vector<double> >& get_equal_A() const;
	const std::vector<double>& get_equal_b() const;
	
private:
	std::set<std::string> uniq_vars;
	std::vector<std::string> vars;
	std::vector<VAR_TYPE> var_types;
	int add_var(const std::string& var, VAR_TYPE var_type);
	/* max/min (cx) */
	enum {MINIMIZE_MODEL, MAXIMIZE_MODEL} model_type;
	std::vector<double> c;
	/* Ax + b <= 0 */
	std::vector< std::vector<double> > A;
	std::vector<double> b;
	std::vector< std::vector<double> > Ae;
	std::vector<double> be;

	
};
#endif
