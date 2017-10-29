PROJECT(constraint_parser)
CMAKE_MINIMUM_REQUIRED(VERSION 2.6)

add_library(constraint SHARED constraint.cpp)
target_link_libraries(constraint entity)
add_library(constraint_parser SHARED constraint_parser.cpp)
target_link_libraries(constraint_parser constraint parser)

