# CMake generated Testfile for 
# Source directory: D:/Games/TTDGame/regression/regression
# Build directory: D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/regression/regression
# 
# This file includes the relevant testing commands required for 
# testing this directory and lists subdirectories to be tested as well.
add_test(regression_regression "C:/Users/CJ/AppData/Local/Android/Sdk/cmake/3.22.1/bin/cmake.exe" "-DOPENTTD_EXECUTABLE=D:/Games/TTDGame/android/app/build/intermediates/cxx/Release/b422o2r6/obj/arm64-v8a/libopenttd.so" "-DEDITBIN_EXECUTABLE=" "-DREGRESSION_TEST=regression" "-DSCRIPT_FOLDER=ai" "-P" "D:/Games/TTDGame/cmake/scripts/Regression.cmake")
set_tests_properties(regression_regression PROPERTIES  RUN_SERIAL "TRUE" WORKING_DIRECTORY "D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a" _BACKTRACE_TRIPLES "D:/Games/TTDGame/cmake/CreateRegression.cmake;49;add_test;D:/Games/TTDGame/regression/regression/CMakeLists.txt;2;create_regression;D:/Games/TTDGame/regression/regression/CMakeLists.txt;0;")
