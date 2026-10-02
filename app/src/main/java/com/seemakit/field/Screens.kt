@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package com.seemakit.field
import android.content.Intent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import kotlinx.coroutines.launch

const val MAX_HDOP = 2.0
fun qualityName(q: Int) = when (q) { 4 -> "RTK fixed"; 5 -> "RTK float"; 2 -> "DGPS"; 1 -> "GPS only"; else -> "No fix" }
fun gateOk(f: Fix?) = f != null && f.quality == 4 && f.hdop <= MAX_HDOP
fun gateMsg(f: Fix?) = when {
    f == null -> "No position yet. Connect the rover and wait for satellites."
    f.quality != 4 -> "${qualityName(f.quality)}: wait for RTK fixed, or move to open sky."
    f.hdop > MAX_HDOP -> "HDOP ${"%.1f".format(f.hdop)} is above $MAX_HDOP. Wait for better geometry."
    else -> "Fix is good. Level the pole and capture."
}

@Composable fun Nav(vm: VM) {
    val nc = rememberNavController()
    NavHost(nc, "parcels") {
        composable("parcels") { ParcelsScreen(vm, nc) }
        composable("survey/{id}") { SurveyScreen(vm, nc, it.arguments!!.getString("id")!!.toLong()) }
        composable("stakeout/{id}") { StakeoutScreen(vm, it.arguments!!.getString("id")!!.toLong()) }
        composable("settings") { SettingsScreen() }
    }
}

@Composable fun ParcelsScreen(vm: VM, nc: NavHostController) {
    val list by vm.parcels.collectAsState()
    var add by remember { mutableStateOf(false) }
    val sb = remember { SnackbarHostState() }; val sc = rememberCoroutineScope()
    Scaffold(
        topBar = { TopAppBar(title = { Text("BhuSeema") }, actions = {
            TextButton(onClick = { vm.sync(); sc.launch { sb.showSnackbar("Sync queued. Set a server in Settings if nothing uploads.") } }) { Text("Sync") }
            TextButton(onClick = { nc.navigate("settings") }) { Text("Settings") } }) },
        snackbarHost = { SnackbarHost(sb) },
        floatingActionButton = { FloatingActionButton(onClick = { add = true }) { Icon(Icons.Default.Add, "Add parcel") } }
    ) { pad ->
        Box(Modifier.padding(pad).fillMaxSize()) {
            when {
                list == null -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                list!!.isEmpty() -> Text("No parcels yet. Tap + to add one.", Modifier.align(Alignment.Center).padding(24.dp))
                else -> LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(list!!, key = { it.id }) { p ->
                        Card(Modifier.fillMaxWidth(), onClick = { nc.navigate("survey/${p.id}") }) {
                            Column(Modifier.padding(16.dp)) {
                                Text("Survey no. ${p.surveyNo}", style = MaterialTheme.typography.titleMedium)
                                Text("${p.village} | RoR area ${"%.0f".format(p.rorAreaSqm)} sq m")
                                Text(if (p.synced) "Synced" else "Not synced", style = MaterialTheme.typography.labelMedium)
                                TextButton(onClick = { vm.delete(p.id) }) { Text("Delete") }
                            }
                        }
                    }
                }
            }
        }
    }
    if (add) AddParcel({ add = false }) { s, v, a -> vm.addParcel(s, v, a); add = false }
}

@Composable fun AddParcel(close: () -> Unit, save: (String, String, Double) -> Unit) {
    var s by remember { mutableStateOf("") }; var v by remember { mutableStateOf("") }; var a by remember { mutableStateOf("") }
    val area = a.toDoubleOrNull()
    AlertDialog(onDismissRequest = close, title = { Text("New parcel") },
        text = { Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(s, { s = it }, label = { Text("Survey number") }, singleLine = true)
            OutlinedTextField(v, { v = it }, label = { Text("Village") }, singleLine = true)
            OutlinedTextField(a, { a = it }, label = { Text("RoR area (sq m)") }, singleLine = true, isError = a.isNotEmpty() && (area == null || area <= 0),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)) } },
        confirmButton = { TextButton(enabled = s.isNotBlank() && area != null && area > 0, onClick = { save(s.trim(), v.trim(), area!!) }) { Text("Add") } },
        dismissButton = { TextButton(onClick = close) { Text("Cancel") } })
}

@Composable fun SurveyScreen(vm: VM, nc: NavHostController, id: Long) {
    val ctx = LocalContext.current
    val parcel by vm.parcel(id).collectAsState(null)
    val corners by vm.corners(id).collectAsState(emptyList())
    val fix by vm.rover.fix.collectAsState(); val status by vm.rover.status.collectAsState()
    var pick by remember { mutableStateOf(false) }
    var owner by rememberSaveable { mutableStateOf("") }; var neigh by rememberSaveable { mutableStateOf("") }
    var gcp by rememberSaveable { mutableStateOf(false) }; var msg by remember { mutableStateOf("") }
    val p = parcel
    if (p == null) { Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }; return }
    val area = Geo.area(corners); val per = Geo.perimeter(corners)
    val ok = gateOk(fix) && owner.isNotBlank() && neigh.isNotBlank()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Survey no. ${p.surveyNo}, ${p.village}", style = MaterialTheme.typography.titleLarge)
        Card { Column(Modifier.padding(16.dp)) {
            Text("Rover: $status"); Text(fix?.let { "${qualityName(it.quality)} | HDOP ${"%.1f".format(it.hdop)} | ${it.sats} sats" } ?: "No fix")
            Text(gateMsg(fix), color = if (gateOk(fix)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { pick = true }) { Text("Connect rover") } } }
        Card { Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Witness confirmation", style = MaterialTheme.typography.titleMedium)
            OutlinedTextField(owner, { owner = it }, label = { Text("Owner name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(neigh, { neigh = it }, label = { Text("Neighbour name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(verticalAlignment = Alignment.CenterVertically) { Checkbox(gcp, { gcp = it }); Text("Tag as ground control point") }
            Button(enabled = ok, modifier = Modifier.fillMaxWidth(), onClick = {
                vm.capture(id, corners.size + 1, fix!!, gcp, owner.trim(), neigh.trim()); msg = "Corner ${corners.size + 1} saved" }) { Text("Capture corner ${corners.size + 1}") }
            if (msg.isNotEmpty()) Text(msg, color = MaterialTheme.colorScheme.primary) } }
        Text("Corners (${corners.size})", style = MaterialTheme.typography.titleMedium)
        if (corners.isEmpty()) Text("No corners captured yet.")
        corners.forEach { c -> Card(Modifier.fillMaxWidth()) { Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text("#${c.seq}${if (c.isGcp) " (GCP)" else ""}  ${"%.7f".format(c.lat)}, ${"%.7f".format(c.lon)}")
                Text("${qualityName(c.quality)} | HDOP ${"%.1f".format(c.hdop)} | ${c.ownerWitness} / ${c.neighbourWitness}", style = MaterialTheme.typography.labelMedium) }
            TextButton(onClick = { vm.removeCorner(c) }) { Text("Remove") } } } }
        if (corners.size >= 3) Card { Column(Modifier.padding(16.dp)) {
            Text("Summary", style = MaterialTheme.typography.titleMedium)
            Text("Measured area: ${"%.1f".format(area)} sq m | Perimeter: ${"%.1f".format(per)} m")
            val diff = (area - p.rorAreaSqm) / p.rorAreaSqm * 100
            Text("Difference from RoR: ${"%.1f".format(diff)}%", color = if (kotlin.math.abs(diff) > 5) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface)
            if (kotlin.math.abs(diff) > 5) Text("Flag: area differs from the record by more than 5%. Re-check corners or refer to the officer.") } }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(enabled = corners.size >= 3, onClick = {
                ctx.startActivity(Intent.createChooser(Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, Export.json(p, corners)), "Export GeoJSON")) }) { Text("Export") }
            OutlinedButton(enabled = corners.isNotEmpty(), onClick = { nc.navigate("stakeout/$id") }) { Text("Stakeout") }
            OutlinedButton(onClick = { vm.sync(); msg = "Sync queued" }) { Text("Sync") } }
    }
    if (pick) { val devs = remember { vm.rover.paired() }
        AlertDialog(onDismissRequest = { pick = false }, title = { Text("Paired devices") },
            text = { if (devs.isEmpty()) Text("No paired Bluetooth devices. Pair the rover in Android settings first.")
                else Column { devs.forEach { d -> TextButton(onClick = { pick = false; vm.connect(d) }) {
                    Text(try { d.name ?: d.address } catch (e: SecurityException) { d.address }) } } } },
            confirmButton = { TextButton(onClick = { pick = false }) { Text("Close") } }) }
}

@Composable fun StakeoutScreen(vm: VM, id: Long) {
    val corners by vm.corners(id).collectAsState(emptyList()); val fix by vm.rover.fix.collectAsState()
    var sel by remember { mutableStateOf<Corner?>(null) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Stakeout", style = MaterialTheme.typography.titleLarge)
        Text("Pick a saved corner, then walk until the distance reaches zero.")
        if (corners.isEmpty()) Text("No saved corners to stake out.")
        corners.forEach { c -> OutlinedButton(onClick = { sel = c }) { Text("Corner #${c.seq}") } }
        val t = sel; val f = fix
        if (t != null) Card(Modifier.fillMaxWidth()) { Column(Modifier.padding(16.dp)) {
            if (f == null) Text("Waiting for rover position...") else {
                Text("Distance: ${"%.2f".format(Geo.dist(f.lat, f.lon, t.lat, t.lon))} m", style = MaterialTheme.typography.headlineMedium)
                Text("Bearing to target: ${"%.0f".format(Geo.bearing(f.lat, f.lon, t.lat, t.lon))} degrees from north")
                Text("Position quality: ${qualityName(f.quality)}") } } }
    }
}

@Composable fun SettingsScreen() {
    val c = LocalContext.current; val pr = remember { Secure.prefs(c) }
    var url by remember { mutableStateOf(pr.getString("url", "") ?: "") }; var tok by remember { mutableStateOf(pr.getString("token", "") ?: "") }
    var saved by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Server settings", style = MaterialTheme.typography.titleLarge)
        OutlinedTextField(url, { url = it; saved = false }, label = { Text("Server URL (https only)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
            isError = url.isNotEmpty() && !url.startsWith("https://"))
        OutlinedTextField(tok, { tok = it; saved = false }, label = { Text("Access token") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        Button(enabled = url.startsWith("https://"), onClick = { pr.edit().putString("url", url.trim()).putString("token", tok.trim()).apply(); saved = true }) { Text("Save") }
        if (saved) Text("Saved (stored encrypted on this device).", color = MaterialTheme.colorScheme.primary)
        Text("Without a server the app works fully offline; use Export to share GeoJSON.")
    }
}
