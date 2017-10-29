#include <builder/ilp_model_builder.h>
#include <model/aggregated_flow.h>
#include <model/pswitch.h>
#include <stdexcept>
#include <algorithm>

ilp_model_builder::ilp_model_builder(config_parser* config) : config(config), model(nullptr), state(BUILDING)
{

}

bool ilp_model_builder::build_model()
{
	if(this->state != BUILD_FINISHED && this->state != BUILDING){
		return false;
	}
	if(this->state == BUILD_FINISHED){
		return true;
	}
	this->model = new ilp_model();
	
	this->gen_variables();
	this->gen_cost_func();
	this->gen_random_probes();
	this->gen_tcam_usage_constraints();
	this->gen_tcam_limit_constraints();
	this->gen_flow_size_constraints();

	this->state = BUILD_FINISHED;
	return true;
}

const ilp_model* ilp_model_builder::get_built_model()
{
	if(this->state != BUILD_FINISHED){
		throw std::logic_error("model hasn't been built");
	}
	return this->model;
}

void ilp_model_builder::gen_random_probes()
{
	for(int r = 1; r <= this->config->get_maximum_run(); r++){
		for(aggregated_flow* af : this->config->get_aggregated_flows()){
			af->generate_random_probes(r-1);
		}
	}
}

void ilp_model_builder::gen_variables()
{
	/* n_{fr}: for flow $f$, in run $r$ */
	for(aggregated_flow* af : this->config->get_aggregated_flows()){
		for(int r = 1; r <= this->config->get_maximum_run(); r++){
			this->model->add_integer_var("n_" + std::to_string(af->get_id()) + "_" + std::to_string(r));
		}
	}
	/* u_{sr}: used tcam on switch $s$ in run $r$ */
	for(pswitch* psw : this->config->get_switches()){
		for(int r = 1; r <= this->config->get_maximum_run(); r++){
			this->model->add_integer_var("u_" + std::to_string(psw->get_id()) + "_" + std::to_string(r));
		}
	}
	/* t_m: the maximum used tcam on all switches, in all run */
	this->model->add_integer_var("t_m");
}

void ilp_model_builder::gen_cost_func()
{
	std::vector<double> coefficient;
	/* fill 0 with all previous variables: n_{fr}, u_{sr} */
	for(int i = 1; i <= this->config->get_aggregated_flows().size() * this->config->get_maximum_run(); i++){
		coefficient.push_back(0);
	}
	for(int i = 1; i <= this->config->get_switches().size() * this->config->get_maximum_run(); i++){
		coefficient.push_back(0);
	}
	/* t_m */
	coefficient.push_back(1);
	this->model->set_model_goal(coefficient);
	this->model->minimize();
}

/* as the lack of index, this function is very inefficient */
void ilp_model_builder::gen_tcam_usage_constraints()
{
	/* construct a constraint for every switch, in every run */
	/* \forall $r$, \forall $s$, $\sum_{f \in F}i_{sf}*n_{fr} - u_{sr} + 0t_m + \sum_{f \in F}m_{sfr} = 0 */
	for(pswitch* psw : this->config->get_switches()){
		for(int r = 1; r <= this->config->get_maximum_run(); r++){
			std::vector<double> coefficients;
			for(aggregated_flow* af : this->config->get_aggregated_flows()){
				for(int r1 = 1; r1 <= this->config->get_maximum_run(); r1++){
					/* coefficient for variable n_{fr}: i_{sf} */
					double coef = 0.0f;
					if(r1 == r && af->get_path().size() != 0 && af->get_path().at(0) == psw){
						coef += 1.0f;
					}
					coefficients.push_back(coef);
				}
			}
			for(pswitch* psw1 : this->config->get_switches()){
				for(int r1 = 1; r1 <= this->config->get_maximum_run(); r1++){
					/* coefficient for variable u_{sr}: -1 if psw1 == psw, r1 ==r else 0 */
					if(psw1 == psw && r1 == r){
						coefficients.push_back(-1.0f);
					} else {
						coefficients.push_back(0.0f);
					}
				}
			}
			/* 0 t_m */
			coefficients.push_back(0);
			/* \sum_{f \in F}m_{sfr} */
			double m_sfr = 0;
			for(aggregated_flow* af : this->config->get_aggregated_flows()){
				/* the last hop */
				int path_len = af->get_path().size();
				if( path_len != 0 && af->get_path().at(path_len - 1) == psw ){
					m_sfr += 1.0f;
				}
				/* intermediate probes */
				const std::vector<const pswitch*>& probe = af->get_probes( r - 1 );
				if( std::find(probe.begin()+1, probe.end()-1, psw) != probe.end()-1){
					m_sfr += 1.0f;
				}
			}
			// this->model->add_constraint(coefficients, m_sfr);
			// std::transform(coefficients.begin(), coefficients.end(), coefficients.begin(), [](double coef) -> double {return -coef;});
			// this->model->add_constraint(coefficients, -m_sfr);
			this->model->add_equal_constraint(coefficients, m_sfr);
		}
	}
}

void ilp_model_builder::gen_tcam_limit_constraints()
{
	/**
	 * construct tcam usage constraint
	 * \forall $s$, \forall $r$, u_{sr} <= t_s 
	 */
	for(pswitch* psw : this->config->get_switches()){
		for(int r = 1; r <= this->config->get_maximum_run(); r++){
			std::vector<double> coefficients;
			/* 0 n_{fr} */
			for(aggregated_flow* af : this->config->get_aggregated_flows()){
				for(int r1 = 1; r1 <= this->config->get_maximum_run(); r1++){
					coefficients.push_back(0.0f);
				}
			}
			/* u_{sr} */
			for(pswitch* psw1 : this->config->get_switches()){
				for(int r1 = 1; r1 <= this->config->get_maximum_run(); r1++){
					if(psw1 == psw && r1 == r){
						coefficients.push_back(1.0f);
					} else {
						coefficients.push_back(0.0f);
					}
				}
			}
			/* 0 t_m */
			coefficients.push_back(0);
			/* b */
			double b = -1 * this->config->get_maximum_tcams(psw);
			this->model->add_constraint(coefficients, b);
		}
	}
	
	/* u_{sr} <= t_m */
	for(pswitch* psw : this->config->get_switches()){
		for(int r = 1; r <= this->config->get_maximum_run(); r++){
			std::vector<double> coefficients;
			/* 0 n_{fr} */
			for(aggregated_flow* af : this->config->get_aggregated_flows()){
				for(int r1 = 1; r1 <= this->config->get_maximum_run(); r1++){
					coefficients.push_back(0.0f);
				}
			}
			/* u_{sr} */
			for(pswitch* psw1 : this->config->get_switches()){
				for(int r1 = 1; r1 <= this->config->get_maximum_run(); r1++){
					if(psw1 == psw && r1 == r){
						coefficients.push_back(1.0f);
					} else {
						coefficients.push_back(0.0f);
					}
				}
			}
			/* 0 t_m */
			coefficients.push_back(-1.0f);
			/* b */
			double b = 0.0f;
			this->model->add_constraint(coefficients, b);
		}
	}
}

void ilp_model_builder::gen_flow_size_constraints()
{
	/* construct flow size constraints, i.e., 
	 * \forall $f$, $\sum_{r \in R}n_{fr} = |f|$ 
	 */
	for(aggregated_flow* af : this->config->get_aggregated_flows()){
		/* sum_{r \in R}n_{fr} */
		std::vector<double> coefficients;
		/* n_{fr} */
		for(aggregated_flow* af1 : this->config->get_aggregated_flows()){
			for(int r = 1; r <= this->config->get_maximum_run(); r++){
				/* 1 if af1 == af else 0 */
				if(af1 == af){
					coefficients.push_back(1.0f);
				} else {
					coefficients.push_back(0.0f);
				}
			}
		}
		/* u_{sr} */
		for(pswitch* psw : this->config->get_switches()){
			for(int r = 1; r <= this->config->get_maximum_run(); r++){
				coefficients.push_back(0.0f);
			}
		}
		/* t_m */
		coefficients.push_back(0.0f);
		/* b */
		double b = -1.0f * af->get_size();
		this->model->add_equal_constraint(coefficients, b);
	}
}
