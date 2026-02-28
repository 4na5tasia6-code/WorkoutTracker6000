package com.anastasia.starsettracker

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat.startActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.anastasia.starsettracker.data.db.MachineEntity
import com.anastasia.starsettracker.ui.navigation.Tab
import com.anastasia.starsettracker.ui.screens.MachinesViewModel
import com.anastasia.starsettracker.ui.screens.StatsViewModel
import com.anastasia.starsettracker.ui.screens.TodayViewModel
import com.anastasia.starsettracker.ui.theme.StarSetTheme
import kotlin.math.min

class MainActivity : ComponentActivity() {
    private val todayVm by viewModels<TodayViewModel>()
    private val statsVm by viewModels<StatsViewModel>()
    private val machinesVm by viewModels<MachinesViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            StarSetTheme {
                AppRoot(todayVm, statsVm, machinesVm) { intent ->
                    startActivity(this, Intent.createChooser(intent, "Export CSV"), null)
                }
            }
        }
    }
}

@Composable
private fun AppRoot(
    todayVm: TodayViewModel,
    statsVm: StatsViewModel,
    machinesVm: MachinesViewModel,
    share: (Intent) -> Unit
) {
    val nav = rememberNavController()
    val tabs = listOf(Tab.Today, Tab.Stats, Tab.Machines)
    val current = nav.currentBackStackEntryAsState().value?.destination?.route ?: Tab.Today.route

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = current == tab.route,
                        onClick = { nav.navigate(tab.route) },
                        label = { Text(tab.label) },
                        icon = {}
                    )
                }
            }
        }
    ) { padding ->
        NavHost(navController = nav, startDestination = Tab.Today.route, modifier = Modifier.padding(padding)) {
            composable(Tab.Today.route) { TodayScreen(todayVm) }
            composable(Tab.Stats.route) { StatsScreen(statsVm, share) }
            composable(Tab.Machines.route) { MachinesScreen(machinesVm) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayScreen(vm: TodayViewModel) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    val logs by vm.logs.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var editMachine by remember { mutableStateOf<MachineEntity?>(null) }

    Scaffold(snackbarHost = { SnackbarHost(hostState = snackbar) }) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                val bounce by animateFloatAsState(if (state.stars > 0) 1.05f else 1f, label = "bounce")
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Day ${state.dayType}", fontWeight = FontWeight.Bold, fontSize = 22.sp)
                        Text("Stars ${state.stars} / 10")
                        LinearProgressIndicator(progress = { min(state.stars, 10) / 10f }, modifier = Modifier.fillMaxWidth())
                        if (state.stars >= 10) Text("Bonus mode active ✨")
                        Text("Points ${state.points}", fontSize = (18 * bounce).sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(onClick = vm::finishWorkout) { Text("Finish Workout") }
                        }
                    }
                }
            }
            items(state.machines) { machine ->
                val done = logs.count { it.machineId == machine.id }
                MachineCard(
                    machine = machine,
                    setsDone = done,
                    onLog = { weight -> vm.logSet(machine, weight) { } },
                    onEdit = { editMachine = machine }
                )
            }
            item {
                Text("Recent sets")
                logs.take(8).forEach { log ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("#${log.starIndex} • ${log.weight}kg")
                        TextButton(onClick = { vm.undo(log.id) }) { Text("Undo") }
                    }
                }
            }
        }
    }

    LaunchedEffect(logs.firstOrNull()?.id) {
        val newest = logs.firstOrNull() ?: return@LaunchedEffect
        val result = snackbar.showSnackbar("Logged +1 star", "Undo")
        if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) vm.undo(newest.id)
    }

    editMachine?.let { machine ->
        var weight by remember(machine.id) { mutableIntStateOf(machine.lastWeight) }
        AlertDialog(
            onDismissRequest = { editMachine = null },
            confirmButton = {
                TextButton(onClick = {
                    vm.saveMachine(machine.copy(lastWeight = weight))
                    editMachine = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { editMachine = null }) { Text("Cancel") } },
            title = { Text("Edit weight") },
            text = {
                OutlinedTextField(
                    value = weight.toString(),
                    onValueChange = { weight = it.toIntOrNull() ?: weight },
                    label = { Text("Weight") }
                )
            }
        )
    }
}

@Composable
private fun MachineCard(
    machine: MachineEntity,
    setsDone: Int,
    onLog: (Int) -> Unit,
    onEdit: () -> Unit
) {
    var quick by remember { mutableStateOf(false) }
    var customWeight by remember { mutableIntStateOf(machine.lastWeight) }

    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(machine.name, fontWeight = FontWeight.SemiBold, fontSize = 20.sp)
            Text("Sets $setsDone / 2 • x${machine.multiplier}")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onLog(machine.lastWeight) },
                    modifier = Modifier.weight(1f).combinedClickable(
                        onClick = { onLog(machine.lastWeight) },
                        onLongClick = { quick = true }
                    )
                ) { Text("Log Set") }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Edit") }
            }
        }
    }

    if (quick) {
        AlertDialog(
            onDismissRequest = { quick = false },
            confirmButton = { TextButton(onClick = { onLog(customWeight); quick = false }) { Text("Log") } },
            dismissButton = { TextButton(onClick = { quick = false }) { Text("Cancel") } },
            title = { Text("Quick weight") },
            text = {
                OutlinedTextField(
                    value = customWeight.toString(),
                    onValueChange = { customWeight = it.toIntOrNull() ?: customWeight }
                )
            }
        )
    }
}

@Composable
fun StatsScreen(vm: StatsViewModel, share: (Intent) -> Unit) {
    val state by vm.uiState.collectAsStateWithLifecycle()
    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Text("Quest streak: ${state.questStreak}")
                Text("Soft streak: ${state.softStreak}")
                Text("Best day points: ${state.bestSessionPoints}")
                Text("Weekly total points: ${state.weeklyTotalPoints}")
                Button(onClick = { vm.export(share) }) { Text("Export CSV") }
            }
        }
        Text("Last 14 sessions")
        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            items(state.sessions) { s ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("${s.workoutDayKey} Day ${s.dayType}")
                        Text("⭐${s.starsEarned} • ${s.pointsEarned} pts • ${if (s.questCleared) "Yes" else "No"}")
                    }
                }
            }
        }
    }
}

@Composable
fun MachinesScreen(vm: MachinesViewModel) {
    val machines by vm.machines.collectAsStateWithLifecycle()
    var edit by remember { mutableStateOf<MachineEntity?>(null) }

    Column(Modifier.fillMaxSize().padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Edit Machines", fontSize = 22.sp)
            TextButton(onClick = vm::resetDefaults) { Text("Reset Defaults") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(machines) { m ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(10.dp).fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Column {
                            Text(m.name)
                            Text("x${m.multiplier} • ${m.lastWeight}kg • ${if (m.isActive) "Active" else "Off"}")
                        }
                        Row {
                            IconButton(onClick = { vm.move(m, true) }) { Icon(Icons.Default.KeyboardArrowUp, null) }
                            IconButton(onClick = { vm.move(m, false) }) { Icon(Icons.Default.KeyboardArrowDown, null) }
                            IconButton(onClick = { edit = m }) { Icon(Icons.Default.Edit, null) }
                        }
                    }
                }
            }
        }
    }

    edit?.let { m ->
        var name by remember { mutableStateOf(m.name) }
        var multiplier by remember { mutableStateOf(m.multiplier.toString()) }
        var weight by remember { mutableStateOf(m.lastWeight.toString()) }
        var active by remember { mutableStateOf(m.isActive) }
        AlertDialog(
            onDismissRequest = { edit = null },
            confirmButton = {
                TextButton(onClick = {
                    vm.save(
                        m.copy(
                            name = name,
                            multiplier = multiplier.toDoubleOrNull() ?: m.multiplier,
                            lastWeight = weight.toIntOrNull() ?: m.lastWeight,
                            isActive = active
                        )
                    )
                    edit = null
                }) { Text("Save") }
            },
            dismissButton = { TextButton(onClick = { edit = null }) { Text("Cancel") } },
            title = { Text("Edit ${m.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Name") })
                    OutlinedTextField(value = multiplier, onValueChange = { multiplier = it }, label = { Text("Multiplier") })
                    OutlinedTextField(value = weight, onValueChange = { weight = it }, label = { Text("Weight") })
                    Row {
                        TextButton(onClick = { active = !active }) { Text(if (active) "Active" else "Inactive") }
                    }
                }
            }
        )
    }
}
