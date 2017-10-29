#include <cstdlib>

#include <network/interface.h>
#include <network/pswitch.h>
#include <common/exception.h>
#include <common/util.h>

namespace ns_network {

	using ns_common::str2ip;
	using ns_common::invalid_element;
	
	interface_addr::interface_addr(uint destination, uint mask_len, uint local_addr) : destination(destination), mask_len(mask_len), local_addr(local_addr)
	{

	}

	interface::interface() : sw(nullptr), id(0), name(""), addrs()
	{
    
	}

	interface::interface(pswitch* sw, const int id, const string& name, const vector<interface_addr>& addrs) : sw(sw), id(id), name(name), addrs(addrs)
	{
	}

	interface::interface(const interface& intf)
	{
		this->id = intf.id;
		this->sw = intf.sw;
		this->name = intf.name;
		this->addrs = std::vector<interface_addr>(intf.addrs);
	}

	interface::~interface()
	{
    
	}

	pswitch* interface::get_switch() const
	{
		return this->sw;
	}

	interface* interface::set_switch(pswitch *sw)
	{
		this->sw = sw;
		return this;
	}

	int interface::get_id() const
	{
		return this->id;
	}

	interface* interface::set_id(const int id)
	{
		this->id = id;
		return this;
	}

	string interface::get_name() const
	{
		return this->name;
	}

	interface* interface::set_name(const string& name)
	{
		this->name = name;
		return this;
	}

	const vector<interface_addr>& interface::get_addrs() const
	{
		return this->addrs;
	}

	interface* interface::set_addrs(const vector<interface_addr>& addrs)
	{
		this->addrs = addrs;
		return this;
	}

	interface* interface::add_addr(const uint prefix, const uint mask_len, const uint local_addr)
	{
		this->addrs.push_back(interface_addr(prefix, mask_len, local_addr));
		return this;
	}

	interface* interface::add_addr(const string& destination, const string& local_addr)
	{
		uint tip = str2ip(local_addr.c_str());
		if(tip == 0)
			throw invalid_element("invalid ip address '"+ local_addr + "'");
    
		uint mask_len = 32;
		uint dest = 0;
		uint slash_pos = destination.find('/');
		if(slash_pos != string::npos){
			mask_len = atoi(destination.substr(slash_pos+1).c_str());
			dest  = str2ip(destination.substr(0, slash_pos).c_str());
		}else{
			dest = str2ip(destination.c_str());
		}
		if(0 == dest)
			throw invalid_element("unrecongnized ip address '" + destination + "'");
		this->addrs.push_back(interface_addr(dest, mask_len, tip));
		return this;
	}

	bool operator < (const interface& left, const interface& right)
	{
		if(left.get_switch() == NULL && right.get_switch() != NULL)
			return true;
		else if(left.get_switch() != NULL && right.get_switch() == NULL)
			return false;
		else if(left.get_switch() == NULL && right.get_switch() == NULL)
			return left.get_id() < right.get_id();
		else{
			if(left.get_switch()->get_name() == right.get_switch()->get_name()){
				return left.get_id() < right.get_id();
			}else{
				return left.get_switch()->get_name() < right.get_switch()->get_name();
			}
		}
	}

	bool operator == (const interface& left, const interface &right)
	{
		if(left.get_switch() == NULL && right.get_switch() != NULL)
			return false;
		else if(left.get_switch() != NULL && right.get_switch() == NULL)
			return false;
		else if(left.get_switch() == NULL && right.get_switch() == NULL)
			return left.get_id() == right.get_id();
		else{
			if(*(left.get_switch()) == *(right.get_switch())){
				return left.get_id() == right.get_id();
			}else{
				return false;
			}
		}
	}

	ostream& operator << (ostream& os, const interface& intf)
	{
    
		os << "id='" << intf.get_id() << "', "
		   << "name='" << intf.get_name() <<"', destination='";
		uint dest = intf.get_addrs().at(0).destination;
		os << ((dest >> 24) & 0xFF) << "." << ((dest >> 16) & 0xFF) << "." << ((dest >> 8) & 0xFF) << "." << (dest & 0xFF) << "/" << intf.get_addrs().at(0).mask_len << "', "
		   << "local addr='";
		uint local_addr = intf.get_addrs().at(0).local_addr;
		os << ((local_addr >> 24) & 0xFF) << "." << ((local_addr >> 16) & 0xFF) << "." << ((local_addr >> 8) & 0xFF) << "." << (local_addr & 0xFF) << "'";
		return os;
	}

}
