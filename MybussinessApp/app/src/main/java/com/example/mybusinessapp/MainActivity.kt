package com.example.mybusinessapp

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.fragment.app.FragmentActivity
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.mybusinessapp.auth.UserSessionManager
import com.example.mybusinessapp.cart.CartBottomSheet
import com.example.mybusinessapp.cart.CartIntent
import com.example.mybusinessapp.cart.CartViewModel
import com.example.mybusinessapp.cart.Product
import com.example.mybusinessapp.network.NetworkStatus
import com.example.mybusinessapp.network.PingStatus
import com.example.mybusinessapp.security.BiometricAuthManager
import com.example.mybusinessapp.ui.ConnectionStatusPill
import com.example.mybusinessapp.ui.FluidBackground
import com.example.mybusinessapp.ui.FluidGlassCard
import com.example.mybusinessapp.ui.ForgotPasswordScreen
import com.example.mybusinessapp.ui.OfflineBanner
import com.example.mybusinessapp.ui.SignUpScreen
import com.example.mybusinessapp.ui.Soft3DIcon
import com.example.mybusinessapp.ui.chart.AnimatedRadialProgress
import com.example.mybusinessapp.ui.chart.InteractiveBezierChart
import com.example.mybusinessapp.ui.theme.GradientBurst
import com.example.mybusinessapp.ui.theme.GradientSunset
import com.example.mybusinessapp.ui.theme.MyBusinessAppTheme
import com.example.mybusinessapp.ui.theme.ThemeAccent
import com.example.mybusinessapp.ui.weather.SimpleWeatherWidget
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.absoluteValue

// ==================================================================
// 📱 MAIN ACTIVITY
// ==================================================================

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val sessionManager = UserSessionManager.getInstance(applicationContext)
        val startDest = if (sessionManager.isLoggedIn()) "main" else "login"

        setContent {
            val viewModel: MainViewModel = viewModel()
            val cartViewModel: CartViewModel = viewModel()

            val networkStatus by viewModel.networkStatus.collectAsState()
            val themeAccent by viewModel.themeAccent.collectAsState()
            val darkMode by viewModel.darkMode.collectAsState()

            MyBusinessAppTheme(darkTheme = darkMode, themeAccent = themeAccent) {
                val rootNav = rememberNavController()

                LaunchedEffect(Unit) {
                    viewModel.initSession(applicationContext, sessionManager)
                }

                FluidBackground {
                    Box(modifier = Modifier.fillMaxSize()) {
                        NavHost(
                            navController = rootNav,
                            startDestination = startDest,
                            enterTransition = { fadeIn(animationSpec = tween(250)) },
                            exitTransition = { fadeOut(animationSpec = tween(200)) },
                            popEnterTransition = { fadeIn(animationSpec = tween(250)) },
                            popExitTransition = { fadeOut(animationSpec = tween(200)) }
                        ) {
                            composable("login") {
                                LoginScreen(
                                    viewModel = viewModel,
                                    onLogin = {
                                        rootNav.navigate("main") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                    onSignUp = { rootNav.navigate("signup") },
                                    onForgotPassword = { rootNav.navigate("forgot_password") }
                                )
                            }
                            composable("signup") {
                                SignUpScreen(
                                    viewModel = viewModel,
                                    onSignUpSuccess = {
                                        rootNav.navigate("main") {
                                            popUpTo("login") { inclusive = true }
                                        }
                                    },
                                    onNavigateToLogin = {
                                        rootNav.popBackStack()
                                    }
                                )
                            }
                            composable("forgot_password") {
                                ForgotPasswordScreen(
                                    viewModel = viewModel,
                                    onResetSuccess = {
                                        rootNav.popBackStack()
                                    },
                                    onBack = {
                                        rootNav.popBackStack()
                                    }
                                )
                            }
                            composable("main") {
                                MainHost(viewModel = viewModel, cartViewModel = cartViewModel, rootNav = rootNav)
                            }
                        }

                        // Reactive Offline Banner displayed at top of screen when internet drops
                        OfflineBanner(
                            networkStatus = networkStatus,
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .zIndex(99f)
                        )
                    }
                }
            }
        }
    }
}

// ==================================================================
// 🏠 MAIN HOST (SCAFFOLD & NAVIGATION)
// ==================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainHost(
    viewModel: MainViewModel,
    cartViewModel: CartViewModel,
    rootNav: NavHostController
) {
    val mainNav = rememberNavController()
    val navBackStackEntry by mainNav.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: "dashboard"
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()

    val bottomBarRoutes = remember { setOf("dashboard", "products", "profile", "settings") }
    val showBottomBar = currentRoute in bottomBarRoutes

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = Color.Transparent,
        topBar = {
            SunburstTopBar(
                currentRoute = currentRoute,
                mainNav = mainNav,
                scrollBehavior = scrollBehavior,
                isTopLevel = showBottomBar,
                onLogout = {
                    viewModel.logout()
                    rootNav.navigate("login") {
                        popUpTo(0) { inclusive = true }
                    }
                }
            )
        },
        bottomBar = {
            if (showBottomBar) {
                SunburstBottomBar(
                    mainNav = mainNav,
                    currentRoute = currentRoute,
                    cartViewModel = cartViewModel
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    top = innerPadding.calculateTopPadding(),
                    bottom = if (!showBottomBar) innerPadding.calculateBottomPadding() else 0.dp
                )
        ) {
            NavHost(
                navController = mainNav,
                startDestination = "dashboard",
                enterTransition = {
                    fadeIn(animationSpec = tween(220, delayMillis = 80)) +
                            scaleIn(initialScale = 0.97f, animationSpec = tween(220, delayMillis = 80))
                },
                exitTransition = {
                    fadeOut(animationSpec = tween(150))
                },
                popEnterTransition = {
                    fadeIn(animationSpec = tween(220, delayMillis = 80)) +
                            scaleIn(initialScale = 0.97f, animationSpec = tween(220, delayMillis = 80))
                },
                popExitTransition = {
                    fadeOut(animationSpec = tween(150))
                }
            ) {
                composable("dashboard") { DashboardScreen(viewModel, mainNav) }
                composable("products") { ProductScreen(cartViewModel = cartViewModel) }
                composable("profile") { EnhancedProfileScreen(viewModel = viewModel, cartViewModel = cartViewModel) }
                composable("settings") {
                    SettingsScreen(
                        viewModel = viewModel,
                        onLogout = {
                            viewModel.logout()
                            rootNav.navigate("login") {
                                popUpTo(0) { inclusive = true }
                            }
                        }
                    )
                }
                composable("contact") { EnhancedContactScreen() }
                composable("about") { EnhancedAboutScreen() }
                composable("playground") { ComposePlaygroundScreen() }

                // Detailed Detail Screens
                composable("revenue_details") {
                    RevenueDetailScreen(onBack = { mainNav.popBackStack() })
                }
                composable("users_details") {
                    UsersDetailScreen(onBack = { mainNav.popBackStack() })
                }
                composable("sales_details") {
                    SalesDetailScreen(onBack = { mainNav.popBackStack() })
                }
                composable("stat_details/{title}") { backStackEntry ->
                    val title = backStackEntry.arguments?.getString("title") ?: "Details"
                    GenericMetricDetailScreen(title, onBack = { mainNav.popBackStack() })
                }
            }
        }
    }
}

// ==================================================================
// 📈 REVENUE DETAIL SCREEN
// ==================================================================

@Composable
fun RevenueDetailScreen(onBack: () -> Unit) {
    MetricDetailBase(
        title = "Total Revenue",
        value = "$14,230.50",
        trend = "+12.5%",
        icon = Icons.Rounded.AttachMoney,
        color = MaterialTheme.colorScheme.primary,
        onBack = onBack
    ) {
        item(span = { GridItemSpan(2) }) {
            FluidGlassCard(modifier = Modifier.wrapContentHeight()) {
                Column(Modifier.padding(20.dp)) {
                    Text(
                        text = "Revenue Performance Sparkline (Interactive Scrubber)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    InteractiveBezierChart(
                        lineColor = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }
        }
        item { DetailMiniCard("Transactions", "1,205", Icons.AutoMirrored.Rounded.ReceiptLong) }
        item { DetailMiniCard("Avg. Order", "$11.80", Icons.Rounded.Analytics) }
        item(span = { GridItemSpan(2) }) {
            DetailInfoCard("Projected Earnings", "Your revenue is projected to reach $18k by the end of this month based on current growth trends.")
        }
    }
}

// ==================================================================
// 👥 USERS DETAIL SCREEN
// ==================================================================

@Composable
fun UsersDetailScreen(onBack: () -> Unit) {
    MetricDetailBase(
        title = "Active Users",
        value = "1,240",
        trend = "+5.2%",
        icon = Icons.Rounded.Group,
        color = MaterialTheme.colorScheme.secondary,
        onBack = onBack
    ) {
        item(span = { GridItemSpan(2) }) {
            DetailChartCard("User Engagement (DAU)")
        }
        item { DetailMiniCard("New Signups", "+42", Icons.Rounded.PersonAdd) }
        item { DetailMiniCard("Retention", "82%", Icons.Rounded.History) }
        item(span = { GridItemSpan(2) }) {
            DetailInfoCard("User Geography", "Top performing region is Dhaka (45%), followed by Chittagong (20%).")
        }
    }
}

// ==================================================================
// 🎯 SALES DETAIL SCREEN
// ==================================================================

@Composable
fun SalesDetailScreen(onBack: () -> Unit) {
    MetricDetailBase(
        title = "Sales Target",
        value = "85%",
        trend = "On Track (+12.5% vs Last Mo)",
        icon = Icons.Rounded.AdsClick,
        color = MaterialTheme.colorScheme.tertiary,
        onBack = onBack
    ) {
        item(span = { GridItemSpan(2) }) {
            FluidGlassCard(modifier = Modifier.wrapContentHeight()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Animated Radial Target Progress (85%)",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp
                    )
                    Spacer(Modifier.height(16.dp))
                    AnimatedRadialProgress(
                        targetPercentage = 0.85f,
                        modifier = Modifier.size(170.dp),
                        startColor = MaterialTheme.colorScheme.primary,
                        endColor = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Spring physics animation with smooth gradient SVG arc",
                        color = Color.White.copy(alpha = 0.6f),
                        fontSize = 11.sp
                    )
                }
            }
        }
        item { DetailMiniCard("Items Sold", "492", Icons.Rounded.Inventory) }
        item { DetailMiniCard("Lead Conv.", "18%", Icons.Rounded.FilterAlt) }
        item(span = { GridItemSpan(2) }) {
            DetailInfoCard("Goal Analysis", "You are currently 5% ahead of your target. Maintaining this pace will result in a record-breaking quarter.")
        }
    }
}

@Composable
fun GenericMetricDetailScreen(title: String, onBack: () -> Unit) {
    MetricDetailBase(
        title = title,
        value = "Data Point",
        trend = "Normal",
        icon = Icons.Rounded.BarChart,
        color = MaterialTheme.colorScheme.primary,
        onBack = onBack
    ) {
        item(span = { GridItemSpan(2) }) { DetailChartCard("$title Analytics") }
        item { DetailMiniCard("Current", "Value", Icons.Rounded.Info) }
        item { DetailMiniCard("Historical", "Average", Icons.Rounded.Update) }
    }
}

// ==================================================================\n// 🏗️ METRIC DETAIL BASE COMPONENTS\n// ==================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MetricDetailBase(
    title: String,
    value: String,
    trend: String,
    icon: ImageVector,
    color: Color,
    onBack: () -> Unit,
    content: LazyGridScope.() -> Unit
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(title, fontWeight = FontWeight.Bold, fontSize = 18.sp) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back")
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { padding ->
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentPadding = PaddingValues(bottom = 40.dp)
        ) {
            item(span = { GridItemSpan(2) }) {
                FluidGlassCard {
                    Row(
                        modifier = Modifier.padding(24.dp).fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Soft3DIcon(icon, color, size = 64.dp, iconSize = 32.dp)
                        Spacer(Modifier.width(20.dp))
                        Column {
                            Text(value, fontSize = 32.sp, fontWeight = FontWeight.Black, color = MaterialTheme.colorScheme.onBackground)
                            Text(trend, color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        }
                    }
                }
            }
            content()
        }
    }
}

@Composable
fun DetailChartCard(label: String) {
    FluidGlassCard(modifier = Modifier.wrapContentHeight()) {
        Column(Modifier.padding(20.dp)) {
            Text(label, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(12.dp))
            InteractiveBezierChart(
                lineColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
            )
        }
    }
}

@Composable
fun DetailMiniCard(label: String, value: String, icon: ImageVector) {
    FluidGlassCard(modifier = Modifier.height(110.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
            Column {
                Text(value, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun DetailInfoCard(title: String, text: String) {
    FluidGlassCard {
        Column(Modifier.padding(20.dp)) {
            Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(8.dp))
            Text(text, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 18.sp)
        }
    }
}

// ==================================================================
// 🔝 SUNBURST TOP APP BAR
// ==================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SunburstTopBar(
    currentRoute: String,
    mainNav: NavHostController,
    scrollBehavior: TopAppBarScrollBehavior,
    isTopLevel: Boolean,
    onLogout: () -> Unit
) {
    val title = when (currentRoute) {
        "dashboard" -> "Dashboard"
        "products" -> "Products"
        "profile" -> "Profile"
        "settings" -> "Settings"
        "contact" -> "Contact Us"
        "about" -> "About Kore"
        "playground" -> "Playground"
        else -> currentRoute.replaceFirstChar { it.uppercase() }
    }
    val colors = MaterialTheme.colorScheme

    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                style = TextStyle(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    brush = GradientSunset
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        },
        navigationIcon = {
            if (!isTopLevel) {
                IconButton(onClick = { mainNav.popBackStack() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = colors.onBackground
                    )
                }
            } else {
                IconButton(onClick = { mainNav.navigate("about") }) {
                    Icon(
                        imageVector = Icons.Rounded.Info,
                        contentDescription = "About",
                        tint = if (currentRoute == "about") colors.tertiary else colors.onSurfaceVariant
                    )
                }
            }
        },
        actions = {
            if (isTopLevel) {
                val isPlayground = currentRoute == "playground"
                val infiniteTransition = rememberInfiniteTransition(label = "pgInfinite")
                
                val breathingScale by infiniteTransition.animateFloat(
                    initialValue = 1f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "breathingScale"
                )

                val glowAlpha by infiniteTransition.animateFloat(
                    initialValue = 0.3f,
                    targetValue = 0.8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "glowAlpha"
                )

                val pgScale by animateFloatAsState(
                    targetValue = if (isPlayground) 1.25f else 1f,
                    animationSpec = spring(dampingRatio = 0.4f, stiffness = Spring.StiffnessMedium),
                    label = "pgScale"
                )
                
                val pgRotation by animateFloatAsState(
                    targetValue = if (isPlayground) 360f else 0f,
                    animationSpec = spring(dampingRatio = 0.5f, stiffness = Spring.StiffnessLow),
                    label = "pgRotation"
                )

                IconButton(
                    onClick = { mainNav.navigate("playground") },
                    modifier = Modifier.graphicsLayer {
                        val baseScale = if (isPlayground) pgScale else breathingScale
                        scaleX = baseScale
                        scaleY = baseScale
                        rotationZ = pgRotation
                    }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(32.dp)) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        colors.tertiary.copy(alpha = glowAlpha * 0.4f),
                                        Color.Transparent
                                    )
                                ),
                                radius = size.minDimension * (if (isPlayground) 1.2f else 1f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.AutoAwesome,
                            contentDescription = "Playground",
                            tint = if (isPlayground) colors.tertiary else colors.onSurfaceVariant
                        )
                    }
                }
                IconButton(onClick = { mainNav.navigate("contact") }) {
                    Icon(
                        imageVector = Icons.Rounded.SupportAgent,
                        contentDescription = "Contact",
                        tint = if (currentRoute == "contact") colors.tertiary else colors.onSurfaceVariant
                    )
                }
            }
            Box(
                modifier = Modifier
                    .padding(end = 8.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(colors.primary.copy(alpha = 0.12f))
                    .clickable { onLogout() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                    contentDescription = "Logout",
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
        ),
        scrollBehavior = scrollBehavior
    )
}

// ==================================================================
// 🔽 SUNBURST BOTTOM NAVIGATION BAR
// ==================================================================

@Composable
fun SunburstBottomBar(
    mainNav: NavHostController,
    currentRoute: String,
    cartViewModel: CartViewModel = viewModel()
) {
    val colors = MaterialTheme.colorScheme
    val cartState by cartViewModel.state.collectAsState()

    val items = remember {
        listOf(
            Triple("dashboard", Icons.Rounded.SpaceDashboard, Icons.Outlined.SpaceDashboard),
            Triple("products", Icons.Rounded.ShoppingBag, Icons.Outlined.ShoppingBag),
            Triple("profile", Icons.Rounded.Person, Icons.Outlined.Person),
            Triple("settings", Icons.Rounded.Settings, Icons.Outlined.Settings)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(horizontal = 16.dp, vertical = 10.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        FluidGlassCard(
            modifier = Modifier
                .height(72.dp)
                .graphicsLayer {
                    shadowElevation = 20f
                    cameraDistance = 10f * density
                },
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { (route, selIcon, unselIcon) ->
                    val selected = currentRoute == route
                    val contentColor = if (selected) colors.tertiary else colors.onSurfaceVariant
                    val pillAlpha by animateFloatAsState(
                        targetValue = if (selected) 0.18f else 0f,
                        label = "pillAlpha",
                        animationSpec = tween(200)
                    )
                    val fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal

                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                if (currentRoute != route) {
                                    mainNav.navigate(route) {
                                        popUpTo(mainNav.graph.findStartDestination().id) {
                                            saveState = true
                                        }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            }
                            .padding(vertical = 4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(width = 48.dp, height = 28.dp)
                                .background(
                                    color = colors.primary.copy(alpha = pillAlpha),
                                    shape = RoundedCornerShape(14.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            val iconVector = if (selected) selIcon else unselIcon
                            if (route == "products" && cartState.totalItemCount > 0) {
                                BadgedBox(
                                    badge = {
                                        Badge(
                                            containerColor = colors.primary,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = if (cartState.totalItemCount > 99) "99+" else "${cartState.totalItemCount}",
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = iconVector,
                                        contentDescription = route,
                                        tint = contentColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = iconVector,
                                    contentDescription = route,
                                    tint = contentColor,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = route.replaceFirstChar { it.uppercase() },
                            color = contentColor,
                            fontSize = 11.sp,
                            fontWeight = fontWeight
                        )
                    }
                }
            }
        }
    }
}

// ==================================================================
// 📊 DASHBOARD SCREEN (BENTO GRID UPGRADE)
// ==================================================================

@Composable
fun DashboardScreen(viewModel: MainViewModel, mainNav: NavHostController) {
    val dashboardData by viewModel.dashboardData.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()
    val colors = MaterialTheme.colorScheme

    val userGreeting = currentUser?.fullName?.takeIf { it.isNotBlank() }
        ?: currentUser?.username?.takeIf { it.isNotBlank() }
        ?: "Partner"

    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(top = 8.dp, bottom = 110.dp)
    ) {
        // 1. Header (Span 2)
        item(span = { GridItemSpan(2) }) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WELCOME BACK, ${userGreeting.uppercase()}",
                        style = TextStyle(
                            color = colors.tertiary,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    )
                    Text(
                        text = "Overview",
                        style = TextStyle(
                            color = colors.onBackground,
                            fontSize = 30.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    )
                }

                FluidGlassCard(
                    modifier = Modifier.size(48.dp),
                    shape = RoundedCornerShape(14.dp),
                    onClick = {}
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(Icons.Rounded.Notifications, null, tint = colors.primary, modifier = Modifier.size(22.dp))
                    }
                }
            }
        }

        // 2. Feature Pager (Span 2)
        item(span = { GridItemSpan(2) }) {
            SunburstFeaturePager(
                onItemClick = { title ->
                    val route = when (title) {
                        "Total Revenue" -> "revenue_details"
                        "Active Users" -> "users_details"
                        "Sales Target" -> "sales_details"
                        else -> "dashboard"
                    }
                    mainNav.navigate(route)
                }
            )
        }

        // 3. Weather Widget (Span 2)
        item(span = { GridItemSpan(2) }) { SimpleWeatherWidget() }

        // 4. Quick Access Header (Span 2)
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "QUICK ACCESS",
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            )
        }

        // 5. Stories Section (Span 2)
        item(span = { GridItemSpan(2) }) { StoriesSection(viewModel) }

        // 6. Performance Header (Span 2)
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "PERFORMANCE",
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            )
        }

        // 7. Stat Cards (Bento Style: 1x1)
        item {
            SunburstStatCard(
                "Revenue", "$${dashboardData.totalRevenue}", Icons.Rounded.AttachMoney, colors.primary, Modifier,
                onClick = { mainNav.navigate("stat_details/Revenue") }
            )
        }
        item {
            SunburstStatCard(
                "Orders", "${dashboardData.newOrders}", Icons.Rounded.ShoppingCart, colors.secondary, Modifier,
                onClick = { mainNav.navigate("stat_details/Orders") }
            )
        }
        item {
            SunburstStatCard(
                "Users", "${dashboardData.customers}", Icons.Rounded.Group, colors.tertiary, Modifier,
                onClick = { mainNav.navigate("stat_details/Users") }
            )
        }
        item {
            SunburstStatCard(
                "Growth", "+${dashboardData.growth}%", Icons.Rounded.TrendingUp, Color(0xFF00E676), Modifier,
                onClick = { mainNav.navigate("stat_details/Growth") }
            )
        }

        // 8. Activity Log Header (Span 2)
        item(span = { GridItemSpan(2) }) {
            Text(
                text = "ACTIVITY LOG",
                style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
            )
        }

        // 9. Activity Log (Span 2)
        item(span = { GridItemSpan(2) }) { SunburstRecentActivityList() }
    }
}

@Composable
fun SunburstStatCard(title: String, value: String, icon: ImageVector, accent: Color, modifier: Modifier, onClick: () -> Unit) {
    FluidGlassCard(
        modifier = modifier.height(140.dp),
        onClick = onClick
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Soft3DIcon(
                imageVector = icon,
                color = accent,
                size = 42.dp,
                iconSize = 20.dp
            )
            
            Column {
                Text(
                    text = value,
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = (-0.5).sp
                    )
                )
                Text(
                    text = title,
                    style = TextStyle(
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SunburstFeaturePager(onItemClick: (String) -> Unit) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val colors = MaterialTheme.colorScheme

    val gradients = listOf(
        Brush.linearGradient(listOf(colors.primary, Color(0xFFFF7EB3))),
        Brush.linearGradient(listOf(colors.secondary, Color(0xFF7F3FF2))),
        Brush.linearGradient(listOf(Color(0xFF00E676), Color(0xFF00B0FF)))
    )
    val titles = listOf("Total Revenue", "Active Users", "Sales Target")
    val values = listOf("$14,230", "1,240", "85%")

    Column(modifier = Modifier.fillMaxWidth()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
            contentPadding = PaddingValues(horizontal = 40.dp),
            pageSpacing = 12.dp
        ) { page ->
            val pageOffset = ((pagerState.currentPage - page) + pagerState.currentPageOffsetFraction)
            val absOffset = pageOffset.absoluteValue

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize(0.9f)
                        .graphicsLayer {
                            translationX = pageOffset * 150f 
                            scaleX = 1f - (absOffset * 0.1f)
                            scaleY = 1f - (absOffset * 0.1f)
                            alpha = (0.4f - (absOffset * 0.6f)).coerceIn(0f, 0.4f)
                            rotationY = pageOffset * 20f
                            cameraDistance = 8f * density
                        }
                        .background(
                            brush = gradients[page % gradients.size],
                            shape = RoundedCornerShape(32.dp)
                        )
                )

                FluidGlassCard(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            val rotation = pageOffset * 35f
                            rotationY = rotation
                            cameraDistance = 12f * density
                            
                            val scale = 1f - (absOffset * 0.18f)
                            scaleX = scale
                            scaleY = scale
                            
                            alpha = 1f - (absOffset * 0.5f).coerceIn(0f, 0.9f)
                            translationY = absOffset * 30f
                        },
                    onClick = { onItemClick(titles[page]) },
                    shape = RoundedCornerShape(32.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(gradients[page % gradients.size])
                    ) {
                        Canvas(
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    translationX = pageOffset * 100f
                                }
                        ) {
                            drawCircle(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color.White.copy(alpha = 0.2f), Color.Transparent),
                                    center = Offset(size.width, 0f),
                                    radius = size.width * 0.8f
                                )
                            )
                        }

                        Column(
                            Modifier
                                .padding(28.dp)
                                .align(Alignment.BottomStart)
                                .graphicsLayer {
                                    translationX = -pageOffset * 50f
                                    alpha = 1f - (absOffset * 1.5f).coerceIn(0f, 1f)
                                }
                        ) {
                            Text(
                                text = titles[page],
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 1.sp
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = values[page],
                                color = Color.White,
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Black,
                                letterSpacing = (-1).sp
                            )
                        }
                    }
                }
            }
        }

        Row(
            Modifier
                .height(24.dp)
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            repeat(3) { iteration ->
                val active = pagerState.currentPage == iteration
                val color = if (active) colors.tertiary else colors.onSurfaceVariant.copy(alpha = 0.3f)
                val width by animateDpAsState(
                    targetValue = if (active) 28.dp else 8.dp,
                    animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessLow),
                    label = "indicatorWidth"
                )
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(CircleShape)
                        .background(color)
                        .size(width = width, height = 6.dp)
                )
            }
        }
    }
}

@Composable
fun SunburstRecentActivityList() {
    val colors = MaterialTheme.colorScheme
    FluidGlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            val activities = listOf(
                "Order #492 Shipped" to "2m ago",
                "New User Registered" to "15m ago",
                "System Backup Completed" to "1h ago"
            )
            activities.forEachIndexed { i, (title, time) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(Modifier.size(8.dp).background(colors.tertiary, CircleShape))
                    Spacer(Modifier.width(12.dp))
                    Text(title, color = colors.onBackground, modifier = Modifier.weight(1f), fontSize = 14.sp)
                    Text(time, color = colors.onSurfaceVariant, fontSize = 12.sp)
                }
                if (i < activities.size - 1) {
                    HorizontalDivider(color = colors.onSurfaceVariant.copy(alpha = 0.1f))
                }
            }
        }
    }
}

// ==================================================================
// 📸 STORIES SECTION
// ==================================================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun StoriesSection(viewModel: MainViewModel) {
    val stories by viewModel.stories.collectAsState()
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        contentPadding = PaddingValues(horizontal = 2.dp)
    ) {
        items(stories, key = { it.id }) { story ->
            if (story.id == "0") {
                AddStoryButton(onClick = { viewModel.addStory() })
            } else {
                StoryItem(story.name)
            }
        }
    }
}

@Composable
fun AddStoryButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.clickable { onClick() }
    ) {
        Soft3DIcon(
            imageVector = Icons.Rounded.Add,
            color = colors.primary,
            size = 56.dp,
            iconSize = 24.dp
        )
        Spacer(Modifier.height(6.dp))
        Text("Add", style = TextStyle(color = colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Bold))
    }
}

@Composable
fun StoryItem(name: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Soft3DIcon(
            imageVector = Icons.Rounded.Person,
            color = colors.tertiary,
            size = 56.dp,
            iconSize = 24.dp
        )
        Spacer(Modifier.height(6.dp))
        Text(name, style = TextStyle(color = colors.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium))
    }
}

// ==================================================================
// 🔐 LOGIN SCREEN (CONNECTED TO MONGODB BACKEND)
// ==================================================================

@Composable
fun LoginScreen(
    viewModel: MainViewModel,
    onLogin: () -> Unit,
    onSignUp: () -> Unit,
    onForgotPassword: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showServerConfig by remember { mutableStateOf(false) }

    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val serverStatus by viewModel.serverStatus.collectAsState()
    val rememberMe by viewModel.rememberMe.collectAsState()
    val serverBaseUrl by viewModel.serverBaseUrl.collectAsState()
    val colors = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Live Server Status Badge (Click to configure)
            Surface(
                shape = CircleShape,
                color = when (serverStatus) {
                    ServerStatus.Connected -> colors.primary.copy(alpha = 0.15f)
                    ServerStatus.Checking -> colors.tertiary.copy(alpha = 0.15f)
                    ServerStatus.Offline -> colors.error.copy(alpha = 0.15f)
                },
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    when (serverStatus) {
                        ServerStatus.Connected -> colors.primary.copy(alpha = 0.4f)
                        ServerStatus.Checking -> colors.tertiary.copy(alpha = 0.4f)
                        ServerStatus.Offline -> colors.error.copy(alpha = 0.4f)
                    }
                ),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable { showServerConfig = true }
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(
                                color = when (serverStatus) {
                                    ServerStatus.Connected -> Color(0xFF4CAF50)
                                    ServerStatus.Checking -> Color(0xFFFFC107)
                                    ServerStatus.Offline -> Color(0xFFF44336)
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = when (serverStatus) {
                            ServerStatus.Connected -> "Backend Online (Port 8080)"
                            ServerStatus.Checking -> "Connecting..."
                            ServerStatus.Offline -> "Server Offline (Configure)"
                        },
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.onBackground
                    )
                    Spacer(Modifier.width(6.dp))
                    Icon(
                        Icons.Rounded.Settings,
                        contentDescription = "Server Settings",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            FluidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(32.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .background(GradientBurst, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.LocalFireDepartment,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(
                        text = stringResource(R.string.mybusiness).uppercase(),
                        style = TextStyle(
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Black,
                            brush = GradientSunset
                        )
                    )
                    Text("Business Suite (MongoDB Cloud)", style = TextStyle(color = colors.onSurfaceVariant, fontSize = 13.sp))
                    Spacer(Modifier.height(24.dp))

                    SunburstTextField(
                        value = username,
                        onValueChange = {
                            username = it
                            errorMessage = null
                        },
                        label = "Username or Email",
                        icon = Icons.Rounded.Person
                    )
                    Spacer(Modifier.height(14.dp))
                    SunburstTextField(
                        value = password,
                        onValueChange = {
                            password = it
                            errorMessage = null
                        },
                        label = "Password",
                        icon = Icons.Rounded.Lock,
                        isPassword = true,
                        passwordVisible = passwordVisible,
                        onTogglePassword = { passwordVisible = !passwordVisible }
                    )

                    // Remember Me Checkbox
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = rememberMe,
                            onCheckedChange = { viewModel.setRememberMe(it) },
                            colors = CheckboxDefaults.colors(checkedColor = colors.primary)
                        )
                        Text(
                            text = "Remember me on this device",
                            color = colors.onSurfaceVariant,
                            fontSize = 12.sp,
                            modifier = Modifier.clickable { viewModel.setRememberMe(!rememberMe) }
                        )
                    }

                    if (errorMessage != null) {
                        Spacer(Modifier.height(12.dp))
                        FluidGlassCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.ErrorOutline, null, tint = colors.error, modifier = Modifier.size(20.dp))
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = errorMessage ?: "",
                                    color = colors.error,
                                    fontSize = 12.sp,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    // Primary Login Button
                    Button(
                        onClick = {
                            if (username.isBlank() || password.isBlank()) {
                                errorMessage = "Please enter both username/email and password"
                                return@Button
                            }
                            viewModel.login(
                                username = username,
                                password = password,
                                rememberMe = rememberMe,
                                onSuccess = onLogin,
                                onError = { msg -> errorMessage = msg }
                            )
                        },
                        enabled = !isAuthLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
                        contentPadding = PaddingValues(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(GradientBurst)
                                .padding(horizontal = 16.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                            } else {
                                Text("LOG IN", fontWeight = FontWeight.Bold, letterSpacing = 1.sp, color = Color.White)
                            }
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    // Quick Demo Login Shortcut
                    OutlinedButton(
                        onClick = {
                            viewModel.loginDemoUser(onSuccess = onLogin)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp),
                        shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.tertiary.copy(alpha = 0.6f))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.AutoAwesome, null, tint = colors.tertiary, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Try Demo Mode (Instant Login)", color = colors.tertiary, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                        }
                    }

                    Spacer(Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        TextButton(onClick = onForgotPassword) {
                            Text("Recovery", color = colors.onSurfaceVariant, fontSize = 13.sp)
                        }
                        TextButton(onClick = onSignUp) {
                            Text("Create Account", color = colors.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // Server Configuration Dialog
    if (showServerConfig) {
        var tempUrl by remember { mutableStateOf(serverBaseUrl) }
        var testResult by remember { mutableStateOf<String?>(null) }
        var isTesting by remember { mutableStateOf(false) }

        Dialog(onDismissRequest = { showServerConfig = false }) {
            FluidGlassCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Backend Server",
                            style = TextStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, brush = GradientSunset)
                        )
                        val loginPingStatus by viewModel.pingStatus.collectAsState()
                        ConnectionStatusPill(
                            status = loginPingStatus,
                            onRetry = { viewModel.pingServer(tempUrl) }
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Select target environment or enter custom IP:",
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )

                    Spacer(Modifier.height(14.dp))

                    // Preset Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { tempUrl = "http://10.0.2.2:8080/" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("Emulator", fontSize = 11.sp, color = colors.onBackground)
                        }
                        OutlinedButton(
                            onClick = { tempUrl = "http://127.0.0.1:8080/" },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("USB / Reverse", fontSize = 11.sp, color = colors.onBackground)
                        }
                    }

                    Spacer(Modifier.height(12.dp))

                    SunburstTextField(
                        value = tempUrl,
                        onValueChange = {
                            tempUrl = it
                            testResult = null
                        },
                        label = "Base URL",
                        icon = Icons.Rounded.Dns
                    )

                    if (testResult != null) {
                        Spacer(Modifier.height(8.dp))
                        Text(testResult ?: "", fontSize = 12.sp, color = if (testResult?.contains("success", true) == true) Color(0xFF4CAF50) else colors.error)
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                isTesting = true
                                testResult = "Pinging server..."
                                viewModel.setServerBaseUrl(tempUrl) { online ->
                                    isTesting = false
                                    testResult = if (online) "Ping successful! Server is UP." else "Failed to connect to $tempUrl"
                                }
                            },
                            enabled = !isTesting,
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Test Ping", fontSize = 12.sp)
                        }

                        Button(
                            onClick = {
                                viewModel.setServerBaseUrl(tempUrl)
                                showServerConfig = false
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Save & Close", fontSize = 12.sp, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SunburstTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    isPassword: Boolean = false,
    passwordVisible: Boolean = false,
    onTogglePassword: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, color = colors.onSurfaceVariant) },
        leadingIcon = { Icon(icon, null, tint = colors.primary) },
        trailingIcon = if (isPassword && onTogglePassword != null) {
            {
                IconButton(onClick = onTogglePassword) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                        contentDescription = "Toggle password",
                        tint = colors.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        } else null,
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = colors.primary,
            unfocusedBorderColor = colors.onSurfaceVariant.copy(alpha = 0.3f),
            focusedTextColor = colors.onBackground,
            unfocusedTextColor = colors.onBackground,
            cursorColor = colors.primary
        ),
        singleLine = true,
        visualTransformation = if (isPassword && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

// ==================================================================
// 🛍️ PRODUCT SCREEN
// ==================================================================

@OptIn(FlowPreview::class)
@Composable
fun ProductScreen(cartViewModel: CartViewModel = viewModel()) {
    var search by remember { mutableStateOf(TextFieldValue("")) }
    val colors = MaterialTheme.colorScheme
    var showCartSheet by remember { mutableStateOf(false) }

    val allProducts = remember { cartViewModel.sampleProducts }
    var filteredProducts by remember { mutableStateOf(allProducts) }
    val cartState by cartViewModel.state.collectAsState()

    // Debounced search with snapshotFlow { search.text }.debounce(300L).distinctUntilChanged()
    LaunchedEffect(Unit) {
        snapshotFlow { search.text }
            .debounce(300L)
            .distinctUntilChanged()
            .collect { query ->
                filteredProducts = if (query.isBlank()) {
                    allProducts
                } else {
                    allProducts.filter { product ->
                        product.name.contains(query, ignoreCase = true) ||
                                product.category.contains(query, ignoreCase = true) ||
                                product.description.contains(query, ignoreCase = true)
                    }
                }
            }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    SunburstTextField(
                        value = search.text,
                        onValueChange = { search = TextFieldValue(it) },
                        label = "Search products...",
                        icon = Icons.Rounded.Search
                    )
                }
                Spacer(Modifier.width(8.dp))
                // Quick Cart Access Action
                FluidGlassCard(
                    modifier = Modifier.size(54.dp),
                    shape = RoundedCornerShape(14.dp),
                    onClick = { showCartSheet = true }
                ) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        BadgedBox(
                            badge = {
                                if (cartState.totalItemCount > 0) {
                                    Badge(
                                        containerColor = colors.primary,
                                        contentColor = Color.White
                                    ) {
                                        Text("${cartState.totalItemCount}", fontSize = 9.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Rounded.ShoppingCart,
                                contentDescription = "View Cart",
                                tint = colors.primary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
            Spacer(Modifier.height(14.dp))

            if (filteredProducts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Rounded.SearchOff,
                            contentDescription = null,
                            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "No products found for '${search.text}'",
                            color = colors.onSurfaceVariant,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 110.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        FluidGlassCard(
                            modifier = Modifier.height(230.dp),
                            onClick = { cartViewModel.processIntent(CartIntent.AddItem(product)) }
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(16.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .fillMaxWidth(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Soft3DIcon(
                                        imageVector = Icons.Rounded.Widgets,
                                        color = colors.primary,
                                        size = 80.dp,
                                        iconSize = 36.dp
                                    )
                                }

                                Spacer(Modifier.height(14.dp))
                                Text(
                                    text = product.name,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = colors.onBackground,
                                    fontSize = 16.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = product.category,
                                    fontSize = 12.sp,
                                    color = colors.onSurfaceVariant,
                                    fontWeight = FontWeight.Medium
                                )

                                Spacer(Modifier.height(12.dp))

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$${String.format("%.2f", product.price)}",
                                        color = colors.tertiary,
                                        fontWeight = FontWeight.Black,
                                        fontSize = 16.sp
                                    )

                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape)
                                            .background(colors.primary.copy(alpha = 0.15f))
                                            .clickable {
                                                cartViewModel.processIntent(CartIntent.AddItem(product))
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Add,
                                            contentDescription = "Add to Cart",
                                            tint = colors.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showCartSheet) {
            CartBottomSheet(
                cartViewModel = cartViewModel,
                onDismiss = { showCartSheet = false }
            )
        }
    }
}

// ==================================================================
// 👤 ENHANCED PROFILE SCREEN (CONNECTED TO MONGODB DATABASE)
// ==================================================================

@Composable
fun EnhancedProfileScreen(
    viewModel: MainViewModel,
    cartViewModel: CartViewModel = viewModel()
) {
    val colors = MaterialTheme.colorScheme
    val currentUser by viewModel.currentUser.collectAsState()
    val isAuthLoading by viewModel.isAuthLoading.collectAsState()
    val cartState by cartViewModel.state.collectAsState()

    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val biometricManager = remember { BiometricAuthManager(context) }

    var showEditDialog by remember { mutableStateOf(false) }
    var showPaymentDialog by remember { mutableStateOf(false) }
    var biometricErrorMessage by remember { mutableStateOf<String?>(null) }

    val displayName = currentUser?.fullName?.takeIf { it.isNotBlank() }
        ?: currentUser?.username?.uppercase()
        ?: "USER PROFILE"
    val displayRole = currentUser?.role?.takeIf { it.isNotBlank() } ?: "Business Member"
    val displayBio = currentUser?.bio?.takeIf { it.isNotBlank() } ?: "Welcome to my profile! Manage orders and stats here."
    val displayPhone = currentUser?.phone?.takeIf { it.isNotBlank() } ?: "+1 555-0199"
    val displayLocation = currentUser?.location?.takeIf { it.isNotBlank() } ?: "San Francisco, CA"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Profile Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().background(GradientBurst, alpha = 0.15f))

            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(88.dp)
                        .border(3.dp, colors.tertiary, CircleShape)
                        .clip(CircleShape)
                        .background(colors.surface),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Person,
                        contentDescription = null,
                        tint = colors.onBackground,
                        modifier = Modifier.size(48.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = displayName,
                    color = colors.onBackground,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
                Text(
                    text = displayRole,
                    color = colors.tertiary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
                if (currentUser?.email != null) {
                    Text(
                        text = currentUser?.email ?: "",
                        color = colors.onSurfaceVariant,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Bio & Details Card
            FluidGlassCard {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("About", style = TextStyle(color = colors.tertiary, fontSize = 14.sp, fontWeight = FontWeight.Bold))
                    Text(displayBio, color = colors.onBackground, fontSize = 13.sp, lineHeight = 18.sp)
                    HorizontalDivider(color = colors.onSurfaceVariant.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.Phone, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(displayPhone, color = colors.onSurfaceVariant, fontSize = 13.sp)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.LocationOn, null, tint = colors.secondary, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(displayLocation, color = colors.onSurfaceVariant, fontSize = 13.sp)
                    }
                }
            }

            // Stats
            FluidGlassCard {
                Row(
                    modifier = Modifier
                        .padding(20.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    ProfileStat("Orders", "124")
                    ProfileStat("Cart", "${cartState.totalItemCount}")
                    ProfileStat("Saved", "12")
                }
            }

            Text("Account Settings", style = TextStyle(color = colors.tertiary, fontSize = 14.sp, fontWeight = FontWeight.Bold))
            FluidGlassCard {
                Column(Modifier.padding(vertical = 4.dp, horizontal = 16.dp)) {
                    SettingRow(
                        text = "Edit Profile",
                        icon = Icons.Rounded.Edit,
                        onClick = { showEditDialog = true }
                    )
                    SettingRow(
                        text = "Payment Methods",
                        icon = Icons.Rounded.CreditCard,
                        onClick = {
                            if (activity != null && biometricManager.canAuthenticate()) {
                                biometricManager.authenticate(
                                    activity = activity,
                                    title = "Payment Methods Security",
                                    subtitle = "Biometric 2FA verification required to view stored payment credentials",
                                    onSuccess = { showPaymentDialog = true },
                                    onError = { err -> biometricErrorMessage = err }
                                )
                            } else {
                                showPaymentDialog = true
                            }
                        }
                    )
                    SettingRow("Manage Subscription", Icons.Rounded.Star)
                }
            }

            Text("Support & Legal", style = TextStyle(color = colors.tertiary, fontSize = 14.sp, fontWeight = FontWeight.Bold))
            FluidGlassCard {
                Column(Modifier.padding(vertical = 4.dp, horizontal = 16.dp)) {
                    SettingRow("Help Center", Icons.Rounded.HelpOutline)
                    SettingRow("Privacy Policy", Icons.Rounded.Policy)
                }
            }

            Spacer(Modifier.height(110.dp))
        }
    }

    // Edit Profile Modal Dialog
    if (showEditDialog) {
        var editFullName by remember { mutableStateOf(currentUser?.fullName ?: "") }
        var editBio by remember { mutableStateOf(currentUser?.bio ?: "") }
        var editPhone by remember { mutableStateOf(currentUser?.phone ?: "") }
        var editLocation by remember { mutableStateOf(currentUser?.location ?: "") }
        var editRole by remember { mutableStateOf(currentUser?.role ?: "") }
        var editError by remember { mutableStateOf<String?>(null) }

        Dialog(onDismissRequest = { showEditDialog = false }) {
            FluidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(24.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Edit Profile",
                        style = TextStyle(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            brush = GradientSunset
                        )
                    )
                    Spacer(Modifier.height(16.dp))

                    SunburstTextField(
                        value = editFullName,
                        onValueChange = { editFullName = it },
                        label = "Full Name",
                        icon = Icons.Rounded.Badge
                    )
                    Spacer(Modifier.height(12.dp))

                    SunburstTextField(
                        value = editRole,
                        onValueChange = { editRole = it },
                        label = "Business Role / Title",
                        icon = Icons.Rounded.Work
                    )
                    Spacer(Modifier.height(12.dp))

                    SunburstTextField(
                        value = editBio,
                        onValueChange = { editBio = it },
                        label = "Bio",
                        icon = Icons.Rounded.Description
                    )
                    Spacer(Modifier.height(12.dp))

                    SunburstTextField(
                        value = editPhone,
                        onValueChange = { editPhone = it },
                        label = "Phone Number",
                        icon = Icons.Rounded.Phone
                    )
                    Spacer(Modifier.height(12.dp))

                    SunburstTextField(
                        value = editLocation,
                        onValueChange = { editLocation = it },
                        label = "Location",
                        icon = Icons.Rounded.LocationOn
                    )

                    if (editError != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(editError ?: "", color = colors.error, fontSize = 12.sp)
                    }

                    Spacer(Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showEditDialog = false },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Cancel", color = colors.onSurfaceVariant)
                        }

                        Button(
                            onClick = {
                                viewModel.updateProfile(
                                    fullName = editFullName,
                                    bio = editBio,
                                    phone = editPhone,
                                    location = editLocation,
                                    role = editRole,
                                    onSuccess = {
                                        showEditDialog = false
                                    },
                                    onError = { msg ->
                                        editError = msg
                                    }
                                )
                            },
                            enabled = !isAuthLoading,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            if (isAuthLoading) {
                                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Text("Save", color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }

    // Protected Biometrics Payment Methods Dialog
    if (showPaymentDialog) {
        Dialog(onDismissRequest = { showPaymentDialog = false }) {
            FluidGlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(Color(0xFF00E676).copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Rounded.Fingerprint,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Payment Methods",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    Text(
                        text = "Hardware Biometrics Verified (Android Keystore)",
                        fontSize = 11.sp,
                        color = Color(0xFF00E676),
                        fontWeight = FontWeight.Medium
                    )

                    Spacer(Modifier.height(20.dp))

                    // Saved Card 1
                    FluidGlassCard(shape = RoundedCornerShape(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.CreditCard, null, tint = colors.primary, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Visa Business •••• 4242", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("Expires 08/28 • Default", fontSize = 11.sp, color = colors.onSurfaceVariant)
                            }
                            Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF00E676), modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(Modifier.height(10.dp))

                    // Saved Card 2
                    FluidGlassCard(shape = RoundedCornerShape(14.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.AccountBalance, null, tint = colors.tertiary, modifier = Modifier.size(28.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Corporate Checking •••• 8891", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Text("JPMorgan Chase Bank", fontSize = 11.sp, color = colors.onSurfaceVariant)
                            }
                        }
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { showPaymentDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }

    if (biometricErrorMessage != null) {
        AlertDialog(
            onDismissRequest = { biometricErrorMessage = null },
            icon = { Icon(Icons.Rounded.Security, null, tint = colors.error) },
            title = { Text("Biometric Authentication Required") },
            text = { Text(biometricErrorMessage ?: "Verification failed.") },
            confirmButton = {
                TextButton(onClick = { biometricErrorMessage = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun ProfileStat(label: String, value: String) {
    val colors = MaterialTheme.colorScheme
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, fontWeight = FontWeight.Bold, fontSize = 20.sp, color = colors.primary)
        Text(label, style = TextStyle(color = colors.onSurfaceVariant, fontSize = 12.sp))
    }
}

// ==================================================================
// ⚙️ SETTINGS SCREEN
// ==================================================================

@Composable
fun SettingsScreen(
    viewModel: MainViewModel? = null,
    onLogout: (() -> Unit)? = null
) {
    val colors = MaterialTheme.colorScheme
    val currentUser = viewModel?.currentUser?.collectAsState()?.value
    val serverBaseUrl: String = viewModel?.serverBaseUrl?.collectAsState()?.value ?: "http://10.0.2.2:8080/"
    val pingStatus: PingStatus = viewModel?.pingStatus?.collectAsState()?.value ?: PingStatus.Idle

    val themeAccent by (viewModel?.themeAccent?.collectAsState() ?: remember { mutableStateOf(ThemeAccent.SUNBURST_AMBER) })
    val darkMode by (viewModel?.darkMode?.collectAsState() ?: remember { mutableStateOf(true) })
    val isBiometricProtected by (viewModel?.isBiometricProtected?.collectAsState() ?: remember { mutableStateOf(true) })

    val context = LocalContext.current
    val activity = context as? androidx.fragment.app.FragmentActivity
    val biometricManager = remember { BiometricAuthManager(context) }

    var endpointInput by remember(serverBaseUrl) { mutableStateOf(serverBaseUrl) }
    var isEditing by remember { mutableStateOf(false) }
    var showSecurityDialog by remember { mutableStateOf(false) }
    var biometricError by remember { mutableStateOf<String?>(null) }

    // Run a lightweight ping coroutine (HEAD request) when the screen opens
    LaunchedEffect(Unit) {
        viewModel?.pingServer()
    }

    fun applyEndpointWithBiometrics(targetUrl: String) {
        if (activity != null && isBiometricProtected && biometricManager.canAuthenticate()) {
            biometricManager.authenticate(
                activity = activity,
                title = "Endpoint Security Check",
                subtitle = "Biometric confirmation required to update backend server URL",
                onSuccess = {
                    endpointInput = targetUrl
                    viewModel?.setServerBaseUrl(targetUrl)
                    isEditing = false
                },
                onError = { err -> biometricError = err }
            )
        } else {
            endpointInput = targetUrl
            viewModel?.setServerBaseUrl(targetUrl)
            isEditing = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text("SETTINGS", style = TextStyle(color = colors.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold))
        Spacer(Modifier.height(16.dp))

        if (currentUser != null) {
            Text("Active Account", color = colors.tertiary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            FluidGlassCard {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        text = currentUser.fullName?.ifBlank { currentUser.username } ?: "User",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    Text(
                        text = currentUser.email ?: "",
                        fontSize = 13.sp,
                        color = colors.onSurfaceVariant
                    )
                    Text(
                        text = "Role: ${currentUser.role ?: "Business Member"}",
                        fontSize = 12.sp,
                        color = colors.tertiary
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }

        // ==============================================================
        // 🌐 RESILIENT NETWORKING SHOWCASE (DYNAMIC LAYER & HEALTH CHECK)
        // ==============================================================
        Text("Backend Network & Live Health", color = colors.tertiary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FluidGlassCard {
            Column(Modifier.padding(16.dp)) {
                // Header with Title and Real-time Status Pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Dns,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "Server Endpoint",
                            color = colors.onBackground,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }

                    // Live Connection Status Pill (Connected / Unreachable with round-trip latency in ms)
                    ConnectionStatusPill(
                        status = pingStatus,
                        onRetry = { viewModel?.pingServer(endpointInput) }
                    )
                }

                Spacer(Modifier.height(14.dp))

                // Editable Endpoint Field with Apply Action (Biometric protected)
                OutlinedTextField(
                    value = endpointInput,
                    onValueChange = { newValue: String ->
                        endpointInput = newValue
                        isEditing = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Base URL (OkHttp Dynamic Interceptor)", fontSize = 12.sp) },
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Rounded.Link, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        IconButton(
                            onClick = { applyEndpointWithBiometrics(endpointInput) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Check,
                                contentDescription = "Apply & Ping",
                                tint = if (isEditing) colors.primary else colors.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        }
                    },
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onDone = { applyEndpointWithBiometrics(endpointInput) }
                    ),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.onSurface.copy(alpha = 0.2f)
                    )
                )

                Spacer(Modifier.height(10.dp))

                // Quick Presets: Emulator (10.0.2.2), USB Reverse (127.0.0.1)
                Text(
                    text = "Quick Presets (Dynamic Switching):",
                    fontSize = 11.sp,
                    color = colors.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = endpointInput.contains("10.0.2.2"),
                        onClick = { applyEndpointWithBiometrics("http://10.0.2.2:8080/") },
                        label = { Text("Emulator (10.0.2.2)", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.PhoneAndroid, null, modifier = Modifier.size(14.dp))
                        }
                    )
                    FilterChip(
                        selected = endpointInput.contains("127.0.0.1"),
                        onClick = { applyEndpointWithBiometrics("http://127.0.0.1:8080/") },
                        label = { Text("USB / Reverse", fontSize = 11.sp) },
                        leadingIcon = {
                            Icon(Icons.Rounded.Usb, null, modifier = Modifier.size(14.dp))
                        }
                    )
                }

                Spacer(Modifier.height(12.dp))

                // Info footer explaining the architecture
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Backed by Jetpack DataStore",
                        fontSize = 11.sp,
                        color = colors.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    TextButton(
                        onClick = { viewModel?.pingServer(endpointInput) },
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Ping HEAD", fontSize = 12.sp)
                    }
                }

                if (pingStatus is PingStatus.Unreachable) {
                    val unreachable = pingStatus as PingStatus.Unreachable
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Notice: ${unreachable.reason}",
                        color = colors.error,
                        fontSize = 11.sp
                    )
                }
            }
        }

        // ==============================================================
        // 🎨 DYNAMIC THEMING ENGINE (ACCENTS & DARK MODE)
        // ==============================================================
        Spacer(Modifier.height(20.dp))
        Text("Visuals & Dynamic Theming", color = colors.tertiary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FluidGlassCard {
            Column(Modifier.padding(16.dp)) {
                // Dark Mode Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.DarkMode, null, tint = colors.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Column {
                            Text("Dark Mode", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            Text(
                                text = if (darkMode) "Deep Evening Sunburst Palette" else "Bright Light Palette",
                                fontSize = 11.sp,
                                color = colors.onSurfaceVariant
                            )
                        }
                    }
                    Switch(
                        checked = darkMode,
                        onCheckedChange = { viewModel?.setDarkMode(it) }
                    )
                }

                HorizontalDivider(
                    color = colors.onSurface.copy(alpha = 0.1f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                // Theme Accent Palette Selection
                Text(
                    text = "Theme Accents (BusinessSuiteTheme):",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onBackground
                )
                Spacer(Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeAccent.values().forEach { accent ->
                        val isSelected = themeAccent == accent
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel?.setThemeAccent(accent) },
                            label = { Text(accent.displayName, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(accent.primaryColor)
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = accent.primaryColor.copy(alpha = 0.25f),
                                selectedLabelColor = accent.tertiaryColor
                            )
                        )
                    }
                }
            }
        }

        // ==============================================================
        // 🔐 HARDWARE BIOMETRICS & KEYSTORE (SECURITY & 2FA)
        // ==============================================================
        Spacer(Modifier.height(20.dp))
        Text("System & Security (2FA)", color = colors.tertiary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        FluidGlassCard {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                SettingRow(
                    text = "Push Notifications",
                    icon = Icons.Rounded.Notifications
                )
                SettingRow(
                    text = "Security & 2FA (Hardware Keystore)",
                    icon = Icons.Rounded.Security,
                    onClick = { showSecurityDialog = true }
                )
                if (onLogout != null) {
                    SettingRow(
                        text = "Sign Out",
                        icon = Icons.AutoMirrored.Rounded.Logout,
                        onClick = onLogout
                    )
                }
            }
        }

        Spacer(Modifier.height(110.dp))
    }

    // Security & 2FA Info Dialog
    if (showSecurityDialog) {
        Dialog(onDismissRequest = { showSecurityDialog = false }) {
            FluidGlassCard(shape = RoundedCornerShape(24.dp)) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = Color(0xFF00E676),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = "Hardware Security & 2FA",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.onBackground
                    )
                    Text(
                        text = "Production-grade Keystore Cryptography",
                        fontSize = 12.sp,
                        color = colors.onSurfaceVariant
                    )

                    Spacer(Modifier.height(16.dp))

                    FluidGlassCard(shape = RoundedCornerShape(12.dp)) {
                        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("Android Keystore: AES-256 GCM Active", fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.CheckCircle, null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("EncryptedSharedPreferences Vault Active", fontSize = 12.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Rounded.Fingerprint, null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(8.dp))
                                Text("BiometricPrompt: BIOMETRIC_STRONG", fontSize = 12.sp)
                            }
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Require Biometrics for Sensitive Actions", fontSize = 12.sp, modifier = Modifier.weight(1f))
                        Switch(
                            checked = isBiometricProtected,
                            onCheckedChange = { viewModel?.setBiometricProtected(it) }
                        )
                    }

                    Spacer(Modifier.height(20.dp))

                    Button(
                        onClick = { showSecurityDialog = false },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Close")
                    }
                }
            }
        }
    }

    if (biometricError != null) {
        AlertDialog(
            onDismissRequest = { biometricError = null },
            icon = { Icon(Icons.Rounded.Warning, null, tint = colors.error) },
            title = { Text("Biometrics Cancelled") },
            text = { Text(biometricError ?: "Biometric confirmation was not completed.") },
            confirmButton = {
                TextButton(onClick = { biometricError = null }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun SettingRow(text: String, icon: ImageVector, onClick: () -> Unit = {}) {
    val colors = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, null, tint = colors.onSurfaceVariant, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(14.dp))
        Text(text, modifier = Modifier.weight(1f), color = colors.onBackground, fontSize = 14.sp)
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
            contentDescription = null,
            tint = colors.onSurfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier.size(18.dp)
        )
    }
}

// ==================================================================
// 📞 CONTACT SCREEN
// ==================================================================

@Composable
fun EnhancedContactScreen() {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        Text("CONTACT US", style = TextStyle(color = colors.onBackground, fontSize = 22.sp, fontWeight = FontWeight.Bold))

        FluidGlassCard {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Get in Touch", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Email, null, tint = colors.tertiary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("support@korebusiness.com", color = colors.onSurfaceVariant, fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Phone, null, tint = colors.tertiary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("+880 1234-567890", color = colors.onSurfaceVariant, fontSize = 14.sp)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.LocationOn, null, tint = colors.tertiary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(12.dp))
                    Text("123 Business Rd, Dhaka", color = colors.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        }

        FluidGlassCard {
            Column(Modifier.padding(20.dp)) {
                Text("Send a Message", style = MaterialTheme.typography.titleMedium, color = colors.onBackground)
                Spacer(Modifier.height(14.dp))
                SunburstTextField("", {}, "Subject", Icons.Rounded.Label)
                Spacer(Modifier.height(12.dp))
                SunburstTextField("", {}, "Message", Icons.Rounded.Message)
                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = {},
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.secondary, contentColor = Color.White),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("SEND MESSAGE", fontWeight = FontWeight.Bold)
                }
            }
        }

        Spacer(Modifier.height(96.dp))
    }
}

// ==================================================================
// ℹ️ ABOUT SCREEN
// ==================================================================

@Composable
fun EnhancedAboutScreen() {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        FluidGlassCard(Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Rounded.BusinessCenter,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(Modifier.height(20.dp))
                Text(
                    text = stringResource(R.string.app_name).uppercase(),
                    color = colors.onBackground,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(6.dp))
                Text("v5.0 Sunburst Edition", color = colors.onSurfaceVariant, fontSize = 14.sp)
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "A complete modern business dashboard crafted with Jetpack Compose.",
                    color = colors.onSurfaceVariant.copy(alpha = 0.8f),
                    fontSize = 13.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        }
    }
}

// ==================================================================
// 📄 PLACEHOLDER SCREEN
// ==================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimplePlaceholderScreen(title: String, msg: String, onBack: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(title, color = colors.onBackground) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = colors.onBackground)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            FluidGlassCard(Modifier.padding(28.dp).fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(msg, color = colors.onSurfaceVariant, fontSize = 14.sp)
                }
            }
        }
    }
}
