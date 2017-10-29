#ifndef __INTERFACE_H
#define __INTERFACE_H
	
#include <string>
#include <iostream>
#include <vector>

namespace ns_network {

	using std::string;
	using std::ostream;
	using std::pair;
	using std::vector;

	typedef unsigned int uint;

	struct interface_addr {
		uint destination;
		uint mask_len;
		uint local_addr;
		interface_addr(uint destination, uint mask_len, uint local_addr);
	};

	class pswitch;
	class interface
	{
	private:
		pswitch* sw;
		int id;
		string name;
		vector<interface_addr> addrs;

	public:
		interface();
		interface(pswitch* sw, const int id, const string& name, const vector<interface_addr>& addrs);
		interface(const interface& intf);
		~interface();

		pswitch* get_switch() const;

		interface* set_switch(pswitch* sw);

		int get_id() const;
		interface* set_id(const int id);
    
		string get_name() const;
		interface* set_name(const string& name);

		const vector<interface_addr>& get_addrs() const;
		interface* set_addrs(const vector<interface_addr>& addrs);
		interface* add_addr(const uint prefix, const uint mask, const uint local_addr);
		interface* add_addr(const string& destination, const string& local_addr);
	};

	bool operator < (const interface& left, const interface& right);
	bool operator==(const interface& left, const interface& right);
	ostream& operator << (ostream& os, const interface& intf);

}
#endif
