#include <ec/ec_emitter.h>

namespace ns_ec
{
	ec_emitter::ec_emitter() : filters()
	{
		
	}

	ec_emitter* ec_emitter::add_ec_filter(ns_ec::ec_filter *filter)
	{
		this->filters.push_back(filter);
		return this;
	}

	equivalent_class* ec_emitter::do_filter(equivalent_class* ec)
	{
		for(auto it = this->filters.begin(); nullptr != ec && it != this->filters.end(); it++)
			ec = (*it)->filter(ec);
		return ec;
	}

	ec_emitter::~ec_emitter()
	{
		
	}
}
