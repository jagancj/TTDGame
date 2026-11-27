package org.openttd.openttd

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.util.Locale

data class HudState(
    val gameMode: Int = 0, // 0 = Menu, 1 = Normal, 2 = Editor
    val isPaused: Boolean = false,
    val isFastForward: Boolean = false,
    val money: Long = 0L,
    val year: Int = 1950,
    val month: Int = 1,
    val day: Int = 1,
    val activeTool: Boolean = false,
    val hasDialogOpen: Boolean = false,
    val surveyAskPending: Boolean = false,
    val confirmPending: Boolean = false,
    val confirmCaption: String = "",
    val confirmMessage: String = ""
)

object HudAction {
    const val NEW_GAME = 1
    const val LOAD_GAME = 2
    const val GAME_OPTIONS = 3
    const val ONLINE_CONTENT = 4

    const val TOGGLE_PAUSE = 10
    const val TOGGLE_FAST_FORWARD = 11
    const val SAVE_GAME = 12

    const val OPEN_RAIL = 20
    const val OPEN_ROAD = 21
    const val OPEN_TRAM = 22
    const val OPEN_DOCK = 23
    const val OPEN_AIRPORT = 24
    const val OPEN_TERRAFORM = 25

    const val OPEN_TRAINS = 30
    const val OPEN_ROAD_VEHS = 31
    const val OPEN_SHIPS = 32
    const val OPEN_AIRCRAFT = 33
    const val OPEN_STATIONS = 34
    const val OPEN_FINANCES = 35
    const val OPEN_TOWNS = 36
    const val OPEN_INDUSTRIES = 37
    const val OPEN_MAP = 38

    const val ZOOM_IN = 40
    const val ZOOM_OUT = 41
    const val CANCEL_TOOL = 42
    const val CLOSE_TOP_WINDOW = 43
}

private val GlassBg = Color(0xCC0F172A)
private val GlassBgElevated = Color(0xE61E293B)
private val GlassBorder = Color(0x33FFFFFF)
private val AccentEmerald = Color(0xFF10B981)
private val AccentSky = Color(0xFF38BDF8)
private val AccentAmber = Color(0xFFF59E0B)
private val AccentCoral = Color(0xFFEF4444)
private val TextPrimary = Color(0xFFF8FAFC)
private val TextMuted = Color(0xFF94A3B8)

private enum class DockCategory(val label: String, val icon: ImageVector) {
    BUILD("Build", Icons.Rounded.Construction),
    FLEET("Fleet", Icons.Rounded.DirectionsTransit),
    WORLD("World", Icons.Rounded.Public)
}

private enum class BuildBarType(val categoryId: Int, val label: String, val accent: Color) {
    RAIL(0, "Rail", AccentSky),
    ROAD(1, "Road", AccentAmber),
    TRAM(2, "Tram", AccentAmber),
    DOCK(3, "Dock", AccentSky),
    AIRPORT(4, "Airport", AccentSky),
    TERRAFORM(5, "Terrain", AccentEmerald)
}

private data class BuildToolItem(
    val label: String,
    val icon: ImageVector,
    val widgetId: Int,
    val tint: Color = TextPrimary
)

@Composable
fun OpenTTDHudOverlay(
    state: HudState,
    onAction: (Int) -> Unit,
    onStartNewGame: (climate: Int, mapLog2: Int, terrain: Int, towns: Int, industries: Int) -> Unit,
    onSurveyChoice: (Boolean) -> Unit,
    onSelectBuildTool: (category: Int, widgetId: Int) -> Unit,
    onResolveConfirm: (Boolean) -> Unit,
    onGetSettings: () -> IntArray?,
    onApplySettings: (Int, Int, Int, Boolean, Boolean, Int, Boolean) -> Unit
) {
    var showWorldGenSheet by remember { mutableStateOf(false) }
    var showSettingsSheet by remember { mutableStateOf(false) }
    var activeBuildBar by remember { mutableStateOf<BuildBarType?>(null) }

    LaunchedEffect(state.gameMode) {
        if (state.gameMode != 0) showWorldGenSheet = false
        if (state.gameMode == 0) activeBuildBar = null
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        if (state.gameMode == 0) {
            AnimatedVisibility(
                visible = !state.hasDialogOpen && !showWorldGenSheet && !showSettingsSheet,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.CenterStart)
            ) {
                ModernMainMenu(
                    onNewGame = { showWorldGenSheet = true },
                    onOpenSettings = { showSettingsSheet = true },
                    onAction = onAction
                )
            }

            AnimatedVisibility(
                visible = showWorldGenSheet && !state.hasDialogOpen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                ModernWorldGenSheet(
                    onGenerate = { climate, mapLog2, terrain, towns, industries ->
                        showWorldGenSheet = false
                        onStartNewGame(climate, mapLog2, terrain, towns, industries)
                    },
                    onAdvanced = {
                        showWorldGenSheet = false
                        onAction(HudAction.NEW_GAME)
                    },
                    onDismiss = { showWorldGenSheet = false }
                )
            }

            AnimatedVisibility(
                visible = state.surveyAskPending && !showWorldGenSheet && !state.hasDialogOpen,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(12.dp)
            ) {
                ModernSurveyCard(onChoice = onSurveyChoice)
            }
        } else {
            InGameTopBar(
                state = state,
                onOpenSettings = { showSettingsSheet = true },
                onAction = onAction,
                modifier = Modifier.align(Alignment.TopCenter)
            )

            InGameRightControls(
                state = state,
                onAction = onAction,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
            )

            AnimatedVisibility(
                visible = state.confirmPending,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 54.dp)
            ) {
                ModernConstructionConfirmBar(
                    caption = state.confirmCaption,
                    message = state.confirmMessage,
                    onCancel = { onResolveConfirm(false) },
                    onApply = { onResolveConfirm(true) }
                )
            }

            AnimatedVisibility(
                visible = !state.hasDialogOpen,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                InGameBottomDock(
                    activeBuildBar = activeBuildBar,
                    onSelectBuildBar = { bar ->
                        activeBuildBar = if (activeBuildBar == bar) {
                            onAction(HudAction.CANCEL_TOOL)
                            null
                        } else {
                            bar
                        }
                    },
                    onSelectBuildTool = { cat, wid -> onSelectBuildTool(cat, wid) },
                    onCloseBuildBar = {
                        activeBuildBar = null
                        onAction(HudAction.CANCEL_TOOL)
                    },
                    onAction = onAction
                )
            }
        }

        AnimatedVisibility(
            visible = showSettingsSheet && !state.hasDialogOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            ModernSettingsSheet(
                initial = onGetSettings(),
                onApply = { ev, mv, gs, ss, sc, ai, sy ->
                    onApplySettings(ev, mv, gs, ss, sc, ai, sy)
                    showSettingsSheet = false
                },
                onExpertSettings = {
                    showSettingsSheet = false
                    onAction(HudAction.GAME_OPTIONS)
                },
                onDismiss = { showSettingsSheet = false }
            )
        }

        AnimatedVisibility(
            visible = state.hasDialogOpen,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopEnd)
        ) {
            Surface(
                onClick = { onAction(HudAction.CLOSE_TOP_WINDOW) },
                shape = CircleShape,
                color = GlassBgElevated,
                border = BorderStroke(1.dp, AccentCoral.copy(alpha = 0.6f)),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = "Close", tint = AccentCoral, modifier = Modifier.size(18.dp))
                    Text("Close", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun ModernMainMenu(
    onNewGame: () -> Unit,
    onOpenSettings: () -> Unit,
    onAction: (Int) -> Unit
) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = GlassBg,
        border = BorderStroke(1.dp, GlassBorder),
        shadowElevation = 16.dp,
        modifier = Modifier
            .widthIn(min = 280.dp, max = 330.dp)
            .padding(start = 16.dp)
    ) {
        Column(
            modifier = Modifier.padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "OPENTTD",
                    color = TextPrimary,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp
                )
                Text(
                    text = "TRANSPORT TYCOON • MOBILE",
                    color = AccentSky,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onNewGame,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                contentPadding = PaddingValues(vertical = 14.dp, horizontal = 18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = Color(0xFF022C22))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "New Game",
                    color = Color(0xFF022C22),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            MenuSecondaryButton("Load Game", Icons.Rounded.FolderOpen) { onAction(HudAction.LOAD_GAME) }
            MenuSecondaryButton("Online Content", Icons.Rounded.CloudDownload) { onAction(HudAction.ONLINE_CONTENT) }
            MenuSecondaryButton("Game Options", Icons.Rounded.Tune, onOpenSettings)
        }
    }
}

@Composable
private fun MenuSecondaryButton(label: String, icon: ImageVector, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = Color(0x22FFFFFF),
        border = BorderStroke(1.dp, Color(0x22FFFFFF)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = AccentSky, modifier = Modifier.size(20.dp))
            Text(label, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InGameTopBar(
    state: HudState,
    onOpenSettings: () -> Unit,
    onAction: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val months = remember { arrayOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec") }
    val monthName = months.getOrElse(state.month - 1) { "Jan" }
    val moneyText = remember(state.money) {
        val nf = NumberFormat.getIntegerInstance(Locale.US)
        "£${nf.format(state.money)}"
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left: Date & Speed Controls
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = GlassBg,
            border = BorderStroke(1.dp, GlassBorder),
            shadowElevation = 8.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "${state.day} $monthName ${state.year}",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )

                HudIconPill(
                    icon = if (state.isPaused) Icons.Rounded.PlayArrow else Icons.Rounded.Pause,
                    active = state.isPaused,
                    activeColor = AccentAmber,
                    onClick = { onAction(HudAction.TOGGLE_PAUSE) }
                )

                HudIconPill(
                    icon = Icons.Rounded.FastForward,
                    active = state.isFastForward,
                    activeColor = AccentSky,
                    onClick = { onAction(HudAction.TOGGLE_FAST_FORWARD) }
                )
            }
        }

        // Right: Cash & Quick Menu (when no dialog is covering top-right)
        if (!state.hasDialogOpen) {
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = GlassBg,
                border = BorderStroke(1.dp, GlassBorder),
                shadowElevation = 8.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onAction(HudAction.OPEN_FINANCES) }
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(Icons.Rounded.AccountBalanceWallet, contentDescription = null, tint = AccentEmerald, modifier = Modifier.size(16.dp))
                        Text(moneyText, color = AccentEmerald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }

                    HudIconPill(icon = Icons.Rounded.Save, active = false, activeColor = AccentSky) {
                        onAction(HudAction.SAVE_GAME)
                    }
                    HudIconPill(icon = Icons.Rounded.Settings, active = false, activeColor = AccentSky, onClick = onOpenSettings)
                }
            }
        }
    }
}

@Composable
private fun HudIconPill(
    icon: ImageVector,
    active: Boolean,
    activeColor: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (active) activeColor.copy(alpha = 0.25f) else Color(0x1AFFFFFF),
        border = if (active) BorderStroke(1.dp, activeColor) else null
    ) {
        Box(
            modifier = Modifier.size(34.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) activeColor else TextPrimary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
private fun InGameRightControls(
    state: HudState,
    onAction: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (state.activeTool) {
            Surface(
                onClick = { onAction(HudAction.CANCEL_TOOL) },
                shape = CircleShape,
                color = GlassBgElevated,
                border = BorderStroke(1.dp, AccentCoral),
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(Icons.Rounded.Close, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(16.dp))
                    Text("Cancel", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = GlassBg,
            border = BorderStroke(1.dp, GlassBorder),
            shadowElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(4.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                HudIconPill(icon = Icons.Rounded.Add, active = false, activeColor = AccentSky) {
                    onAction(HudAction.ZOOM_IN)
                }
                HudIconPill(icon = Icons.Rounded.Remove, active = false, activeColor = AccentSky) {
                    onAction(HudAction.ZOOM_OUT)
                }
            }
        }
    }
}

@Composable
private fun InGameBottomDock(
    activeBuildBar: BuildBarType?,
    onSelectBuildBar: (BuildBarType) -> Unit,
    onSelectBuildTool: (category: Int, widgetId: Int) -> Unit,
    onCloseBuildBar: () -> Unit,
    onAction: (Int) -> Unit
) {
    var selectedCategory by remember { mutableStateOf(DockCategory.BUILD) }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        // Modern Construction Sub-Toolbar when Rail / Road / Tram / Dock / Airport / Terrain is open
        AnimatedVisibility(
            visible = activeBuildBar != null,
            enter = slideInVertically { it / 2 } + fadeIn(),
            exit = slideOutVertically { it / 2 } + fadeOut()
        ) {
            activeBuildBar?.let { bar ->
                ConstructionSubToolbar(
                    barType = bar,
                    onSelectTool = { widgetId -> onSelectBuildTool(bar.categoryId, widgetId) },
                    onClose = onCloseBuildBar
                )
            }
        }

        // Sub-action bar
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = GlassBg,
            border = BorderStroke(1.dp, GlassBorder),
            shadowElevation = 10.dp
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                when (selectedCategory) {
                    DockCategory.BUILD -> {
                        DockActionChip("Rail", Icons.Rounded.Train, AccentSky, activeBuildBar == BuildBarType.RAIL) {
                            onSelectBuildBar(BuildBarType.RAIL)
                        }
                        DockActionChip("Road", Icons.Rounded.AddRoad, AccentAmber, activeBuildBar == BuildBarType.ROAD) {
                            onSelectBuildBar(BuildBarType.ROAD)
                        }
                        DockActionChip("Tram", Icons.Rounded.Tram, AccentAmber, activeBuildBar == BuildBarType.TRAM) {
                            onSelectBuildBar(BuildBarType.TRAM)
                        }
                        DockActionChip("Dock", Icons.Rounded.DirectionsBoat, AccentSky, activeBuildBar == BuildBarType.DOCK) {
                            onSelectBuildBar(BuildBarType.DOCK)
                        }
                        DockActionChip("Airport", Icons.Rounded.Flight, AccentSky, activeBuildBar == BuildBarType.AIRPORT) {
                            onSelectBuildBar(BuildBarType.AIRPORT)
                        }
                        DockActionChip("Terrain", Icons.Rounded.Landscape, AccentEmerald, activeBuildBar == BuildBarType.TERRAFORM) {
                            onSelectBuildBar(BuildBarType.TERRAFORM)
                        }
                    }
                    DockCategory.FLEET -> {
                        DockActionChip("Trains", Icons.Rounded.Train, AccentSky) { onAction(HudAction.OPEN_TRAINS) }
                        DockActionChip("Road Veh", Icons.Rounded.LocalShipping, AccentAmber) { onAction(HudAction.OPEN_ROAD_VEHS) }
                        DockActionChip("Ships", Icons.Rounded.DirectionsBoat, AccentSky) { onAction(HudAction.OPEN_SHIPS) }
                        DockActionChip("Planes", Icons.Rounded.Flight, AccentSky) { onAction(HudAction.OPEN_AIRCRAFT) }
                        DockActionChip("Stations", Icons.Rounded.Apartment, AccentEmerald) { onAction(HudAction.OPEN_STATIONS) }
                    }
                    DockCategory.WORLD -> {
                        DockActionChip("Map", Icons.Rounded.Map, AccentEmerald) { onAction(HudAction.OPEN_MAP) }
                        DockActionChip("Towns", Icons.Rounded.LocationCity, AccentSky) { onAction(HudAction.OPEN_TOWNS) }
                        DockActionChip("Industries", Icons.Rounded.Factory, AccentAmber) { onAction(HudAction.OPEN_INDUSTRIES) }
                        DockActionChip("Finances", Icons.Rounded.AccountBalance, AccentEmerald) { onAction(HudAction.OPEN_FINANCES) }
                    }
                }
            }
        }

        // Category switcher pills
        Surface(
            shape = CircleShape,
            color = GlassBgElevated,
            border = BorderStroke(1.dp, GlassBorder),
            shadowElevation = 6.dp
        ) {
            Row(
                modifier = Modifier.padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                DockCategory.entries.forEach { cat ->
                    val selected = cat == selectedCategory
                    Surface(
                        onClick = { selectedCategory = cat },
                        shape = CircleShape,
                        color = if (selected) AccentSky.copy(alpha = 0.22f) else Color.Transparent,
                        border = if (selected) BorderStroke(1.dp, AccentSky.copy(alpha = 0.6f)) else null
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = cat.icon,
                                contentDescription = null,
                                tint = if (selected) AccentSky else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = cat.label,
                                color = if (selected) TextPrimary else TextMuted,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DockActionChip(
    label: String,
    icon: ImageVector,
    tint: Color,
    selected: Boolean = false,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) tint.copy(alpha = 0.25f) else Color(0x1EFFFFFF),
        border = BorderStroke(1.dp, if (selected) tint else Color(0x1EFFFFFF))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
            Text(label, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ConstructionSubToolbar(
    barType: BuildBarType,
    onSelectTool: (Int) -> Unit,
    onClose: () -> Unit
) {
    var activeWidget by remember(barType) { mutableIntStateOf(-1) }
    var removeMode by remember(barType) { mutableStateOf(false) }

    val tools = remember(barType) {
        when (barType) {
            BuildBarType.RAIL -> listOf(
                BuildToolItem("Autorail", Icons.Rounded.Timeline, 5, AccentSky),         // WID_RAT_AUTORAIL
                BuildToolItem("Station", Icons.Rounded.Train, 9, AccentEmerald),        // WID_RAT_BUILD_STATION
                BuildToolItem("Depot", Icons.Rounded.Warehouse, 7, AccentSky),          // WID_RAT_BUILD_DEPOT
                BuildToolItem("Signals", Icons.Rounded.Traffic, 10, AccentAmber),       // WID_RAT_BUILD_SIGNALS
                BuildToolItem("Bridge", Icons.Rounded.Architecture, 11, AccentSky),     // WID_RAT_BUILD_BRIDGE
                BuildToolItem("Tunnel", Icons.Rounded.Subway, 12, AccentSky),           // WID_RAT_BUILD_TUNNEL
                BuildToolItem("Waypoint", Icons.Rounded.Flag, 8, AccentSky),            // WID_RAT_BUILD_WAYPOINT
                BuildToolItem("Remove", Icons.Rounded.LayersClear, 13, AccentAmber),    // WID_RAT_REMOVE
                BuildToolItem("Demolish", Icons.Rounded.DeleteForever, 6, AccentCoral)  // WID_RAT_DEMOLISH
            )
            BuildBarType.ROAD, BuildBarType.TRAM -> listOf(
                BuildToolItem("Autoroad", Icons.Rounded.AddRoad, 3, AccentAmber),       // WID_ROT_AUTOROAD
                BuildToolItem("Bus Stop", Icons.Rounded.DirectionsBus, 7, AccentEmerald),// WID_ROT_BUS_STATION
                BuildToolItem("Truck Bay", Icons.Rounded.LocalShipping, 8, AccentEmerald),// WID_ROT_TRUCK_STATION
                BuildToolItem("Depot", Icons.Rounded.Warehouse, 5, AccentAmber),        // WID_ROT_DEPOT
                BuildToolItem("Bridge", Icons.Rounded.Architecture, 10, AccentSky),     // WID_ROT_BUILD_BRIDGE
                BuildToolItem("Tunnel", Icons.Rounded.Subway, 11, AccentSky),           // WID_ROT_BUILD_TUNNEL
                BuildToolItem("One-Way", Icons.Rounded.ArrowForward, 9, AccentAmber),   // WID_ROT_ONE_WAY
                BuildToolItem("Remove", Icons.Rounded.LayersClear, 12, AccentAmber),    // WID_ROT_REMOVE
                BuildToolItem("Demolish", Icons.Rounded.DeleteForever, 4, AccentCoral)  // WID_ROT_DEMOLISH
            )
            BuildBarType.DOCK -> listOf(
                BuildToolItem("Dock", Icons.Rounded.DirectionsBoat, 4, AccentEmerald),  // WID_DT_STATION
                BuildToolItem("Depot", Icons.Rounded.Warehouse, 3, AccentSky),          // WID_DT_DEPOT
                BuildToolItem("Canal", Icons.Rounded.Water, 0, AccentSky),              // WID_DT_CANAL
                BuildToolItem("Lock", Icons.Rounded.WaterfallChart, 1, AccentSky),      // WID_DT_LOCK
                BuildToolItem("Buoy", Icons.Rounded.Anchor, 5, AccentSky),              // WID_DT_BUOY
                BuildToolItem("Aqueduct", Icons.Rounded.Architecture, 7, AccentSky),    // WID_DT_BUILD_AQUEDUCT
                BuildToolItem("Demolish", Icons.Rounded.DeleteForever, 2, AccentCoral)  // WID_DT_DEMOLISH
            )
            BuildBarType.AIRPORT -> listOf(
                BuildToolItem("Airport", Icons.Rounded.FlightTakeoff, 0, AccentEmerald),// WID_AT_AIRPORT
                BuildToolItem("Demolish", Icons.Rounded.DeleteForever, 1, AccentCoral)  // WID_AT_DEMOLISH
            )
            BuildBarType.TERRAFORM -> listOf(
                BuildToolItem("Lower", Icons.Rounded.ArrowDownward, 1, AccentEmerald),  // WID_TT_LOWER_LAND
                BuildToolItem("Raise", Icons.Rounded.ArrowUpward, 2, AccentEmerald),    // WID_TT_RAISE_LAND
                BuildToolItem("Level", Icons.Rounded.DragHandle, 3, AccentEmerald),     // WID_TT_LEVEL_LAND
                BuildToolItem("Trees", Icons.Rounded.Park, 6, AccentEmerald),           // WID_TT_PLANT_TREES
                BuildToolItem("Sign", Icons.Rounded.EditLocationAlt, 7, AccentSky),     // WID_TT_PLACE_SIGN
                BuildToolItem("Buy Land", Icons.Rounded.Sell, 5, AccentAmber),          // WID_TT_BUY_LAND
                BuildToolItem("Demolish", Icons.Rounded.DeleteForever, 4, AccentCoral)  // WID_TT_DEMOLISH
            )
        }
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = GlassBgElevated,
        border = BorderStroke(1.dp, barType.accent.copy(alpha = 0.5f)),
        shadowElevation = 12.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = barType.label.uppercase(),
                color = barType.accent,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 0.8.sp,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            tools.forEach { tool ->
                val isRemoveBtn = (barType == BuildBarType.RAIL && tool.widgetId == 13) ||
                    ((barType == BuildBarType.ROAD || barType == BuildBarType.TRAM) && tool.widgetId == 12)
                val selected = if (isRemoveBtn) removeMode else activeWidget == tool.widgetId

                Surface(
                    onClick = {
                        if (isRemoveBtn) {
                            removeMode = !removeMode
                        } else {
                            activeWidget = tool.widgetId
                        }
                        onSelectTool(tool.widgetId)
                    },
                    shape = RoundedCornerShape(12.dp),
                    color = if (selected) tool.tint.copy(alpha = 0.28f) else Color(0x16FFFFFF),
                    border = BorderStroke(1.dp, if (selected) tool.tint else Color(0x18FFFFFF))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(tool.icon, contentDescription = null, tint = tool.tint, modifier = Modifier.size(16.dp))
                        Text(tool.label, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }

            HudIconPill(icon = Icons.Rounded.Close, active = false, activeColor = AccentCoral, onClick = onClose)
        }
    }
}

@Composable
private fun ModernConstructionConfirmBar(
    caption: String,
    message: String,
    onCancel: () -> Unit,
    onApply: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = GlassBgElevated,
        border = BorderStroke(1.dp, AccentEmerald.copy(alpha = 0.6f)),
        shadowElevation = 14.dp
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(modifier = Modifier.widthIn(max = 260.dp)) {
                Text(caption, color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                if (message.isNotBlank()) {
                    Text(message, color = TextMuted, fontSize = 11.sp)
                }
            }

            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AccentCoral.copy(alpha = 0.7f)),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.Close, contentDescription = null, tint = AccentCoral, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Cancel", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            }

            Button(
                onClick = onApply,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Icon(Icons.Rounded.Check, contentDescription = null, tint = Color(0xFF022C22), modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Build", color = Color(0xFF022C22), fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ModernSettingsSheet(
    initial: IntArray?,
    onApply: (Int, Int, Int, Boolean, Boolean, Int, Boolean) -> Unit,
    onExpertSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    var effectVol by remember { mutableIntStateOf(initial?.getOrNull(0) ?: 100) }
    val musicVol = remember { initial?.getOrNull(1) ?: 100 }
    var guiScale by remember { mutableIntStateOf(initial?.getOrNull(2) ?: -1) }
    var smoothScroll by remember { mutableStateOf((initial?.getOrNull(3) ?: 1) != 0) }
    var showCoverage by remember { mutableStateOf((initial?.getOrNull(4) ?: 1) != 0) }
    var autosaveInterval by remember { mutableIntStateOf(initial?.getOrNull(5) ?: 3) }
    var surveyYes by remember { mutableStateOf((initial?.getOrNull(6) ?: 0) != 0) }

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = GlassBgElevated,
        border = BorderStroke(1.dp, GlassBorder),
        shadowElevation = 20.dp,
        modifier = Modifier
            .widthIn(min = 440.dp, max = 580.dp)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("GAME OPTIONS", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("Display, controls & audio", color = TextMuted, fontSize = 12.sp)
                }
                HudIconPill(icon = Icons.Rounded.Close, active = false, activeColor = AccentCoral, onClick = onDismiss)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Sound", color = TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(72.dp))
                Slider(
                    value = effectVol.toFloat(),
                    onValueChange = { effectVol = it.toInt() },
                    valueRange = 0f..127f,
                    colors = SliderDefaults.colors(thumbColor = AccentSky, activeTrackColor = AccentSky),
                    modifier = Modifier.weight(1f)
                )
                Text("${(effectVol * 100) / 127}%", color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold, modifier = Modifier.width(40.dp))
            }

            OptionRow(
                "UI Scale",
                listOf("Auto" to -1, "250%" to 250, "300%" to 300, "350%" to 350, "400%" to 400),
                guiScale
            ) { guiScale = it }

            OptionRow(
                "Auto-Save",
                listOf("Off" to 0, "1 mo" to 1, "3 mo" to 3, "12 mo" to 12),
                autosaveInterval
            ) { autosaveInterval = it }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsToggleChip("Smooth Pan", smoothScroll, Modifier.weight(1f)) { smoothScroll = it }
                SettingsToggleChip("Coverage", showCoverage, Modifier.weight(1f)) { showCoverage = it }
                SettingsToggleChip("Telemetry", surveyYes, Modifier.weight(1f)) { surveyYes = it }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onExpertSettings) {
                    Text("Expert Settings...", color = AccentSky, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onApply(effectVol, musicVol, guiScale, smoothScroll, showCoverage, autosaveInterval, surveyYes) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 11.dp)
                ) {
                    Text("Apply", color = Color(0xFF022C22), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SettingsToggleChip(
    label: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onToggle: (Boolean) -> Unit
) {
    Surface(
        onClick = { onToggle(!checked) },
        shape = RoundedCornerShape(12.dp),
        color = if (checked) AccentEmerald.copy(alpha = 0.22f) else Color(0x16FFFFFF),
        border = BorderStroke(1.dp, if (checked) AccentEmerald else Color(0x22FFFFFF)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, color = if (checked) TextPrimary else TextMuted, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Icon(
                imageVector = if (checked) Icons.Rounded.CheckCircle else Icons.Rounded.RadioButtonUnchecked,
                contentDescription = null,
                tint = if (checked) AccentEmerald else TextMuted,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun ModernWorldGenSheet(
    onGenerate: (climate: Int, mapLog2: Int, terrain: Int, towns: Int, industries: Int) -> Unit,
    onAdvanced: () -> Unit,
    onDismiss: () -> Unit
) {
    var climate by remember { mutableIntStateOf(0) }
    var mapLog2 by remember { mutableIntStateOf(8) } // 256x256
    var terrain by remember { mutableIntStateOf(2) } // Hilly
    var towns by remember { mutableIntStateOf(2) }   // Normal
    var industries by remember { mutableIntStateOf(4) } // Normal

    Surface(
        shape = RoundedCornerShape(24.dp),
        color = GlassBgElevated,
        border = BorderStroke(1.dp, GlassBorder),
        shadowElevation = 20.dp,
        modifier = Modifier
            .widthIn(min = 420.dp, max = 560.dp)
            .padding(12.dp)
    ) {
        Column(
            modifier = Modifier
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("NEW WORLD", color = TextPrimary, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
                    Text("Configure your transport empire", color = TextMuted, fontSize = 12.sp)
                }
                HudIconPill(icon = Icons.Rounded.Close, active = false, activeColor = AccentCoral, onClick = onDismiss)
            }

            OptionRow("Climate", listOf("Temperate" to 0, "Arctic" to 1, "Tropical" to 2, "Toyland" to 3), climate) { climate = it }
            OptionRow("Map Size", listOf("256×256" to 8, "512×512" to 9, "1024×1024" to 10), mapLog2) { mapLog2 = it }
            OptionRow("Terrain", listOf("Flat" to 1, "Hilly" to 2, "Mountains" to 3), terrain) { terrain = it }
            OptionRow("Density", listOf("Low" to 1, "Normal" to 2, "High" to 3), towns) {
                towns = it
                industries = when (it) { 1 -> 3; 3 -> 5; else -> 4 }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAdvanced) {
                    Text("Advanced Setup", color = AccentSky, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { onGenerate(climate, mapLog2, terrain, towns, industries) },
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentEmerald),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp)
                ) {
                    Icon(Icons.Rounded.Public, contentDescription = null, tint = Color(0xFF022C22), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Generate World", color = Color(0xFF022C22), fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun OptionRow(
    title: String,
    options: List<Pair<String, Int>>,
    selected: Int,
    onSelect: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Text(
            text = title,
            color = TextMuted,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.width(72.dp)
        )
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            options.forEach { (label, value) ->
                val isSelected = value == selected
                Surface(
                    onClick = { onSelect(value) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) AccentSky.copy(alpha = 0.25f) else Color(0x1AFFFFFF),
                    border = BorderStroke(1.dp, if (isSelected) AccentSky else Color(0x22FFFFFF))
                ) {
                    Text(
                        text = label,
                        color = if (isSelected) TextPrimary else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ModernSurveyCard(onChoice: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = GlassBgElevated,
        border = BorderStroke(1.dp, GlassBorder),
        shadowElevation = 12.dp,
        modifier = Modifier.widthIn(max = 320.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Anonymous Telemetry", color = TextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Text(
                "Share anonymous crash & hardware stats to help improve OpenTTD?",
                color = TextMuted,
                fontSize = 11.sp,
                lineHeight = 15.sp
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = { onChoice(false) },
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, GlassBorder),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("No Thanks", color = TextMuted, fontSize = 12.sp)
                }
                Button(
                    onClick = { onChoice(true) },
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentSky),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Allow", color = Color(0xFF082F49), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
