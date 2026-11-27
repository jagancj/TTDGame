/*
 * This file is part of OpenTTD.
 * OpenTTD is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, version 2.
 * OpenTTD is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with OpenTTD. If not, see <https://www.gnu.org/licenses/old-licenses/gpl-2.0>.
 */

/** @file sdl3_v.h Base of the SDL3 video driver. */

#ifndef VIDEO_SDL_H
#define VIDEO_SDL_H

#include <chrono>
#include <cmath>
#include <vector>
#include "video_driver.hpp"

/** The SDL video driver. */
class VideoDriver_SDL_Base : public VideoDriver {
public:
	/**
	 * Create the video driver.
	 * @param uses_hardware_acceleration Whether hardware acceleration is used by this instance of the driver.
	 * @param supports_animation_buffer Whether this driver supports a hardware accelerated animation buffer.
	 */
	VideoDriver_SDL_Base(bool uses_hardware_acceleration = false, bool supports_animation_buffer = false) : VideoDriver(uses_hardware_acceleration, supports_animation_buffer) {}

	std::optional<std::string_view> Start(const StringList &param) override;

	void Stop() override;

	void MakeDirty(int left, int top, int width, int height) override;

	void MainLoop() override;

	bool ChangeResolution(int w, int h) override;

	bool ToggleFullscreen(bool fullscreen) override;

	bool AfterBlitterChange() override;

	void ClaimMousePointer() override;

	void EditBoxGainedFocus() override;

	void EditBoxLostFocus() override;

	std::vector<int> GetListOfMonitorRefreshRates() override;

	std::string_view GetInfoString() const override { return this->driver_info; }

	void SetScreensaverInhibited(bool inhibited) override;

	float GetDPIScale() const override;

	RectPadding GetSafeAreaInsets() const override { return this->safe_area_insets; }

protected:
	struct SDL_Window *sdl_window = nullptr; ///< Main SDL window.
	Palette local_palette{}; ///< Current palette to use for drawing.
	bool buffer_locked = false; ///< Video buffer was locked by the main thread.
	Rect dirty_rect{}; ///< Rectangle encompassing the dirty area of the video buffer.
	std::string driver_info{}; ///< Information string about selected driver.
	RectPadding safe_area_insets{}; ///< Display safe-area insets in pixels.

	void UpdateSafeAreaInsets();

	Dimension GetScreenSize() const override;
	void InputLoop() override;
	bool LockVideoBuffer() override;
	void UnlockVideoBuffer() override;
	void CheckPaletteAnim() override;
	bool PollEvent() override;

	void ClientSizeChanged(int w, int h, bool force);

	/**
	 * (Re-)create the backing store.
	 * @param w The width of the window.
	 * @param h The height of the window.
	 * @param force Whether to force full reallocation, instead of not reallocating when size did not change.
	 * @return Whether the backing store was (re-)created.
	 */
	virtual bool AllocateBackingStore(int w, int h, bool force = false) = 0;

	/**
	 * Get a pointer to the video buffer.
	 * @return The pointer.
	 */
	virtual void *GetVideoPointer() = 0;

	/** Hand video buffer back to the painting backend. */
	virtual void ReleaseVideoPointer() = 0;

	/**
	 * Create the main window.
	 * @param w The width of the window.
	 * @param h The height of the window.
	 * @param flags SDL specific flags for the window.
	 * @return Whether the window was created or already existed.
	 */
	virtual bool CreateMainWindow(uint w, uint h, uint flags = 0);

private:
	void LoopOnce();
	bool CreateMainSurface(uint w, uint h, bool resize);
	std::optional<std::string_view> Initialize();

#ifdef __EMSCRIPTEN__
	/* Convert a constant pointer back to a non-constant pointer to a member function. */
	static void EmscriptenLoop(void *self) { ((VideoDriver_SDL_Base *)self)->LoopOnce(); }
#endif

	/**
	 * This is true to indicate that keyboard input is in text input mode, and SDL_TEXTINPUT events are enabled.
	 */
	bool edit_box_focused = false;

	int startup_display = 0; ///< The display to show OpenTTD on when starting.

	/** Tracking state for touch input: direct tapping, two-finger panning, and pinch-to-zoom. */
	struct TouchPoint {
		int64_t id = 0;
		int x = 0;
		int y = 0;
		int start_x = 0;
		int start_y = 0;
		std::chrono::steady_clock::time_point start_time{};
	};

	struct TouchState {
		std::vector<TouchPoint> points;
		bool is_dragging = false;       ///< Single finger left-drag active.
		bool is_panning = false;        ///< Single finger viewport pan active.
		bool is_list_scrolling = false; ///< Single finger touch-drag list scroll active.
		bool is_multi_touch = false;    ///< Multi-touch (panning or pinching) active.
		bool started_on_map = false;    ///< Touch started on MainWindow viewport.
		bool started_on_scrollable = false; ///< Touch started on a scrollable non-scrollbar widget.
		bool long_press_fired = false;  ///< Long-press right-click already triggered.
		float last_pinch_distance = 0.0f;
		float accumulated_pinch = 0.0f;
		float accumulated_scroll_y = 0.0f;
		int last_mid_x = 0;
		int last_mid_y = 0;
		int last_x = 0;
		int last_y = 0;

		TouchPoint *FindPoint(int64_t id) {
			for (auto &pt : this->points) {
				if (pt.id == id) return &pt;
			}
			return nullptr;
		}

		void RemovePoint(int64_t id) {
			std::erase_if(this->points, [id](const TouchPoint &pt) { return pt.id == id; });
		}
	} touch_state{};
};

#endif /* VIDEO_SDL_H */
