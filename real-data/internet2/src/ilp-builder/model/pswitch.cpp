#include <model/pswitch.h>

int pswitch::next_id = 1;

pswitch::pswitch(std::string name, int max_tcam)
{
	this->id = generate_next_id();
	this->name = name;
	this->max_tcam = max_tcam;
}

int pswitch::generate_next_id()
{
	return next_id++;
}

int pswitch::get_id() const
{
	return this->id;
}

std::string pswitch::get_name() const
{
	return this->name;
}

int pswitch::get_max_tcam() const
{
	return this->max_tcam;
}


