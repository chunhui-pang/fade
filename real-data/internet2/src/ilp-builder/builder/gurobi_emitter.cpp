#include <builder/gurobi_emitter.h>
#include <algorithm>

const std::string gurobi_emitter::START_MAX_GOAL = "Maximize";
const std::string gurobi_emitter::START_MIN_GOAL = "Minimize";
const std::string gurobi_emitter::START_CONSTRAINT = "Subject To";
const std::string gurobi_emitter::START_BOUNDS = "Bounds";
const std::string gurobi_emitter::START_INTEGER_VARIABLES = "Integers";
const std::string gurobi_emitter::START_BINARY_VARIABLES = "Binaries";
const std::string gurobi_emitter::END_MODEL = "End";

gurobi_emitter::gurobi_emitter(const ilp_model* model) : model(model)
{

}

bool gurobi_emitter::generate_code(std::ostream &os)
{
	return this->generate_goal(os) && 
		this->generate_equal_constraints(os) &&
		this->generate_constraints(os) && 
		this->generate_bounds(os) && 
		this->generate_variables(os) && 
		this->generate_end_tag(os);
}

bool gurobi_emitter::generate_goal(std::ostream &os)
{
	if(this->model->is_maxmize_model()){
		os << START_MAX_GOAL << std::endl;
	} else {
		os << START_MIN_GOAL << std::endl;
	}
	os << "\t";
	auto it_var = this->model->get_vars().begin();
	auto it_coef = this->model->get_c().begin();
	bool first = true;
	while(it_var != this->model->get_vars().end() && it_coef != this->model->get_c().end()){
		if( (*it_coef) != 0.0f){
			if( std::abs(*it_coef) == 1.0f ){
				os << ( (*it_coef) < 0.0f ? " - " : ((!first) ? " + " : " ") ) << (*it_var);
			} else {
				os << ( (*it_coef) < 0.0f ? " - " : ((!first) ? " + " : " ") ) << std::abs(*it_coef) << " " << (*it_var);
			}
			first = false;
		}
		it_var++, it_coef++;
	}
	os << std::endl;
	return true;
}

bool gurobi_emitter::generate_equal_constraints(std::ostream &os)
{
	os << START_CONSTRAINT << std::endl;
	int idx = 1;
	auto it_coefs = this->model->get_equal_A().begin();
	auto it_b = this->model->get_equal_b().begin();
	while(it_coefs != this->model->get_equal_A().end() && it_b != this->model->get_equal_b().end()){
		this->genreate_constraint(os, *it_coefs, this->model->get_vars(), *it_b, "EC" + std::to_string(idx++), true);
		it_coefs++, it_b++;
	}
	return true;
}

bool gurobi_emitter::generate_constraints(std::ostream &os)
{
	auto it_coefs = this->model->get_A().begin();
	auto it_b = this->model->get_b().begin();
	int idx = 1;
	while(it_coefs != this->model->get_A().end() && it_b != this->model->get_b().end()){
		this->genreate_constraint(os, *it_coefs, this->model->get_vars(), *it_b, "C" + std::to_string(idx++));
		it_coefs++, it_b++;
	}
	return true;
}

bool gurobi_emitter::generate_bounds(std::ostream &os)
{
	os << "Bounds" << std::endl;
	/* we have no bounds in our model */
	return true;
}

bool gurobi_emitter::generate_variables(std::ostream &os)
{
	std::vector<std::string> integers;
	std::vector<std::string> binaries;
	auto it_var_type = this->model->get_var_types().begin();
	auto it_var = this->model->get_vars().begin();
	while (it_var_type != this->model->get_var_types().end() && it_var != this->model->get_vars().end()) {
		switch((*it_var_type)){
		case ilp_model::INTEGER_VAR:
			integers.push_back(*it_var);
			break;
		case ilp_model::BINARY_VAR:
			binaries.push_back(*it_var);
			break;
		default:
			break;
		}
		it_var_type++, it_var++;
	}
	os << START_INTEGER_VARIABLES << std::endl;
	os << "\t";
	for(std::string& var : integers){
		os << " " << var;
	}
	os << std::endl;

	os << START_BINARY_VARIABLES << std::endl;
	os << "\t";
	for(std::string& var : binaries){
		os << " " << var;
	}
	os << std::endl;
	return true;
}

bool gurobi_emitter::generate_end_tag(std::ostream &os)
{
	os << END_MODEL << std::endl;
	return true;
}

bool gurobi_emitter::genreate_constraint(std::ostream &os, const std::vector<double> &coefs, const std::vector<std::string> &vars, const double &b, const std::string& name, bool equal)
{
	if(name.length() != 0){
		os << "\t" << name << ": ";
	}
	auto it_coef = coefs.begin();
	auto it_var = vars.begin();
	bool first = true;
	while(it_coef != coefs.end() && it_var != vars.end()){
		if( (*it_coef) != 0.0 ){
			if( std::abs(*it_coef) == 1.0f ){
				os << ( (*it_coef) < 0.0f ? ((!first) ? " - " : "-") : ((!first) ? " + " : " ") ) << (*it_var);
			} else {
				os << ( (*it_coef) < 0.0f ? ((!first) ? " - " : "-") : ((!first) ? " + " : " ") ) << std::abs(*it_coef) << " " << (*it_var);
			}
			first = false;
		}
		it_coef++, it_var++;
	}
	os << (equal ? " = " : " <= ") << (-b) << std::endl;
	return true;
}
