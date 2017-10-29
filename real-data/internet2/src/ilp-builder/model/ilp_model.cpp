#include <model/ilp_model.h>

ilp_model::ilp_model() : vars(), uniq_vars(), var_types(), model_type(MINIMIZE_MODEL), c(), A(), b(), Ae(), be()
{
	
}

void ilp_model::maximize()
{
	this->model_type = MAXIMIZE_MODEL;
}

void ilp_model::minimize()
{
	this->model_type = MINIMIZE_MODEL;
}

int ilp_model::add_var(const std::string &var)
{
	return this->add_var(var, CONTINUOUS_VAR);
}

int ilp_model::add_var(const std::vector<std::string> &vars)
{
	int n_succ = 0;
	for(std::string var : vars){
		n_succ += this->add_var(var);
	}
	return n_succ;
}

int ilp_model::add_integer_var(const std::string &var)
{
	return this->add_var(var, INTEGER_VAR);
}

int ilp_model::add_integer_var(const std::vector<std::string> &vars)
{
	int n_succ = 0;
	for(std::string var : vars){
		n_succ += this->add_integer_var(var);
	}
	return n_succ;
}

int ilp_model::add_binary_var(const std::string &var)
{
	return this->add_var(var, BINARY_VAR);
}

int ilp_model::add_binary_var(const std::vector<std::string> &vars)
{
	int n_succ = 0;
	for(std::string var : vars){
		n_succ += this->add_binary_var(var);
	}
	return n_succ;
}

int ilp_model::add_var(const std::string &var, ilp_model::VAR_TYPE var_type)
{
	if( !this->uniq_vars.insert(var).second ){
		return 0;
	}
	this->vars.push_back(var);
	this->var_types.push_back(var_type);
	return 1;
}

void ilp_model::set_model_goal(const std::vector<double> &coefficient)
{
	this->c = coefficient;
}

int ilp_model::add_constraint(const std::vector<double> &coefficient, const double& b)
{
	this->A.push_back(coefficient);
	this->b.push_back(b);
	return 1;
}

int ilp_model::add_equal_constraint(const std::vector<double> &coefficient, const double& b)
{
	this->Ae.push_back(coefficient);
	this->be.push_back(b);
	return 1;
}

bool ilp_model::is_maxmize_model() const
{
	return this->model_type == MAXIMIZE_MODEL;
}

const std::vector<std::string>& ilp_model::get_vars() const
{
	return this->vars;
}

const std::vector<ilp_model::VAR_TYPE>& ilp_model::get_var_types() const
{
	return this->var_types;
}

const std::vector<double>& ilp_model::get_c() const
{
	return this->c;
}

const std::vector< std::vector<double> >& ilp_model::get_A() const
{
	return this->A;
}

const std::vector<double>& ilp_model::get_b() const
{
	return this->b;
}

const std::vector< std::vector<double> >& ilp_model::get_equal_A() const
{
	return this->Ae;
}

const std::vector<double>& ilp_model::get_equal_b() const
{
	return this->be;
}

int ilp_model::add_constraint(const std::vector<std::vector<double> > &coefficients, const std::vector<double> &bs)
{
	if(coefficients.size() != bs.size()){
		return 0;
	}
	int n_succ = 0;
	for(int i = 0; i < coefficients.size(); i++){
		n_succ += this->add_constraint(coefficients.at(i), bs.at(i));
	}
	return n_succ;
}

int ilp_model::add_equal_constraint(const std::vector<std::vector<double> > &coefficients, const std::vector<double> &be)
{
	if(coefficients.size() != be.size()){
		return 0;
	}
	int n_succ = 0;
	for(int i = 0; i < coefficients.size(); i++){
		n_succ += this->add_equal_constraint(coefficients.at(i), be.at(i));
	}
	return n_succ;
}

