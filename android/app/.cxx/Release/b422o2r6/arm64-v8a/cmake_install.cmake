# Install script for directory: D:/Games/TTDGame

# Set the install prefix
if(NOT DEFINED CMAKE_INSTALL_PREFIX)
  set(CMAKE_INSTALL_PREFIX "C:/Program Files (x86)/openttd")
endif()
string(REGEX REPLACE "/$" "" CMAKE_INSTALL_PREFIX "${CMAKE_INSTALL_PREFIX}")

# Set the install configuration name.
if(NOT DEFINED CMAKE_INSTALL_CONFIG_NAME)
  if(BUILD_TYPE)
    string(REGEX REPLACE "^[^A-Za-z0-9_]+" ""
           CMAKE_INSTALL_CONFIG_NAME "${BUILD_TYPE}")
  else()
    set(CMAKE_INSTALL_CONFIG_NAME "Release")
  endif()
  message(STATUS "Install configuration: \"${CMAKE_INSTALL_CONFIG_NAME}\"")
endif()

# Set the component getting installed.
if(NOT CMAKE_INSTALL_COMPONENT)
  if(COMPONENT)
    message(STATUS "Install component: \"${COMPONENT}\"")
    set(CMAKE_INSTALL_COMPONENT "${COMPONENT}")
  else()
    set(CMAKE_INSTALL_COMPONENT)
  endif()
endif()

# Install shared libraries without execute permission?
if(NOT DEFINED CMAKE_INSTALL_SO_NO_EXE)
  set(CMAKE_INSTALL_SO_NO_EXE "0")
endif()

# Is this installation the result of a crosscompile?
if(NOT DEFINED CMAKE_CROSSCOMPILING)
  set(CMAKE_CROSSCOMPILING "TRUE")
endif()

# Set default install directory permissions.
if(NOT DEFINED CMAKE_OBJDUMP)
  set(CMAKE_OBJDUMP "C:/Users/CJ/AppData/Local/Android/Sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-objdump.exe")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/SDL3_build/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/bin/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/src/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/media/cmake_install.cmake")
endif()

if(NOT CMAKE_INSTALL_LOCAL_ONLY)
  # Include the install script for the subdirectory.
  include("D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/regression/cmake_install.cmake")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libopenttd.so" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libopenttd.so")
    file(RPATH_CHECK
         FILE "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libopenttd.so"
         RPATH "")
  endif()
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/lib" TYPE SHARED_LIBRARY FILES "D:/Games/TTDGame/android/app/build/intermediates/cxx/Release/b422o2r6/obj/arm64-v8a/libopenttd.so")
  if(EXISTS "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libopenttd.so" AND
     NOT IS_SYMLINK "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libopenttd.so")
    if(CMAKE_INSTALL_DO_STRIP)
      execute_process(COMMAND "C:/Users/CJ/AppData/Local/Android/Sdk/ndk/27.0.12077973/toolchains/llvm/prebuilt/windows-x86_64/bin/llvm-strip.exe" "$ENV{DESTDIR}${CMAKE_INSTALL_PREFIX}/lib/libopenttd.so")
    endif()
  endif()
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xUnspecifiedx" OR NOT CMAKE_INSTALL_COMPONENT)
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xlanguage_filesx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/games/openttd" TYPE DIRECTORY FILES
    "D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/lang"
    "D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/baseset"
    "D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/ai"
    "D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/game"
    "D:/Games/TTDGame/bin/scripts"
    REGEX "ai/[^.]+$" EXCLUDE)
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xdocsx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/doc/openttd" TYPE FILE FILES
    "D:/Games/TTDGame/COPYING.md"
    "D:/Games/TTDGame/README.md"
    "D:/Games/TTDGame/CREDITS.md"
    "D:/Games/TTDGame/CONTRIBUTING.md"
    "D:/Games/TTDGame/changelog.md"
    "D:/Games/TTDGame/known-bugs.md"
    )
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xdocsx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/doc/openttd/docs" TYPE FILE FILES
    "D:/Games/TTDGame/docs/admin_network.md"
    "D:/Games/TTDGame/docs/debugging_desyncs.md"
    "D:/Games/TTDGame/docs/desync.md"
    "D:/Games/TTDGame/docs/directory_structure.md"
    "D:/Games/TTDGame/docs/eints.md"
    "D:/Games/TTDGame/docs/fonts.md"
    "D:/Games/TTDGame/docs/game_coordinator.md"
    "D:/Games/TTDGame/docs/linkgraph.md"
    "D:/Games/TTDGame/docs/logging_and_performance_metrics.md"
    "D:/Games/TTDGame/docs/multiplayer.md"
    "D:/Games/TTDGame/docs/savegame_format.md"
    "D:/Games/TTDGame/docs/symbol_server.md"
    "D:/Games/TTDGame/docs/obg_format.txt"
    "D:/Games/TTDGame/docs/obm_format.txt"
    "D:/Games/TTDGame/docs/obs_format.txt"
    )
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmanualx" OR NOT CMAKE_INSTALL_COMPONENT)
  
                execute_process(COMMAND C:/Users/CJ/AppData/Local/Android/Sdk/cmake/3.22.1/bin/cmake.exe -E copy D:/Games/TTDGame/docs/openttd.6 D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/docs/openttd.6)
                execute_process(COMMAND gzip -9 -n -f D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/docs/openttd.6)
            
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmanualx" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/man/man6" TYPE FILE FILES "D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/docs/openttd.6.gz")
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmediax" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share" TYPE DIRECTORY FILES
    "D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/media/icons"
    "D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/media/pixmaps"
    )
endif()

if("x${CMAKE_INSTALL_COMPONENT}x" STREQUAL "xmenux" OR NOT CMAKE_INSTALL_COMPONENT)
  file(INSTALL DESTINATION "${CMAKE_INSTALL_PREFIX}/share/applications" TYPE FILE FILES "D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/media/openttd.desktop")
endif()

if(CMAKE_INSTALL_COMPONENT)
  set(CMAKE_INSTALL_MANIFEST "install_manifest_${CMAKE_INSTALL_COMPONENT}.txt")
else()
  set(CMAKE_INSTALL_MANIFEST "install_manifest.txt")
endif()

string(REPLACE ";" "\n" CMAKE_INSTALL_MANIFEST_CONTENT
       "${CMAKE_INSTALL_MANIFEST_FILES}")
file(WRITE "D:/Games/TTDGame/android/app/.cxx/Release/b422o2r6/arm64-v8a/${CMAKE_INSTALL_MANIFEST}"
     "${CMAKE_INSTALL_MANIFEST_CONTENT}")
