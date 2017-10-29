#include <ec/len_ec_filter.h>

namespace ns_ec
{
	len_ec_filter::len_ec_filter(int min_len) : min_len(min_len)
	{
		
	}

	equivalent_class* len_ec_filter::filter(ns_ec::equivalent_class *ec)
	{
		if(this->min_len > ec->get_length())
			return nullptr;
		return ec;
	}

	len_ec_filter::~len_ec_filter()
	{
		
	}
}
