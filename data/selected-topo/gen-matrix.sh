#!/bin/bash

dir_prefix=topology-zoo/
len_prefix=${#dir_prefix}
while read line; do
    name=$( echo $line | cut -f1 -d ' ' )
    name=${name:$len_prefix:-4}
    output=$name.gml.dat
    if [ -e $output ]; then
	rm $output
    fi
    echo $name > $output
    ../gml2matrix ../topology-zoo/$name.gml >> $output
done < selected-topo.dat 

