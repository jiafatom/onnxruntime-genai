include(${CMAKE_SOURCE_DIR}/cmake/cxx_standard.cmake)

# unit tests program
add_executable(unit_tests)

file(GLOB test_srcs CONFIGURE_DEPENDS
  "${CMAKE_CURRENT_SOURCE_DIR}/*.h"
  "${CMAKE_CURRENT_SOURCE_DIR}/*.cpp"
)

target_sources(unit_tests PRIVATE ${test_srcs})

target_include_directories(unit_tests PRIVATE
  ${ORT_HEADER_DIR}
  ${onnxruntime_extensions_SOURCE_DIR}/shared/api
  ${CMAKE_SOURCE_DIR}/src
)

target_link_directories(unit_tests PRIVATE ${ORT_LIB_DIR})
target_link_libraries(unit_tests PRIVATE
  onnxruntime-genai
  onnxruntime_extensions
  GTest::gtest
)
set_target_properties(unit_tests PROPERTIES
    RUNTIME_OUTPUT_DIRECTORY "$<TARGET_FILE_DIR:onnxruntime-genai>"
)

if(NOT (CMAKE_SYSTEM_NAME STREQUAL "Android" OR CMAKE_SYSTEM_NAME STREQUAL "Linux" OR CMAKE_SYSTEM_NAME STREQUAL "Darwin"))
target_link_libraries(unit_tests PRIVATE ${ONNXRUNTIME_LIB})
endif()

set(TEST_MODEL_SRC_DIR "${CMAKE_CURRENT_SOURCE_DIR}/test_models/")

add_compile_definitions(MODEL_PATH="${TEST_MODEL_SRC_DIR}")
set_target_properties(unit_tests PROPERTIES FOLDER "Tests")
get_target_property(all_test_srcs unit_tests SOURCES)
source_group(TREE ${PROJECT_SOURCE_DIR} FILES ${all_test_srcs})
set_property(DIRECTORY ${CMAKE_SOURCE_DIR} PROPERTY VS_STARTUP_PROJECT unit_tests)

# Hide symbols by default, so that shared libraries don't link to our redirected symbols (leads to infinite loops)
if (NOT MSVC)
  target_compile_options(unit_tests PRIVATE "-fvisibility=hidden")
endif()

add_test(NAME UnitTests COMMAND unit_tests)
