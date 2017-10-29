PROJECT(localization_parser)
CMAKE_MINIMUM_REQUIRED(VERSION 2.6)

add_library(localization SHARED localization.cpp)
target_link_libraries(localization entity)
add_library(localization_parser SHARED localization_parser.cpp)
target_link_libraries(localization_parser localization parser)

