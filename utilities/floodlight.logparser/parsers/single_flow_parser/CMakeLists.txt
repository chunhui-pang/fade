PROJECT(single_flow_parser)
CMAKE_MINIMUM_REQUIRED(VERSION 2.6)

add_library(single_flow SHARED single_flow.cpp)
target_link_libraries(single_flow entity)
add_library(single_flow_parser SHARED single_flow_parser.cpp)
target_link_libraries(single_flow_parser single_flow parser)

