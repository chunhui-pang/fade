PROJECT(rule_link_parser)
CMAKE_MINIMUM_REQUIRED(VERSION 2.6)

add_library(rule_link SHARED rule_link.cpp)
target_link_libraries(rule_link entity)
add_library(rule_link_parser SHARED rule_link_parser.cpp)
target_link_libraries(rule_link_parser rule_link parser)

