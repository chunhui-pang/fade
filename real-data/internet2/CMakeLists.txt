cmake_minimum_required (VERSION 2.6)
project (Internet2-Analyzer)
set (Internet2-Analyzer_VERSION_MAJOR 1)
set (Internet2-Analyzer_VERSION_MINOR 0)

add_subdirectory (src)
add_custom_target(copy-fib-parser ALL COMMAND cp src/fib-parser/fib-parser fib-parser DEPENDS src/fib-parser/fib-parser)
add_custom_target(copy-ilp-builder ALL COMMAND cp src/ilp-builder/ilp-builder ilp-builder DEPENDS src/ilp-builder/ilp-builder)
add_custom_target(copy-solver.sh ALL COMMAND cp src/ilp-builder/solver.sh solver.sh DEPENDS src/ilp-builder/solver.sh)
add_custom_target(copy-gurobi-result-sorter ALL COMMAND cp src/gurobi-result-sorter/gurobi-result-sorter gurobi-result-sorter DEPENDS src/gurobi-result-sorter)

