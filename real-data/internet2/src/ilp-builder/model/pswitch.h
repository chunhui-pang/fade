#ifndef __PSWITCH_H
#define __PSWITCH_H

#include <string>

/**
 * Switch (immutable)
 * define the switch structure, its main attributes include: id, name, max_tcam (t_m)
 */
class pswitch
{
public:
	/* constructors */
	pswitch(std::string name, int max_tcam);
	
	static int generate_next_id();

	int get_id() const;

	std::string get_name() const;

	int get_max_tcam() const;

private:
	static int next_id;        /* next id */
	int id;                   /* assigned id */
	std::string name;         /* switch name */
	int max_tcam;             /* maximum TCAM */
};

#endif
