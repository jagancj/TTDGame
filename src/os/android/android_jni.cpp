/*
 * This file is part of OpenTTD.
 * OpenTTD is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2.
 * OpenTTD is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with OpenTTD. If not, see <https://www.gnu.org/licenses/old-licenses/gpl-2.0>.
 */

/** @file android_jni.cpp JNI bridge for Android Compose HUD overlay. */

#include "../../stdafx.h"
#include "../../openttd.h"
#include "../../company_base.h"
#include "../../company_func.h"
#include "../../timer/timer_game_calendar.h"
#include "../../gfx_func.h"
#include "../../gui.h"
#include "../../fios.h"
#include "../../company_gui.h"
#include "../../rail_gui.h"
#include "../../road_gui.h"
#include "../../terraform_gui.h"
#include "../../smallmap_gui.h"
#include "../../vehicle_gui.h"
#include "../../tilehighlight_func.h"
#include "../../viewport_func.h"
#include "../../window_func.h"
#include "../../window_gui.h"
#include "../../command_func.h"
#include "../../misc_cmd.h"
#include "../../genworld.h"
#include "../../core/random_func.hpp"
#include "../../settings_type.h"
#include "../../network/network_content_gui.h"
#include "../../video/video_driver.hpp"

#include <jni.h>
#include <android/log.h>
#include <array>
#include <mutex>

#include "../../safeguards.h"

extern RailType _last_built_railtype;
extern RoadType _last_built_roadtype;
extern RoadType _last_built_tramtype;

namespace {

enum AndroidHudAction : jint {
	ACTION_NEW_GAME = 1,
	ACTION_LOAD_GAME = 2,
	ACTION_GAME_OPTIONS = 3,
	ACTION_ONLINE_CONTENT = 4,
	ACTION_TOGGLE_PAUSE = 10,
	ACTION_TOGGLE_FAST_FORWARD = 11,
	ACTION_SAVE_GAME = 12,
	ACTION_OPEN_RAIL = 20,
	ACTION_OPEN_ROAD = 21,
	ACTION_OPEN_TRAM = 22,
	ACTION_OPEN_DOCK = 23,
	ACTION_OPEN_AIRPORT = 24,
	ACTION_OPEN_TERRAFORM = 25,
	ACTION_OPEN_TRAINS = 30,
	ACTION_OPEN_ROAD_VEHS = 31,
	ACTION_OPEN_SHIPS = 32,
	ACTION_OPEN_AIRCRAFT = 33,
	ACTION_OPEN_STATIONS = 34,
	ACTION_OPEN_FINANCES = 35,
	ACTION_OPEN_TOWNS = 36,
	ACTION_OPEN_INDUSTRIES = 37,
	ACTION_OPEN_MAP = 38,
	ACTION_ZOOM_IN = 40,
	ACTION_ZOOM_OUT = 41,
	ACTION_CANCEL_TOOL = 42,
	ACTION_CLOSE_TOP_WINDOW = 43,
};

bool HasOpenDialogWindow()
{
	for (const Window *w : Window::Iterate()) {
		switch (w->window_class) {
			case WindowClass::MainWindow:
			case WindowClass::MainToolbar:
			case WindowClass::Statusbar:
			case WindowClass::SelectGame:
			case WindowClass::ToolTips:
			case WindowClass::BuildToolbar:
			case WindowClass::ScenarioGenerateLandscape:
				continue;
			default:
				return true;
		}
	}
	return false;
}

std::mutex g_hud_mutex;
std::array<jlong, 11> g_hud_state{};
bool g_confirm_active = false;
std::string g_confirm_caption;
std::string g_confirm_message;
std::function<void()> g_confirm_apply;
std::function<void()> g_confirm_cancel;

} // namespace

void AndroidShowConstructionConfirm(std::string caption, std::string message, std::function<void()> on_apply, std::function<void()> on_cancel)
{
	std::lock_guard<std::mutex> lock(g_hud_mutex);
	g_confirm_caption = std::move(caption);
	g_confirm_message = std::move(message);
	g_confirm_apply = std::move(on_apply);
	g_confirm_cancel = std::move(on_cancel);
	g_confirm_active = true;
}

void AndroidUpdateHudSnapshot()
{
	std::array<jlong, 11> state{};
	state[0] = static_cast<jlong>(_game_mode);
	state[1] = _pause_mode.Any() ? 1 : 0;
	state[2] = (_game_speed != 100) ? 1 : 0;

	if (_game_mode == GameMode::Normal && Company::IsValidID(_local_company)) {
		const Company *c = Company::Get(_local_company);
		state[3] = static_cast<jlong>(c->money);
	}

	TimerGameCalendar::YearMonthDay ymd = TimerGameCalendar::ConvertDateToYMD(TimerGameCalendar::date);
	state[4] = static_cast<jlong>(ymd.year.base());
	state[5] = static_cast<jlong>(ymd.month + 1);
	state[6] = static_cast<jlong>(ymd.day);
	state[7] = (_thd.place_mode != HT_NONE) ? 1 : 0;
	state[8] = HasOpenDialogWindow() ? 1 : 0;
	state[9] = (_game_mode == GameMode::Menu && _settings_client.network.participate_survey == ParticipateSurvey::Ask) ? 1 : 0;

	std::lock_guard<std::mutex> lock(g_hud_mutex);
	state[10] = g_confirm_active ? 1 : 0;
	g_hud_state = state;
}

extern "C" JNIEXPORT jlongArray JNICALL
Java_org_openttd_openttd_MainActivity_nativeGetHudState(JNIEnv *env, jobject /* thiz */)
{
	/* Packed state: [gameMode, isPaused, isFastForward, money, year, month, day, activeTool, hasDialogOpen, surveyAsk, confirmPending] */
	std::array<jlong, 11> state{};
	{
		std::lock_guard<std::mutex> lock(g_hud_mutex);
		state = g_hud_state;
	}

	jlongArray arr = env->NewLongArray(11);
	if (arr != nullptr) {
		env->SetLongArrayRegion(arr, 0, 11, state.data());
	}
	return arr;
}

extern "C" JNIEXPORT jobjectArray JNICALL
Java_org_openttd_openttd_MainActivity_nativeGetConfirmText(JNIEnv *env, jobject /* thiz */)
{
	std::string cap, msg;
	{
		std::lock_guard<std::mutex> lock(g_hud_mutex);
		cap = g_confirm_caption;
		msg = g_confirm_message;
	}
	jclass str_cls = env->FindClass("java/lang/String");
	jobjectArray arr = env->NewObjectArray(2, str_cls, nullptr);
	if (arr != nullptr) {
		env->SetObjectArrayElement(arr, 0, env->NewStringUTF(cap.c_str()));
		env->SetObjectArrayElement(arr, 1, env->NewStringUTF(msg.c_str()));
	}
	return arr;
}

extern "C" JNIEXPORT void JNICALL
Java_org_openttd_openttd_MainActivity_nativeResolveConfirm(JNIEnv * /* env */, jobject /* thiz */, jboolean apply)
{
	std::function<void()> cb;
	{
		std::lock_guard<std::mutex> lock(g_hud_mutex);
		cb = apply ? std::move(g_confirm_apply) : std::move(g_confirm_cancel);
		g_confirm_apply = nullptr;
		g_confirm_cancel = nullptr;
		g_confirm_active = false;
	}
	VideoDriver *vd = VideoDriver::GetInstance();
	if (vd != nullptr && cb) {
		vd->QueueOnMainThread(std::move(cb));
	}
}

extern "C" JNIEXPORT void JNICALL
Java_org_openttd_openttd_MainActivity_nativeStartNewGame(
	JNIEnv * /* env */, jobject /* thiz */,
	jint climate, jint map_log2, jint terrain, jint towns, jint industries)
{
	VideoDriver *vd = VideoDriver::GetInstance();
	if (vd == nullptr) return;

	vd->QueueOnMainThread([climate, map_log2, terrain, towns, industries]() {
		_settings_newgame.game_creation.landscape = static_cast<LandscapeType>(Clamp(climate, 0, 3));
		uint8_t size_log2 = static_cast<uint8_t>(Clamp(map_log2, 6, 11));
		_settings_newgame.game_creation.map_x = size_log2;
		_settings_newgame.game_creation.map_y = size_log2;
		_settings_newgame.difficulty.terrain_type = static_cast<GenworldMaxHeight>(Clamp(terrain, 0, 4));
		_settings_newgame.difficulty.number_towns = static_cast<uint8_t>(Clamp(towns, 0, 3));
		_settings_newgame.difficulty.industry_density = static_cast<IndustryDensity>(Clamp(industries, 1, 5));
		StartNewGameWithoutGUI(InteractiveRandom());
	});
}

extern "C" JNIEXPORT void JNICALL
Java_org_openttd_openttd_MainActivity_nativeSetSurveyChoice(JNIEnv * /* env */, jobject /* thiz */, jboolean allow)
{
	VideoDriver *vd = VideoDriver::GetInstance();
	if (vd == nullptr) return;

	vd->QueueOnMainThread([allow]() {
		_settings_client.network.participate_survey = allow ? ParticipateSurvey::Yes : ParticipateSurvey::No;
	});
}

extern "C" JNIEXPORT void JNICALL
Java_org_openttd_openttd_MainActivity_nativeSelectBuildTool(JNIEnv * /* env */, jobject /* thiz */, jint category, jint widget_id)
{
	VideoDriver *vd = VideoDriver::GetInstance();
	if (vd == nullptr) return;

	vd->QueueOnMainThread([category, widget_id]() {
		Window *w = nullptr;
		switch (category) {
			case 0: w = ShowBuildRailToolbar(_last_built_railtype); break;
			case 1: w = ShowBuildRoadToolbar(_last_built_roadtype); break;
			case 2: w = ShowBuildRoadToolbar(_last_built_tramtype); break;
			case 3: w = ShowBuildDocksToolbar(); break;
			case 4: w = ShowBuildAirToolbar(); break;
			case 5: w = ShowTerraformToolbar(); break;
			default: break;
		}
		if (w != nullptr && widget_id >= 0) {
			w->OnClick({0, 0}, widget_id, 1);
		}
	});
}

extern "C" JNIEXPORT jintArray JNICALL
Java_org_openttd_openttd_MainActivity_nativeGetSettings(JNIEnv *env, jobject /* thiz */)
{
	jint vals[7] = {
		static_cast<jint>(_settings_client.music.effect_vol),
		static_cast<jint>(_settings_client.music.music_vol),
		static_cast<jint>(_gui_scale_cfg),
		_settings_client.gui.smooth_scroll ? 1 : 0,
		_settings_client.gui.station_show_coverage ? 1 : 0,
		static_cast<jint>(_settings_client.gui.autosave_interval),
		(_settings_client.network.participate_survey == ParticipateSurvey::Yes) ? 1 : 0
	};
	jintArray arr = env->NewIntArray(7);
	if (arr != nullptr) {
		env->SetIntArrayRegion(arr, 0, 7, vals);
	}
	return arr;
}

extern "C" JNIEXPORT void JNICALL
Java_org_openttd_openttd_MainActivity_nativeApplySettings(
	JNIEnv * /* env */, jobject /* thiz */,
	jint effect_vol, jint music_vol, jint gui_scale_cfg,
	jboolean smooth_scroll, jboolean show_coverage, jint autosave_interval, jboolean survey_yes)
{
	VideoDriver *vd = VideoDriver::GetInstance();
	if (vd == nullptr) return;

	vd->QueueOnMainThread([=]() {
		_settings_client.music.effect_vol = static_cast<uint8_t>(Clamp(effect_vol, 0, 127));
		_settings_client.music.music_vol = static_cast<uint8_t>(Clamp(music_vol, 0, 127));
		_settings_client.gui.smooth_scroll = smooth_scroll;
		_settings_client.gui.station_show_coverage = show_coverage;
		_settings_client.gui.autosave_interval = static_cast<uint32_t>(std::max(0, autosave_interval));
		_settings_client.network.participate_survey = survey_yes ? ParticipateSurvey::Yes : ParticipateSurvey::No;

		if (_gui_scale_cfg != gui_scale_cfg) {
			_gui_scale_cfg = gui_scale_cfg;
			if (AdjustGUIZoom(false)) {
				ReInitAllWindows(true);
			}
		}
	});
}

extern "C" JNIEXPORT void JNICALL
Java_org_openttd_openttd_MainActivity_nativeDispatchAction(JNIEnv * /* env */, jobject /* thiz */, jint action_id)
{
	VideoDriver *vd = VideoDriver::GetInstance();
	if (vd == nullptr) return;

	vd->QueueOnMainThread([action_id]() {
		__android_log_print(ANDROID_LOG_INFO, "OpenTTD-JNI", "Dispatching action %d", action_id);
		switch (action_id) {
			case ACTION_NEW_GAME:
				__android_log_print(ANDROID_LOG_INFO, "OpenTTD-JNI", "Calling ShowGenerateLandscape()");
				ShowGenerateLandscape();
				__android_log_print(ANDROID_LOG_INFO, "OpenTTD-JNI", "Returned from ShowGenerateLandscape()");
				break;
			case ACTION_LOAD_GAME:
				ShowSaveLoadDialog(AbstractFileType::Savegame, SaveLoadOperation::Load);
				break;
			case ACTION_GAME_OPTIONS:
				ShowGameOptions();
				break;
			case ACTION_ONLINE_CONTENT:
				ShowNetworkContentListWindow(nullptr, ContentType::End);
				break;
			case ACTION_TOGGLE_PAUSE:
				Command<Commands::Pause>::Post(PauseMode::Normal, _pause_mode.None());
				break;
			case ACTION_TOGGLE_FAST_FORWARD:
				ChangeGameSpeed(_game_speed == 100);
				break;
			case ACTION_SAVE_GAME:
				ShowSaveLoadDialog(AbstractFileType::Savegame, SaveLoadOperation::Save);
				break;
			case ACTION_OPEN_RAIL:
				ShowBuildRailToolbar(_last_built_railtype);
				break;
			case ACTION_OPEN_ROAD:
				ShowBuildRoadToolbar(_last_built_roadtype);
				break;
			case ACTION_OPEN_TRAM:
				ShowBuildRoadToolbar(_last_built_tramtype);
				break;
			case ACTION_OPEN_DOCK:
				ShowBuildDocksToolbar();
				break;
			case ACTION_OPEN_AIRPORT:
				ShowBuildAirToolbar();
				break;
			case ACTION_OPEN_TERRAFORM:
				ShowTerraformToolbar();
				break;
			case ACTION_OPEN_TRAINS:
				ShowVehicleListWindow(_local_company, VehicleType::Train);
				break;
			case ACTION_OPEN_ROAD_VEHS:
				ShowVehicleListWindow(_local_company, VehicleType::Road);
				break;
			case ACTION_OPEN_SHIPS:
				ShowVehicleListWindow(_local_company, VehicleType::Ship);
				break;
			case ACTION_OPEN_AIRCRAFT:
				ShowVehicleListWindow(_local_company, VehicleType::Aircraft);
				break;
			case ACTION_OPEN_STATIONS:
				ShowCompanyStations(_local_company);
				break;
			case ACTION_OPEN_FINANCES:
				ShowCompanyFinances(_local_company);
				break;
			case ACTION_OPEN_TOWNS:
				ShowTownDirectory();
				break;
			case ACTION_OPEN_INDUSTRIES:
				ShowIndustryDirectory();
				break;
			case ACTION_OPEN_MAP:
				ShowSmallMap();
				break;
			case ACTION_ZOOM_IN:
				if (Window *w = GetMainWindow(); w != nullptr) DoZoomInOutWindow(ZOOM_IN, w);
				break;
			case ACTION_ZOOM_OUT:
				if (Window *w = GetMainWindow(); w != nullptr) DoZoomInOutWindow(ZOOM_OUT, w);
				break;
			case ACTION_CANCEL_TOOL:
				ResetObjectToPlace();
				break;
			case ACTION_CLOSE_TOP_WINDOW:
				for (Window *w : Window::Iterate()) {
					if (w->window_class != WindowClass::MainWindow &&
							w->window_class != WindowClass::MainToolbar &&
							w->window_class != WindowClass::Statusbar &&
							w->window_class != WindowClass::SelectGame &&
							w->window_class != WindowClass::ToolTips) {
						w->Close();
						break;
					}
				}
				break;
			default:
				break;
		}
	});
}
