PROJECT(topology_parser)
CMAKE_MINIMUM_REQUIRED(VERSION 2.6)

add_library(topology SHARED topology.cpp)
target_link_libraries(topology entity)
add_library(topology_parser SHARED topology_parser.cpp)
target_link_libraries(topology_parser topology parser)
