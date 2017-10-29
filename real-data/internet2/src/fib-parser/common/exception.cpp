#include <common/exception.h>

namespace ns_common {

	invalid_element::invalid_element(std::string msg) : std::logic_error(msg)
	{
		
	}

	
}
