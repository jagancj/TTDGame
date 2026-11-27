/*
 * This file is part of OpenTTD.
 * OpenTTD is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2.
 * OpenTTD is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with OpenTTD. If not, see <https://www.gnu.org/licenses/old-licenses/gpl-2.0>.
 */

/** @file unix_main.cpp Main entry for Unix. */

#include "../../stdafx.h"
#include "../../openttd.h"
#include "../../crashlog.h"
#include "../../core/random_func.hpp"
#include "../../string_func.h"

#include <time.h>
#include <signal.h>

#ifdef __ANDROID__
#include <SDL3/SDL_main.h>
#include <SDL3/SDL_system.h>
#include <stdlib.h>
#include <string>
#include <sys/stat.h>
#include <unistd.h>
#endif

#include "../../safeguards.h"

int CDECL main(int argc, char *argv[])
{
#ifdef __ANDROID__
	if (const char *internal_path = SDL_GetAndroidInternalStoragePath(); internal_path != nullptr) {
		chdir(internal_path);
		/* Keep HOME distinct from WorkingDir so DoScanWorkingDirectory() does not skip baseset/. */
		std::string home_path = std::string(internal_path) + "/home";
		mkdir(home_path.c_str(), 0755);
		setenv("HOME", home_path.c_str(), 1);
	}
#endif
	/* Make sure our arguments contain only valid UTF-8 characters. */
	std::vector<std::string_view> params;
	for (int i = 0; i < argc; ++i) {
		StrMakeValidInPlace(argv[i]);
		params.emplace_back(argv[i]);
	}

	CrashLog::InitialiseCrashLog();

	SetRandomSeed(time(nullptr));

	signal(SIGPIPE, SIG_IGN);

	return openttd_main(params);
}
