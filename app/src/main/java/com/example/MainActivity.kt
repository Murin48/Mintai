package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.LeaseAppTab
import com.example.ui.LeaseViewModel
import com.example.ui.screens.*
import com.example.ui.theme.LeaseCheckTheme

class MainActivity : ComponentActivity() {

    private val leaseViewModel: LeaseViewModel by viewModels()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            LeaseCheckTheme {
                val currentTab by leaseViewModel.currentTab.collectAsState()

                // Handle back button to return to home check tab
                BackHandler(enabled = currentTab != LeaseAppTab.CHECK) {
                    leaseViewModel.setTab(LeaseAppTab.CHECK)
                }

                Scaffold(
                    modifier = Modifier.fillMaxSize(),
                    topBar = {
                        CenterAlignedTopAppBar(
                            title = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Shield,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "LeaseCheck",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(
                                        shape = MaterialTheme.shapes.extraSmall,
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = "สคบ. 2562",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        )
                    },
                    bottomBar = {
                        LeaseBottomBar(
                            currentTab = currentTab,
                            onTabSelected = { leaseViewModel.setTab(it) }
                        )
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        Crossfade(targetState = currentTab, label = "TabCrossfade") { tab ->
                            when (tab) {
                                LeaseAppTab.CHECK -> ContractCheckScreen(viewModel = leaseViewModel)
                                LeaseAppTab.CALCULATOR -> CalculatorScreen(viewModel = leaseViewModel)
                                LeaseAppTab.RULES -> HandbookScreen()
                                LeaseAppTab.HISTORY -> HistoryScreen(viewModel = leaseViewModel)
                            }
                        }
                    }
                }
            }
        }
    }
}

data class NavigationTabItem(
    val tab: LeaseAppTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

@Composable
fun LeaseBottomBar(
    currentTab: LeaseAppTab,
    onTabSelected: (LeaseAppTab) -> Unit
) {
    val items = listOf(
        NavigationTabItem(
            tab = LeaseAppTab.CHECK,
            label = "ตรวจสัญญา",
            selectedIcon = Icons.Filled.FactCheck,
            unselectedIcon = Icons.Outlined.FactCheck
        ),
        NavigationTabItem(
            tab = LeaseAppTab.CALCULATOR,
            label = "คำนวณส่วนต่าง",
            selectedIcon = Icons.Filled.Calculate,
            unselectedIcon = Icons.Outlined.Calculate
        ),
        NavigationTabItem(
            tab = LeaseAppTab.RULES,
            label = "เกณฑ์ สคบ.",
            selectedIcon = Icons.Filled.MenuBook,
            unselectedIcon = Icons.Outlined.MenuBook
        ),
        NavigationTabItem(
            tab = LeaseAppTab.HISTORY,
            label = "ประวัติ",
            selectedIcon = Icons.Filled.History,
            unselectedIcon = Icons.Outlined.History
        )
    )

    NavigationBar(
        modifier = Modifier.testTag("lease_bottom_bar")
    ) {
        items.forEach { item ->
            val isSelected = currentTab == item.tab
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.label
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        fontSize = 10.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            )
        }
    }
}
