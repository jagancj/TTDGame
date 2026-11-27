package org.openttd.openttd

import android.content.pm.ActivityInfo
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import android.view.WindowManager
import androidx.compose.runtime.*
import androidx.compose.ui.platform.ComposeView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import kotlinx.coroutines.delay
import org.libsdl.app.SDLActivity
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Modern Android entry-point activity for OpenTTD extending SDLActivity,
 * hosting a Jetpack Compose dark glassmorphic HUD overlay above the SDL game surface.
 */
class MainActivity : SDLActivity(), LifecycleOwner, SavedStateRegistryOwner {

    companion object {
        private const val TAG = "OpenTTD-MainActivity"
        private const val BASESET_ASSET_SUBDIR = "baseset"
        private const val LANG_ASSET_SUBDIR = "lang"
    }

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateRegistryController = SavedStateRegistryController.create(this)

    override val lifecycle: Lifecycle
        get() = lifecycleRegistry

    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private external fun nativeGetHudState(): LongArray?
    private external fun nativeDispatchAction(actionId: Int)
    private external fun nativeStartNewGame(climate: Int, mapLog2: Int, terrain: Int, towns: Int, industries: Int)
    private external fun nativeSetSurveyChoice(allow: Boolean)
    private external fun nativeSelectBuildTool(category: Int, widgetId: Int)
    private external fun nativeGetConfirmText(): Array<String>?
    private external fun nativeResolveConfirm(apply: Boolean)
    private external fun nativeGetSettings(): IntArray?
    private external fun nativeApplySettings(
        effectVol: Int,
        musicVol: Int,
        guiScaleCfg: Int,
        smoothScroll: Boolean,
        showCoverage: Boolean,
        autosaveInterval: Int,
        surveyYes: Boolean
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        savedStateRegistryController.performRestore(savedInstanceState)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)

        // Ensure OpenGFX, OpenSFX, base graphics, and language packs are extracted from APK assets
        // to the internal filesDir before native C++ code initializes.
        val prefs = getSharedPreferences("openttd_assets", MODE_PRIVATE)
        val pkgUpdateTime = packageManager.getPackageInfo(packageName, 0).lastUpdateTime
        val forceRefresh = prefs.getLong("last_update_time", 0L) != pkgUpdateTime

        extractAssetSubdir(BASESET_ASSET_SUBDIR, forceRefresh)
        extractAssetSubdir(LANG_ASSET_SUBDIR, forceRefresh)

        if (forceRefresh) {
            prefs.edit().putLong("last_update_time", pkgUpdateTime).apply()
        }

        super.onCreate(savedInstanceState)
        setupImmersiveDisplay()
        attachComposeHudOverlay()
    }

    override fun onStart() {
        super.onStart()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun onResume() {
        super.onResume()
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    override fun onPause() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        super.onPause()
    }

    override fun onStop() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        super.onStop()
    }

    override fun onDestroy() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        super.onDestroy()
    }

    override fun getLibraries(): Array<String> {
        return arrayOf(
            "SDL3",
            "openttd"
        )
    }

    override fun getMainFunction(): String {
        return "SDL_main"
    }

    private fun attachComposeHudOverlay() {
        window.decorView.setViewTreeLifecycleOwner(this)
        window.decorView.setViewTreeSavedStateRegistryOwner(this)

        val composeView = ComposeView(this).apply {
            setViewTreeLifecycleOwner(this@MainActivity)
            setViewTreeSavedStateRegistryOwner(this@MainActivity)
            setContent {
                var hudState by remember { mutableStateOf(HudState()) }

                LaunchedEffect(Unit) {
                    while (true) {
                        val raw = try {
                            nativeGetHudState()
                        } catch (_: UnsatisfiedLinkError) {
                            null
                        }
                        if (raw != null && raw.size >= 11) {
                            val confirmPending = raw[10] != 0L
                            val confirmTexts = if (confirmPending) {
                                try {
                                    nativeGetConfirmText()
                                } catch (_: UnsatisfiedLinkError) {
                                    null
                                }
                            } else null

                            hudState = HudState(
                                gameMode = raw[0].toInt(),
                                isPaused = raw[1] != 0L,
                                isFastForward = raw[2] != 0L,
                                money = raw[3],
                                year = raw[4].toInt(),
                                month = raw[5].toInt(),
                                day = raw[6].toInt(),
                                activeTool = raw[7] != 0L,
                                hasDialogOpen = raw[8] != 0L,
                                surveyAskPending = raw[9] != 0L,
                                confirmPending = confirmPending,
                                confirmCaption = confirmTexts?.getOrNull(0) ?: "Confirm Construction",
                                confirmMessage = confirmTexts?.getOrNull(1) ?: ""
                            )
                        }
                        delay(150)
                    }
                }

                OpenTTDHudOverlay(
                    state = hudState,
                    onAction = { actionId ->
                        try {
                            nativeDispatchAction(actionId)
                        } catch (e: UnsatisfiedLinkError) {
                            Log.w(TAG, "JNI action $actionId ignored before native init", e)
                        }
                    },
                    onStartNewGame = { climate, mapLog2, terrain, towns, industries ->
                        try {
                            nativeStartNewGame(climate, mapLog2, terrain, towns, industries)
                        } catch (e: UnsatisfiedLinkError) {
                            Log.w(TAG, "JNI startNewGame ignored before native init", e)
                        }
                    },
                    onSurveyChoice = { allow ->
                        try {
                            nativeSetSurveyChoice(allow)
                        } catch (e: UnsatisfiedLinkError) {
                            Log.w(TAG, "JNI surveyChoice ignored before native init", e)
                        }
                    },
                    onSelectBuildTool = { category, widgetId ->
                        try {
                            nativeSelectBuildTool(category, widgetId)
                        } catch (e: UnsatisfiedLinkError) {
                            Log.w(TAG, "JNI selectBuildTool ignored before native init", e)
                        }
                    },
                    onResolveConfirm = { apply ->
                        try {
                            nativeResolveConfirm(apply)
                        } catch (e: UnsatisfiedLinkError) {
                            Log.w(TAG, "JNI resolveConfirm ignored before native init", e)
                        }
                    },
                    onGetSettings = {
                        try {
                            nativeGetSettings()
                        } catch (_: UnsatisfiedLinkError) {
                            null
                        }
                    },
                    onApplySettings = { ev, mv, gs, ss, sc, ai, sy ->
                        try {
                            nativeApplySettings(ev, mv, gs, ss, sc, ai, sy)
                        } catch (e: UnsatisfiedLinkError) {
                            Log.w(TAG, "JNI applySettings ignored before native init", e)
                        }
                    }
                )
            }
        }

        addContentView(
            composeView,
            ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
        )
    }

    private fun setupImmersiveDisplay() {
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val insetsController = WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        insetsController.hide(WindowInsetsCompat.Type.systemBars())

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes.layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
        }
    }

    private fun extractAssetSubdir(subdir: String, forceRefresh: Boolean) {
        try {
            val destinationDir = File(filesDir, subdir)
            if (!destinationDir.exists()) {
                destinationDir.mkdirs()
            } else if (forceRefresh && subdir == BASESET_ASSET_SUBDIR) {
                File(destinationDir, "opengfx-0.6.0.tar").takeIf { it.exists() }?.delete()
            }

            val assetNames = assets.list(subdir) ?: return
            for (assetName in assetNames) {
                val destFile = File(destinationDir, assetName)
                if (forceRefresh || !destFile.exists() || destFile.length() == 0L) {
                    Log.i(TAG, "Extracting asset: $subdir/$assetName -> ${destFile.absolutePath}")
                    assets.open("$subdir/$assetName").use { input ->
                        FileOutputStream(destFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error extracting assets from $subdir", e)
        }
    }
}
