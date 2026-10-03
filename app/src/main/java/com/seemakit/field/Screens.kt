@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.seemakit.field

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import kotlinx.coroutines.launch

const val MAX_HDOP = 2.0
fun qualityName(q: Int) = when (q) { 4 -> "RTK Fixed (cm-level)"; 5 -> "RTK Float (dm)"; 2 -> "DGPS"; 1 -> "GPS only"; else -> "No fix" }
fun gateOk(f: Fix?) = f != null && f.quality == 4 && f.hdop <= MAX_HDOP
fun gateMsg(f: Fix?) = when {
    f == null -> "No position stream. Connect rover or start Demo Simulation."
    f.quality != 4 -> "${qualityName(f.quality)}: Requires RTK Fixed lock for legal cadastral validity."
    f.hdop > MAX_HDOP -> "HDOP ${"%.1f".format(f.hdop)} is above $MAX_HDOP. Satellite geometry too poor."
    else -> "Fix verified: Centimeter RTK locked. Level the pole and capture."
}

@Composable fun Nav(vm: VM) {
    val nc = rememberNavController()
    NavHost(nc, "parcels") {
        composable("parcels") { ParcelsScreen(vm, nc) }
        composable("survey/{id}") { SurveyScreen(vm, nc, it.arguments!!.getString("id")!!.toLong()) }
        composable("stakeout/{id}") { StakeoutScreen(vm, nc, it.arguments!!.getString("id")!!.toLong()) }
        composable("settings") { SettingsScreen(nc) }
    }
}

// ==========================================
// 1. PARCELS LIST SCREEN
// ==========================================
@Composable fun ParcelsScreen(vm: VM, nc: NavHostController) {
    val list by vm.parcels.collectAsState()
    var add by remember { mutableStateOf(false) }
    var deleteTarget by remember { mutableStateOf<Parcel?>(null) }
    val sb = remember { SnackbarHostState() }
    val sc = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                        Text("SeemaKit", modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                },
                actions = {
                    TextButton(onClick = { vm.loadDemoData(); sc.launch { sb.showSnackbar("Sample cadastral parcel loaded (Khasra 142/1)") } }) {
                        Text("Demo Data")
                    }
                    TextButton(onClick = { vm.sync(); sc.launch { sb.showSnackbar("Sync queued to central cadastre.") } }) {
                        Text("Sync")
                    }
                    IconButton(onClick = { nc.navigate("settings") }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(sb) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { add = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, "New parcel")
            }
        }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                list == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                list!!.isEmpty() -> {
                    Column(
                        Modifier.align(Alignment.Center).padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer, modifier = Modifier.size(72.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                        Text("No Cadastral Parcels Yet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "Start a new field survey or load our pre-configured sample farm to test the workflow.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                        Button(
                            onClick = { vm.loadDemoData() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Load Sample Parcel (Khasra 142/1)")
                        }
                    }
                }
                else -> {
                    LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        item {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("Tap any parcel to view the polygon map, capture corners, or test stakeout navigation.", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }

                        items(list!!, key = { it.id }) { p ->
                            Card(
                                onClick = { nc.navigate("survey/${p.id}") },
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)),
                                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            ) {
                                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("Khasra / Survey No. ${p.surveyNo}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                        Spacer(Modifier.weight(1f))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = if (p.synced) Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                        ) {
                                            Text(
                                                if (p.synced) "✓ Synced" else "⏳ Local Only",
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (p.synced) Color(0xFF2E7D32) else Color(0xFFE65100)
                                            )
                                        }
                                    }
                                    Text("Village: ${p.village}", style = MaterialTheme.typography.bodyMedium)
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text("RoR Area: ${"%.0f".format(p.rorAreaSqm)} sq m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(Modifier.weight(1f))
                                        TextButton(onClick = { deleteTarget = p }) {
                                            Text("Delete", color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (add) {
        AddParcel({ add = false }) { s, v, a -> vm.addParcel(s, v, a); add = false }
    }

    deleteTarget?.let { parcel ->
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete survey parcel?") },
            text = { Text("Khasra No. ${parcel.surveyNo} in ${parcel.village} and all its surveyed corner pegs will be permanently removed.") },
            confirmButton = { TextButton(onClick = { vm.delete(parcel.id); deleteTarget = null }) { Text("Delete", color = MaterialTheme.colorScheme.error) } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text("Cancel") } }
        )
    }
}

// ==========================================
// 2. ADD PARCEL DIALOG
// ==========================================
@Composable fun AddParcel(close: () -> Unit, save: (String, String, Double) -> Unit) {
    var s by remember { mutableStateOf("") }
    var v by remember { mutableStateOf("") }
    var a by remember { mutableStateOf("") }
    val area = a.toDoubleOrNull()
    AlertDialog(
        onDismissRequest = close,
        title = { Text("New Cadastral Parcel") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(s, { s = it }, label = { Text("Khasra / Survey Number") }, placeholder = { Text("e.g. 142/1") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(v, { v = it }, label = { Text("Village / Gram Panchayat") }, placeholder = { Text("e.g. Rampur") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(a, { a = it }, label = { Text("RoR Registered Area (sq m)") }, placeholder = { Text("e.g. 2450") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
                    isError = a.isNotEmpty() && (area == null || area <= 0),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal))
            }
        },
        confirmButton = { TextButton(enabled = s.isNotBlank() && area != null && area > 0, onClick = { save(s.trim(), v.trim(), area!!) }) { Text("Create Survey") } },
        dismissButton = { TextButton(onClick = close) { Text("Cancel") } }
    )
}

// ==========================================
// 3. SURVEY & MAP SCREEN
// ==========================================
@Composable fun SurveyScreen(vm: VM, nc: NavHostController, id: Long) {
    val ctx = LocalContext.current
    val parcel by vm.parcel(id).collectAsState(null)
    val corners by vm.corners(id).collectAsState(emptyList())
    val fix by vm.rover.fix.collectAsState()
    val status by vm.rover.status.collectAsState()
    val isSimulating by vm.rover.isSimulating.collectAsState()

    var pick by remember { mutableStateOf(false) }
    var owner by rememberSaveable { mutableStateOf("") }
    var neigh by rememberSaveable { mutableStateOf("") }
    var gcp by rememberSaveable { mutableStateOf(false) }
    var msg by remember { mutableStateOf("") }

    val p = parcel
    if (p == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }; return }

    val area = Geo.area(corners)
    val per = Geo.perimeter(corners)
    val ok = gateOk(fix) && owner.isNotBlank() && neigh.isNotBlank()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Survey: Khasra ${p.surveyNo}") },
                navigationIcon = {
                    IconButton(onClick = { nc.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = { nc.navigate("stakeout/$id") }, enabled = corners.isNotEmpty()) {
                        Text("Stakeout")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header information
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column {
                    Text("Village: ${p.village}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Legal RoR Area: ${"%.0f".format(p.rorAreaSqm)} sq m", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            // 1. Interactive 2D Cadastral Polygon Canvas
            Text("Cadastral Map View", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            ParcelCanvas(corners = corners, roverFix = fix)

            // 2. Rover Connection & Simulation Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = if (gateOk(fix)) Color(0xFF2E7D32) else if (fix != null) Color(0xFFFF9800) else Color(0xFF9E9E9E),
                            modifier = Modifier.size(10.dp)
                        ) {}
                        Spacer(Modifier.width(8.dp))
                        Text(status, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        if (isSimulating) {
                            Surface(shape = RoundedCornerShape(4.dp), color = Color(0xFFE3F2FD)) {
                                Text("DEMO ACTIVE", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp), style = MaterialTheme.typography.labelSmall, color = Color(0xFF1565C0), fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    fix?.let {
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            AssistChip(onClick = {}, label = { Text(qualityName(it.quality)) }, leadingIcon = { Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(14.dp), tint = Color(0xFF2E7D32)) })
                            AssistChip(onClick = {}, label = { Text("HDOP ${"%.1f".format(it.hdop)}") })
                            AssistChip(onClick = {}, label = { Text("${it.sats} Sats") })
                        }
                    }

                    Text(gateMsg(fix), style = MaterialTheme.typography.bodySmall, color = if (gateOk(fix)) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = { vm.toggleSimulation() },
                            colors = ButtonDefaults.buttonColors(containerColor = if (isSimulating) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(if (isSimulating) Icons.Default.Close else Icons.Default.PlayArrow, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text(if (isSimulating) "Stop Demo" else "Simulate Rover")
                        }

                        if (isSimulating) {
                            OutlinedButton(
                                onClick = {
                                    val nextSeq = vm.stepDemoCorner()
                                    msg = "Virtual rover moved to Peg #$nextSeq"
                                }
                            ) {
                                Text("Next Peg")
                            }
                        }

                        OutlinedButton(onClick = { pick = true }) {
                            Icon(Icons.Default.Bluetooth, null, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Hardware")
                        }
                    }
                }
            }

            // 3. Witness Verification & Corner Capture Card
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Witness Demarcation (पंचनामा)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = {
                                owner = "Ramesh Kumar (मालिक)"
                                neigh = "Suresh Patel (पड़ोसी)"
                            }
                        ) {
                            Text("Fill Demo", style = MaterialTheme.typography.labelSmall)
                        }
                    }

                    OutlinedTextField(owner, { owner = it }, label = { Text("Land Owner Name (मालिक)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(neigh, { neigh = it }, label = { Text("Adjacent Neighbour Name (पड़ोसी गवाह)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(gcp, { gcp = it })
                        Text("Tag as Ground Control Point (GCP)", style = MaterialTheme.typography.bodyMedium)
                    }

                    Button(
                        enabled = ok,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        onClick = {
                            vm.capture(id, corners.size + 1, fix!!, gcp, owner.trim(), neigh.trim())
                            msg = "✓ Corner #${corners.size + 1} captured & witness recorded"
                            if (isSimulating) vm.stepDemoCorner()
                        }
                    ) {
                        Icon(Icons.Default.Place, null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Capture Corner #${corners.size + 1}")
                    }

                    if (msg.isNotEmpty()) {
                        Text(msg, color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold)
                    }
                }
            }

            // 4. Surveyed Corners List
            Text("Surveyed Pegs (${corners.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            if (corners.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("No corner pegs recorded yet. Activate simulator or walk to peg with rover.", modifier = Modifier.padding(14.dp), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            corners.forEach { c ->
                Card(Modifier.fillMaxWidth()) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Surface(shape = CircleShape, color = if (c.isGcp) Color(0xFFE65100) else Color(0xFF2E7D32), modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("#${c.seq}", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text("${"%.7f".format(c.lat)}, ${"%.7f".format(c.lon)}${if (c.isGcp) " (GCP)" else ""}", fontWeight = FontWeight.SemiBold)
                            Text("Witness: ${c.ownerWitness} & ${c.neighbourWitness}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(onClick = { vm.removeCorner(c) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }

            // 5. Cadastral Summary & RoR Discrepancy Check
            if (corners.size >= 3) {
                val diff = (area - p.rorAreaSqm) / p.rorAreaSqm * 100
                val isDiscrepancy = kotlin.math.abs(diff) > 5.0

                Card(
                    colors = CardDefaults.cardColors(containerColor = if (isDiscrepancy) Color(0xFFFFEBEE) else Color(0xFFE8F5E9)),
                    border = androidx.compose.foundation.BorderStroke(1.dp, if (isDiscrepancy) Color(0xFFEF5350) else Color(0xFF66BB6A))
                ) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Area Audit Summary", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isDiscrepancy) Color(0xFFC62828) else Color(0xFF1B5E20))
                        Text("Field Measured Area: ${"%.1f".format(area)} sq m")
                        Text("Registered RoR Area: ${"%.0f".format(p.rorAreaSqm)} sq m")
                        Text("Variance: ${"%.2f".format(diff)}%", fontWeight = FontWeight.Bold, color = if (isDiscrepancy) Color(0xFFC62828) else Color(0xFF2E7D32))
                        Text(
                            if (isDiscrepancy) "⚠ Flag: Area deviates from official Record of Rights by > 5%. Field re-survey or Revenue Officer inspection recommended."
                            else "✓ Verified: Measured boundary complies with official revenue records (within 5% tolerance).",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isDiscrepancy) Color(0xFFB71C1C) else Color(0xFF1B5E20)
                        )
                    }
                }
            }

            // 6. Action Bar
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    enabled = corners.size >= 3,
                    onClick = {
                        ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, Export.json(p, corners)), "Export Cadastral GeoJSON"))
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Export GeoJSON")
                }
                OutlinedButton(
                    enabled = corners.isNotEmpty(),
                    onClick = { nc.navigate("stakeout/$id") },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Navigation, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Stakeout HUD")
                }
            }
        }
    }

    if (pick) {
        val devs = remember { vm.rover.paired() }
        AlertDialog(
            onDismissRequest = { pick = false },
            title = { Text("Paired Bluetooth Rovers") },
            text = {
                if (devs.isEmpty()) {
                    Text("No paired Bluetooth SPP devices found. Pair the ESP32 rover in phone Bluetooth settings first.")
                } else {
                    Column {
                        devs.forEach { d ->
                            TextButton(onClick = { pick = false; vm.connect(d) }) {
                                Text(try { d.name ?: d.address } catch (e: SecurityException) { d.address })
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { pick = false }) { Text("Close") } }
        )
    }
}

// ==========================================
// 4. STAKEOUT RADAR HUD SCREEN
// ==========================================
@Composable fun StakeoutScreen(vm: VM, nc: NavHostController, id: Long) {
    val corners by vm.corners(id).collectAsState(emptyList())
    val fix by vm.rover.fix.collectAsState()
    var sel by remember { mutableStateOf<Corner?>(null) }
    val isSimulating by vm.rover.isSimulating.collectAsState()

    // Default select first corner
    LaunchedEffect(corners) {
        if (sel == null && corners.isNotEmpty()) sel = corners.first()
    }

    val t = sel
    val f = fix
    val dist = if (t != null && f != null) Geo.dist(f.lat, f.lon, t.lat, t.lon) else 999.0
    val bearing = if (t != null && f != null) Geo.bearing(f.lat, f.lon, t.lat, t.lon) else 0.0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stakeout Navigation (निशानदेही)") },
                navigationIcon = {
                    IconButton(onClick = { nc.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(pad)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Select target boundary peg to locate:", style = MaterialTheme.typography.bodyMedium)

            // Horizontal peg selector
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(corners) { c ->
                    FilterChip(
                        selected = sel?.id == c.id,
                        onClick = { sel = c },
                        label = { Text("Peg #${c.seq}") },
                        leadingIcon = if (sel?.id == c.id) {
                            { Icon(Icons.Default.Place, null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            // Radar HUD
            StakeoutRadar(distanceMeters = dist, bearingDegrees = bearing)

            // Target Telemetry Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = if (dist <= 0.5) Color(0xFFE8F5E9) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, if (dist <= 0.5) Color(0xFF4CAF50) else Color.Transparent)
            ) {
                Column(Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (f == null) {
                        Text("Waiting for rover position stream...", style = MaterialTheme.typography.bodyMedium)
                        if (!isSimulating) {
                            Button(onClick = { vm.toggleSimulation() }) { Text("Start Virtual Rover") }
                        }
                    } else {
                        Text(
                            "${"%.2f".format(dist)} m",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = if (dist <= 0.5) Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface
                        )
                        Text("Bearing to Target: ${"%.0f".format(bearing)}°", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)

                        if (dist <= 0.5) {
                            Surface(shape = RoundedCornerShape(8.dp), color = Color(0xFF2E7D32)) {
                                Text("🎯 TARGET LOCKED: Place Boundary Stone Here!", modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp), color = Color.White, fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Text("Walk forward following the radar needle", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }

                        // Simulation test button to jump to target
                        if (isSimulating && t != null) {
                            Spacer(Modifier.height(4.dp))
                            OutlinedButton(
                                onClick = { vm.rover.setSimulatedLocation(t.lat, t.lon) }
                            ) {
                                Text("Demo: Jump to Target Peg")
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 5. SETTINGS SCREEN
// ==========================================
@Composable fun SettingsScreen(nc: NavHostController) {
    val c = LocalContext.current
    val pr = remember { Secure.prefs(c) }
    var url by remember { mutableStateOf(pr.getString("url", "") ?: "") }
    var tok by remember { mutableStateOf(pr.getString("token", "") ?: "") }
    var saved by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings & About") },
                navigationIcon = {
                    IconButton(onClick = { nc.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { pad ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(pad)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text("Central Cadastre Server", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                url, { url = it; saved = false },
                label = { Text("Server URL (HTTPS Only)") },
                placeholder = { Text("https://cadastre.gov.in/api") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                isError = url.isNotEmpty() && !url.startsWith("https://")
            )
            OutlinedTextField(
                tok, { tok = it; saved = false },
                label = { Text("Surveyor Bearer Token") },
                placeholder = { Text("auth_token_xxx") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Button(
                enabled = url.isEmpty() || url.startsWith("https://"),
                onClick = {
                    pr.edit().putString("url", url.trim()).putString("token", tok.trim()).apply()
                    saved = true
                }
            ) {
                Text("Save Credentials")
            }
            if (saved) {
                Text("✓ Stored securely with hardware-backed AES256-GCM encryption.", color = Color(0xFF2E7D32), style = MaterialTheme.typography.bodySmall)
            }

            Spacer(Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            ) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("About SeemaKit (सीमाकिट)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("Version 1.0 (Prototype)", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Text(
                        "SeemaKit is a decentralized, centimeter-accurate RTK land boundary surveying system. " +
                        "It empowers rural surveyors, panchayats, and farmers to demarcate land parcels at 1/50th the cost of commercial Total Stations, " +
                        "protecting against boundary disputes with mandatory dual-witness verification.",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Divider(Modifier.padding(vertical = 4.dp))
                    Text("Hardware Support:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodySmall)
                    Text("• ESP32 MCU + u-blox ZED-F9P RTK GNSS Receiver\n• Bluetooth Classic SPP (NMEA GGA stream)\n• Accuracy: ±1 to 2 cm (RTK Fixed)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
