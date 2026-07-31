package com.geepscontrol.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.geepscontrol.pixel.PixelBitmap
import com.geepscontrol.pixel.PixelEngine
import com.geepscontrol.pixel.PixelFitMode
import com.geepscontrol.pixel.PixelFrame
import com.geepscontrol.pixel.PixelLayer
import com.geepscontrol.pixel.PixelProject
import com.geepscontrol.pixel.PixelResizeMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.util.UUID
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

private enum class PixelTool { PENCIL, ERASER, FILL, EYEDROPPER }
private enum class StudioPanel { SOURCE, DRAW, PALETTE, TRANSFORM, FRAMES, LAYERS, EXPORT }

@Composable
actual fun PixelStudioScreen(padding: PaddingValues) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val store = remember { AndroidPixelProjectStore(File(context.filesDir, "pixel-studio")) }
    var projects by remember { mutableStateOf(store.loadAll()) }
    var project by remember { mutableStateOf<PixelProject?>(projects.maxByOrNull { it.updatedAt }) }
    var sourceImage by remember { mutableStateOf<PixelBitmap?>(null) }
    var status by remember { mutableStateOf("Import an image or start a blank sprite.") }
    var showNewDialog by remember { mutableStateOf(false) }
    var showProjects by remember { mutableStateOf(project == null) }
    var pendingExport by remember { mutableStateOf<PixelBitmap?>(null) }
    var pendingProjectExport by remember { mutableStateOf<String?>(null) }
    var importIntoCurrent by remember { mutableStateOf(false) }
    val latestProject by rememberUpdatedState(project)

    DisposableEffect(Unit) {
        onDispose { latestProject?.let(store::save) }
    }

    val importLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                runCatching {
                    context.contentResolver.openInputStream(it)?.use(BitmapFactory::decodeStream)?.toPixelBitmap()
                        ?: error("The selected image could not be decoded.")
                }.onSuccess { image ->
                    sourceImage = image
                    if (importIntoCurrent && project != null) {
                        status = "Loaded a new ${image.width}×${image.height} source. Adjust settings, then tap Pixelify original."
                    } else {
                        val created =
                            newProject(
                                name = "Sprite ${projects.size + 1}",
                                width = minOf(32, image.width),
                                height = minOf(32, image.height),
                            )
                        val pixelified = PixelEngine.pixelify(image, created.width, created.height, 16, false, PixelFitMode.CONTAIN)
                        project = created.withActivePixels(pixelified).copy(palette = PixelEngine.palette(pixelified), updatedAt = System.currentTimeMillis())
                        status = "Imported ${image.width}×${image.height}. Adjust Pixelify settings to refine it."
                    }
                    importIntoCurrent = false
                    showProjects = false
                }.onFailure {
                    importIntoCurrent = false
                    status = it.message ?: "Import failed."
                }
            }
        }
    val exportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("image/png")) { uri ->
            val image = pendingExport
            if (uri != null && image != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.use { stream ->
                        image.toAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, stream)
                    } ?: error("Could not open the export destination.")
                }.onSuccess { status = "Exported ${image.width}×${image.height} PNG." }
                    .onFailure { status = it.message ?: "Export failed." }
            }
            pendingExport = null
        }
    val projectImportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri?.let {
                runCatching {
                    val content = context.contentResolver.openInputStream(it)?.bufferedReader()?.use { reader -> reader.readText() } ?: error("Project file was empty.")
                    store.decode(content).copy(id = UUID.randomUUID().toString(), updatedAt = System.currentTimeMillis())
                }.onSuccess { imported ->
                    store.save(imported)
                    projects = store.loadAll()
                    project = imported
                    sourceImage = null
                    showProjects = false
                    status = "Imported editable project \"${imported.name}\"."
                }.onFailure { status = it.message ?: "Project import failed." }
            }
        }
    val projectExportLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
            val content = pendingProjectExport
            if (uri != null && content != null) {
                runCatching {
                    context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(content) } ?: error("Could not open the export destination.")
                }.onSuccess { status = "Exported editable Pixel Studio project." }
                    .onFailure { status = it.message ?: "Project export failed." }
            }
            pendingProjectExport = null
        }

    LaunchedEffect(project) {
        val value = project ?: return@LaunchedEffect
        delay(350)
        store.save(value)
        projects = store.loadAll()
    }

    if (showProjects || project == null) {
        PixelProjectLibrary(
            projects = projects,
            padding = padding,
            onOpen = {
                project = it
                showProjects = false
                sourceImage = null
            },
            onImport = {
                importIntoCurrent = false
                importLauncher.launch("image/*")
            },
            onNew = { showNewDialog = true },
            onImportProject = { projectImportLauncher.launch("application/json") },
            onDelete = {
                store.delete(it.id)
                projects = store.loadAll()
                if (project?.id == it.id) project = null
            },
        )
    } else {
        PixelEditor(
            project = project!!,
            sourceImage = sourceImage,
            padding = padding,
            status = status,
            onProject = { project = it.copy(updatedAt = System.currentTimeMillis()) },
            onStatus = { status = it },
            onImport = {
                importIntoCurrent = true
                importLauncher.launch("image/*")
            },
            onProjects = { showProjects = true },
            onExport = { image, name ->
                pendingExport = image
                exportLauncher.launch(name)
            },
            onExportProject = { value ->
                pendingProjectExport = store.encode(value)
                projectExportLauncher.launch("${value.safeName()}.life-pixel.json")
            },
        )
    }

    if (showNewDialog) {
        NewPixelProjectDialog(
            onDismiss = { showNewDialog = false },
            onCreate = { name, width, height ->
                project = newProject(name, width, height)
                sourceImage = null
                showProjects = false
                showNewDialog = false
            },
        )
    }
}

@Composable
private fun PixelProjectLibrary(
    projects: List<PixelProject>,
    padding: PaddingValues,
    onOpen: (PixelProject) -> Unit,
    onImport: () -> Unit,
    onNew: () -> Unit,
    onImportProject: () -> Unit,
    onDelete: (PixelProject) -> Unit,
) {
    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Text("Pixel Studio", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Text("Convert images, edit pixel art, build animation frames, and export sprites or sheets.")
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Import image") }
                OutlinedButton(onClick = onNew, modifier = Modifier.weight(1f)) { Text("Blank sprite") }
            }
            OutlinedButton(onClick = onImportProject, modifier = Modifier.fillMaxWidth()) { Text("Import editable project") }
        }
        if (projects.isEmpty()) {
            item { Text("No saved pixel projects yet.") }
        } else {
            itemsIndexed(projects, key = { _, value -> value.id }) { _, value ->
                Card(Modifier.fillMaxWidth().clickable { onOpen(value) }) {
                    Row(Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(value.name, fontWeight = FontWeight.SemiBold)
                            Text("${value.width}×${value.height} · ${value.frames.size} frame${if (value.frames.size == 1) "" else "s"}")
                        }
                        TextButton(onClick = { onDelete(value) }) { Text("Delete", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewPixelProjectDialog(onDismiss: () -> Unit, onCreate: (String, Int, Int) -> Unit) {
    var name by remember { mutableStateOf("New sprite") }
    var width by remember { mutableStateOf("32") }
    var height by remember { mutableStateOf("32") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Blank pixel project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Project name") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(width, { width = it.filter(Char::isDigit).take(3) }, label = { Text("Width") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                    OutlinedTextField(height, { height = it.filter(Char::isDigit).take(3) }, label = { Text("Height") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number))
                }
            }
        },
        confirmButton = {
            Button(onClick = { onCreate(name.ifBlank { "New sprite" }, (width.toIntOrNull() ?: 32).coerceIn(1, 512), (height.toIntOrNull() ?: 32).coerceIn(1, 512)) }) { Text("Create") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

@Composable
private fun PixelEditor(
    project: PixelProject,
    sourceImage: PixelBitmap?,
    padding: PaddingValues,
    status: String,
    onProject: (PixelProject) -> Unit,
    onStatus: (String) -> Unit,
    onImport: () -> Unit,
    onProjects: () -> Unit,
    onExport: (PixelBitmap, String) -> Unit,
    onExportProject: (PixelProject) -> Unit,
) {
    var panel by remember { mutableStateOf(StudioPanel.SOURCE) }
    var tool by remember { mutableStateOf(PixelTool.PENCIL) }
    var selectedColor by remember(project.id) { mutableIntStateOf(project.palette.firstOrNull() ?: 0xff000000.toInt()) }
    var brushSize by remember { mutableIntStateOf(1) }
    var zoom by remember(project.id, project.width, project.height) {
        mutableFloatStateOf((1024f / max(project.width, project.height)).coerceIn(1f, 10f))
    }
    var undo by remember(project.id) { mutableStateOf(emptyList<PixelProject>()) }
    var redo by remember(project.id) { mutableStateOf(emptyList<PixelProject>()) }
    var playing by remember { mutableStateOf(false) }
    var previewFrame by remember { mutableIntStateOf(project.activeFrame) }

    fun commit(updated: PixelProject) {
        undo = (undo + project).takeLast(30)
        redo = emptyList()
        onProject(updated)
    }

    LaunchedEffect(playing, project.frames) {
        while (playing && project.frames.isNotEmpty()) {
            val frame = project.frames[previewFrame.coerceIn(project.frames.indices)]
            delay(frame.durationMillis.coerceIn(20, 5000).toLong())
            previewFrame = (previewFrame + 1) % project.frames.size
        }
    }

    LazyColumn(
        Modifier.fillMaxSize().padding(padding).padding(horizontal = 12.dp),
        contentPadding = PaddingValues(vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = project.name,
                    onValueChange = { onProject(project.copy(name = it.take(80))) },
                    label = { Text("Project name") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                )
                OutlinedButton(onClick = onProjects) { Text("Projects") }
            }
            Text("${project.width}×${project.height} · Frame ${project.activeFrame + 1}/${project.frames.size} · autosaved", style = MaterialTheme.typography.bodySmall)
        }
        item {
            Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StudioPanel.entries.forEach { value ->
                    if (panel == value) Button(onClick = { panel = value }) { Text(value.name.lowercase().replaceFirstChar(Char::uppercase)) }
                    else OutlinedButton(onClick = { panel = value }) { Text(value.name.lowercase().replaceFirstChar(Char::uppercase)) }
                }
            }
        }
        item {
            PixelCanvas(
                project = project,
                frameIndex = if (playing) previewFrame else project.activeFrame,
                tool = tool,
                selectedColor = selectedColor,
                brushSize = brushSize,
                cellSizeDp = zoom,
                onColorPicked = { selectedColor = it },
                onPixelsChanged = { commit(project.withActivePixels(it)) },
            )
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(
                    onClick = {
                        undo.lastOrNull()?.let {
                            redo = (redo + project).takeLast(30)
                            undo = undo.dropLast(1)
                            onProject(it)
                        }
                    },
                    enabled = undo.isNotEmpty(),
                ) { Text("Undo") }
                OutlinedButton(
                    onClick = {
                        redo.lastOrNull()?.let {
                            undo = (undo + project).takeLast(30)
                            redo = redo.dropLast(1)
                            onProject(it)
                        }
                    },
                    enabled = redo.isNotEmpty(),
                ) { Text("Redo") }
                Text("Zoom")
                Slider(zoom, { zoom = it }, valueRange = 1f..24f, modifier = Modifier.weight(1f))
            }
        }
        item {
            when (panel) {
                StudioPanel.SOURCE ->
                    PixelifyControls(project, sourceImage, onImport, onCommit = ::commit, onStatus = onStatus)
                StudioPanel.DRAW ->
                    DrawControls(project, tool, selectedColor, brushSize, onTool = { tool = it }, onColor = { selectedColor = it }, onBrush = { brushSize = it }, onCommit = ::commit)
                StudioPanel.PALETTE ->
                    PaletteControls(project, selectedColor, onColor = { selectedColor = it }, onCommit = ::commit)
                StudioPanel.TRANSFORM ->
                    TransformControls(project, onCommit = ::commit)
                StudioPanel.FRAMES ->
                    FrameControls(project, playing, onPlaying = { playing = it; previewFrame = project.activeFrame }, onCommit = ::commit, onProject = onProject)
                StudioPanel.LAYERS ->
                    LayerControls(project, onCommit = ::commit, onProject = onProject)
                StudioPanel.EXPORT ->
                    ExportControls(project, onExport, onExportProject)
            }
        }
        item { Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
    }
}

@Composable
private fun PixelCanvas(
    project: PixelProject,
    frameIndex: Int,
    tool: PixelTool,
    selectedColor: Int,
    brushSize: Int,
    cellSizeDp: Float,
    onColorPicked: (Int) -> Unit,
    onPixelsChanged: (PixelBitmap) -> Unit,
) {
    val frame = project.frames[frameIndex.coerceIn(project.frames.indices)]
    val flattened = remember(frame, project.width, project.height) { PixelEngine.flatten(frame, project.width, project.height) }
    val previous =
        if (project.showOnionSkin && project.frames.size > 1) {
            PixelEngine.flatten(project.frames[(frameIndex - 1 + project.frames.size) % project.frames.size], project.width, project.height)
        } else {
            null
        }
    val activePixels = project.frames[project.activeFrame].layers[project.activeLayer].pixels
    val latestActivePixels by rememberUpdatedState(activePixels)
    val latestPixelsChanged by rememberUpdatedState(onPixelsChanged)
    val latestColorPicked by rememberUpdatedState(onColorPicked)
    val bitmap = remember(flattened.pixels) { flattened.toAndroidBitmap().asImageBitmap() }
    val checker =
        remember(project.width, project.height) {
            PixelBitmap(
                project.width,
                project.height,
                IntArray(project.width * project.height) { index ->
                    val x = index % project.width
                    val y = index / project.width
                    if ((x + y) % 2 == 0) 0xffeeeeee.toInt() else 0xffc8c8c8.toInt()
                },
            ).toAndroidBitmap().asImageBitmap()
        }
    var dragStart by remember { mutableStateOf<IntOffset?>(null) }
    var dragPixels by remember { mutableStateOf(activePixels) }
    val horizontal = rememberScrollState()
    val vertical = rememberScrollState()
    Box(
        Modifier.fillMaxWidth().height(420.dp).background(Color(0xff202124)).horizontalScroll(horizontal).verticalScroll(vertical),
    ) {
        Canvas(
            Modifier
                .size((project.width * cellSizeDp).dp, (project.height * cellSizeDp).dp)
                .pointerInput(project.width, project.height, project.activeFrame, project.activeLayer, tool, selectedColor, brushSize, cellSizeDp, activePixels) {
                    detectTapGestures { offset ->
                        val x = floor(offset.x / cellSizeDp.dp.toPx()).toInt().coerceIn(0, project.width - 1)
                        val y = floor(offset.y / cellSizeDp.dp.toPx()).toInt().coerceIn(0, project.height - 1)
                        val layer = PixelBitmap(project.width, project.height, latestActivePixels)
                        when (tool) {
                            PixelTool.PENCIL -> latestPixelsChanged(PixelEngine.line(layer, x, y, x, y, selectedColor, brushSize))
                            PixelTool.ERASER -> latestPixelsChanged(PixelEngine.line(layer, x, y, x, y, 0, brushSize))
                            PixelTool.FILL -> latestPixelsChanged(PixelEngine.floodFill(layer, x, y, selectedColor))
                            PixelTool.EYEDROPPER -> latestColorPicked(flattened.pixels[y * project.width + x])
                        }
                    }
                }
                .pointerInput(project.width, project.height, project.activeFrame, project.activeLayer, tool, selectedColor, brushSize, cellSizeDp) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            dragPixels = latestActivePixels.copyOf()
                            dragStart =
                                IntOffset(
                                    floor(offset.x / cellSizeDp.dp.toPx()).toInt().coerceIn(0, project.width - 1),
                                    floor(offset.y / cellSizeDp.dp.toPx()).toInt().coerceIn(0, project.height - 1),
                                )
                        },
                        onDragEnd = { dragStart = null },
                        onDragCancel = { dragStart = null },
                    ) { change, _ ->
                        if (tool !in setOf(PixelTool.PENCIL, PixelTool.ERASER)) return@detectDragGestures
                        val end =
                            IntOffset(
                                floor(change.position.x / cellSizeDp.dp.toPx()).toInt().coerceIn(0, project.width - 1),
                                floor(change.position.y / cellSizeDp.dp.toPx()).toInt().coerceIn(0, project.height - 1),
                            )
                        val start = dragStart ?: end
                        val result = PixelEngine.line(PixelBitmap(project.width, project.height, dragPixels), start.x, start.y, end.x, end.y, if (tool == PixelTool.ERASER) 0 else selectedColor, brushSize)
                        dragPixels = result.pixels
                        latestPixelsChanged(result)
                        dragStart = end
                        change.consume()
                    }
                },
        ) {
            drawImage(checker, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.None)
            previous?.let {
                val onion = it.toAndroidBitmap().asImageBitmap()
                drawImage(onion, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), alpha = 0.22f, filterQuality = FilterQuality.None)
            }
            drawImage(bitmap, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = FilterQuality.None)
            if (project.showGrid && cellSizeDp >= 6f) {
                val cellWidth = size.width / project.width
                val cellHeight = size.height / project.height
                repeat(project.width + 1) { x -> drawLine(Color.Black.copy(alpha = 0.28f), Offset(x * cellWidth, 0f), Offset(x * cellWidth, size.height), 1f) }
                repeat(project.height + 1) { y -> drawLine(Color.Black.copy(alpha = 0.28f), Offset(0f, y * cellHeight), Offset(size.width, y * cellHeight), 1f) }
            }
        }
    }
}

@Composable
private fun PixelifyControls(
    project: PixelProject,
    source: PixelBitmap?,
    onImport: () -> Unit,
    onCommit: (PixelProject) -> Unit,
    onStatus: (String) -> Unit,
) {
    var width by remember(project.width) { mutableStateOf(project.width.toString()) }
    var height by remember(project.height) { mutableStateOf(project.height.toString()) }
    var colors by remember { mutableIntStateOf(16) }
    var dither by remember { mutableStateOf(false) }
    var removeBackground by remember { mutableStateOf(false) }
    var backgroundTolerance by remember { mutableIntStateOf(24) }
    var fit by remember { mutableStateOf(PixelFitMode.CONTAIN) }
    var resizeMode by remember { mutableStateOf(PixelResizeMode.SMART) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pixelify & smart resize", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = onImport, modifier = Modifier.weight(1f)) { Text("Import / replace source") }
            Text(source?.let { "${it.width}×${it.height}" } ?: "No original retained", modifier = Modifier.align(Alignment.CenterVertically))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumericField(width, { width = it }, "Width", Modifier.weight(1f))
            NumericField(height, { height = it }, "Height", Modifier.weight(1f))
        }
        Text("Colors: $colors")
        Slider(colors.toFloat(), { colors = it.roundToInt() }, valueRange = 2f..64f, steps = 61)
        ChoiceRow("Fit", PixelFitMode.entries, fit, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) { fit = it }
        ChoiceRow("Resize", PixelResizeMode.entries, resizeMode, { it.name.lowercase().replaceFirstChar(Char::uppercase) }) { resizeMode = it }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Floyd–Steinberg dithering", modifier = Modifier.weight(1f))
            Switch(dither, { dither = it })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Remove connected corner background", modifier = Modifier.weight(1f))
            Switch(removeBackground, { removeBackground = it })
        }
        if (removeBackground) {
            Text("Background tolerance: $backgroundTolerance")
            Slider(backgroundTolerance.toFloat(), { backgroundTolerance = it.roundToInt() }, valueRange = 0f..96f)
        }
        Button(
            onClick = {
                val targetWidth = (width.toIntOrNull() ?: project.width).coerceIn(1, 512)
                val targetHeight = (height.toIntOrNull() ?: project.height).coerceIn(1, 512)
                val original = source ?: PixelEngine.flatten(project.frames[project.activeFrame], project.width, project.height)
                val base = if (removeBackground) PixelEngine.removeCornerBackground(original, backgroundTolerance) else original
                val result =
                    if (source != null) PixelEngine.pixelify(base, targetWidth, targetHeight, colors, dither, fit)
                    else PixelEngine.quantize(PixelEngine.resize(base, targetWidth, targetHeight, resizeMode, fit), colors, dither)
                val resizedFrames =
                    project.frames.mapIndexed { index, frame ->
                        if (index == project.activeFrame) {
                            PixelFrame(frame.name, frame.durationMillis, listOf(PixelLayer("Pixelified", pixels = result.pixels)))
                        } else {
                            frame.copy(layers = frame.layers.map { layer -> layer.copy(pixels = PixelEngine.resize(PixelBitmap(project.width, project.height, layer.pixels), targetWidth, targetHeight, resizeMode).pixels) })
                        }
                    }
                onCommit(project.copy(width = targetWidth, height = targetHeight, frames = resizedFrames, activeLayer = 0, palette = PixelEngine.palette(result)))
                onStatus("Pixelified to $targetWidth×$targetHeight with ${PixelEngine.palette(result).size} colors.")
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text(if (source == null) "Smart resize current sprite" else "Pixelify original") }
    }
}

@Composable
private fun DrawControls(
    project: PixelProject,
    tool: PixelTool,
    selectedColor: Int,
    brushSize: Int,
    onTool: (PixelTool) -> Unit,
    onColor: (Int) -> Unit,
    onBrush: (Int) -> Unit,
    onCommit: (PixelProject) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Pixel tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        ChoiceRow("Tool", PixelTool.entries, tool, { it.name.lowercase().replaceFirstChar(Char::uppercase) }, onTool)
        Text("Brush size: $brushSize")
        Slider(brushSize.toFloat(), { onBrush(it.roundToInt()) }, valueRange = 1f..7f, steps = 5)
        ColorEditor(selectedColor, onColor)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Grid", modifier = Modifier.weight(1f))
            Switch(project.showGrid, { onCommit(project.copy(showGrid = it)) })
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Onion skin previous frame", modifier = Modifier.weight(1f))
            Switch(project.showOnionSkin, { onCommit(project.copy(showOnionSkin = it)) })
        }
        Button(
            onClick = {
                val layer = PixelBitmap(project.width, project.height, project.frames[project.activeFrame].layers[project.activeLayer].pixels)
                onCommit(project.withActivePixels(PixelEngine.outline(layer, selectedColor)))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Add 1-pixel outline in selected color") }
    }
}

@Composable
private fun PaletteControls(project: PixelProject, selectedColor: Int, onColor: (Int) -> Unit, onCommit: (PixelProject) -> Unit) {
    var replacement by remember { mutableIntStateOf(selectedColor) }
    var hue by remember { mutableFloatStateOf(0f) }
    var saturation by remember { mutableFloatStateOf(1f) }
    var brightness by remember { mutableFloatStateOf(1f) }
    var contrast by remember { mutableFloatStateOf(1f) }
    val flat = PixelEngine.flatten(project.frames[project.activeFrame], project.width, project.height)
    val activeLayer = PixelBitmap(project.width, project.height, project.frames[project.activeFrame].layers[project.activeLayer].pixels)
    val palette = PixelEngine.palette(flat, 64)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Palette & color variants", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            palette.forEach { color ->
                Box(
                    Modifier.size(38.dp).background(Color(color), RoundedCornerShape(6.dp))
                        .border(if (color == selectedColor) 3.dp else 1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(6.dp))
                        .clickable { onColor(color); replacement = color },
                )
            }
        }
        Text("Replacement color")
        ColorEditor(replacement) { replacement = it }
        Button(
            onClick = {
                val updated = PixelEngine.replaceColor(activeLayer, selectedColor, replacement)
                onCommit(project.withActivePixels(updated).copy(palette = PixelEngine.palette(updated)))
                onColor(replacement)
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Replace selected color") }
        Text("Hue: ${hue.roundToInt()}°")
        Slider(hue, { hue = it }, valueRange = -180f..180f)
        Text("Saturation: ${"%.2f".format(saturation)}")
        Slider(saturation, { saturation = it }, valueRange = 0f..2f)
        Text("Brightness: ${"%.2f".format(brightness)}")
        Slider(brightness, { brightness = it }, valueRange = 0.2f..2f)
        Text("Contrast: ${"%.2f".format(contrast)}")
        Slider(contrast, { contrast = it }, valueRange = 0.2f..2f)
        Button(
            onClick = {
                val adjusted = PixelEngine.adjustColor(activeLayer, hue, saturation, brightness, contrast)
                onCommit(project.withActivePixels(adjusted).copy(palette = PixelEngine.palette(adjusted)))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Create color variant") }
    }
}

@Composable
private fun TransformControls(project: PixelProject, onCommit: (PixelProject) -> Unit) {
    var angle by remember { mutableFloatStateOf(0f) }
    val active = PixelEngine.flatten(project.frames[project.activeFrame], project.width, project.height)
    fun apply(transform: (PixelBitmap) -> PixelBitmap) {
        var outputWidth = project.width
        var outputHeight = project.height
        val frames =
            project.frames.map { frame ->
                frame.copy(
                    layers =
                        frame.layers.map { layer ->
                            val result = transform(PixelBitmap(project.width, project.height, layer.pixels))
                            outputWidth = result.width
                            outputHeight = result.height
                            layer.copy(pixels = result.pixels)
                        },
                )
            }
        onCommit(project.copy(width = outputWidth, height = outputHeight, frames = frames))
    }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Transform & rotations", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { apply { PixelEngine.rotate90(it, false) } }, modifier = Modifier.weight(1f)) { Text("↶ 90°") }
            OutlinedButton(onClick = { apply { PixelEngine.rotate90(it, true) } }, modifier = Modifier.weight(1f)) { Text("↷ 90°") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = { apply { PixelEngine.flip(it, true) } }, modifier = Modifier.weight(1f)) { Text("Flip horizontal") }
            OutlinedButton(onClick = { apply { PixelEngine.flip(it, false) } }, modifier = Modifier.weight(1f)) { Text("Flip vertical") }
        }
        Text("Arbitrary angle: ${angle.roundToInt()}°")
        Slider(angle, { angle = it }, valueRange = -180f..180f)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { apply { PixelEngine.rotate(it, angle, false) } }, modifier = Modifier.weight(1f)) { Text("Rotate in frame") }
            OutlinedButton(onClick = { apply { PixelEngine.rotate(it, angle, true) } }, modifier = Modifier.weight(1f)) { Text("Rotate & expand") }
        }
        Button(
            onClick = {
                val frames =
                    (0 until 8).map { index ->
                        val rotated = PixelEngine.rotate(active, index * 45f, false)
                        PixelFrame("Direction ${index + 1}", project.frames[project.activeFrame].durationMillis, listOf(PixelLayer("Direction", pixels = rotated.pixels)))
                    }
                onCommit(project.copy(frames = frames, activeFrame = 0, activeLayer = 0))
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Generate 8-direction frame set") }
        Text("Tip: duplicate the frame, rotate it, then touch up pixels to build directional variants.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun FrameControls(
    project: PixelProject,
    playing: Boolean,
    onPlaying: (Boolean) -> Unit,
    onCommit: (PixelProject) -> Unit,
    onProject: (PixelProject) -> Unit,
) {
    val frame = project.frames[project.activeFrame]
    var duration by remember(project.activeFrame, frame.durationMillis) { mutableStateOf(frame.durationMillis.toString()) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Animation frames", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            project.frames.forEachIndexed { index, value ->
                if (index == project.activeFrame) Button(onClick = {}) { Text("${index + 1}: ${value.name}") }
                else OutlinedButton(onClick = { onProject(project.copy(activeFrame = index, activeLayer = 0)) }) { Text("${index + 1}: ${value.name}") }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val duplicate = frame.copy(name = "Frame ${project.frames.size + 1}", layers = frame.layers.map { it.copy(pixels = it.pixels.copyOf()) })
                    onCommit(project.copy(frames = project.frames + duplicate, activeFrame = project.frames.size, activeLayer = 0))
                },
                modifier = Modifier.weight(1f),
            ) { Text("Duplicate") }
            OutlinedButton(
                onClick = {
                    val blank = PixelFrame("Frame ${project.frames.size + 1}", layers = listOf(PixelLayer(pixels = IntArray(project.width * project.height))))
                    onCommit(project.copy(frames = project.frames + blank, activeFrame = project.frames.size, activeLayer = 0))
                },
                modifier = Modifier.weight(1f),
            ) { Text("Blank") }
            OutlinedButton(
                onClick = {
                    if (project.frames.size > 1) {
                        val frames = project.frames.toMutableList().also { it.removeAt(project.activeFrame) }
                        onCommit(project.copy(frames = frames, activeFrame = project.activeFrame.coerceAtMost(frames.lastIndex), activeLayer = 0))
                    }
                },
                enabled = project.frames.size > 1,
            ) { Text("Delete") }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            NumericField(duration, { duration = it }, "Frame ms", Modifier.weight(1f))
            Button(onClick = {
                val frames = project.frames.toMutableList()
                frames[project.activeFrame] = frame.copy(durationMillis = (duration.toIntOrNull() ?: 120).coerceIn(20, 5000))
                onCommit(project.copy(frames = frames))
            }) { Text("Set") }
            OutlinedButton(onClick = { onPlaying(!playing) }) { Text(if (playing) "Stop" else "Preview") }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    if (project.activeFrame > 0) {
                        val frames = project.frames.toMutableList()
                        val moved = frames.removeAt(project.activeFrame)
                        frames.add(project.activeFrame - 1, moved)
                        onCommit(project.copy(frames = frames, activeFrame = project.activeFrame - 1))
                    }
                },
                enabled = project.activeFrame > 0,
                modifier = Modifier.weight(1f),
            ) { Text("Move left") }
            OutlinedButton(
                onClick = {
                    if (project.activeFrame < project.frames.lastIndex) {
                        val frames = project.frames.toMutableList()
                        val moved = frames.removeAt(project.activeFrame)
                        frames.add(project.activeFrame + 1, moved)
                        onCommit(project.copy(frames = frames, activeFrame = project.activeFrame + 1))
                    }
                },
                enabled = project.activeFrame < project.frames.lastIndex,
                modifier = Modifier.weight(1f),
            ) { Text("Move right") }
        }
    }
}

@Composable
private fun LayerControls(project: PixelProject, onCommit: (PixelProject) -> Unit, onProject: (PixelProject) -> Unit) {
    val frame = project.frames[project.activeFrame]
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Layers", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        frame.layers.reversed().forEachIndexed { reverseIndex, layer ->
            val index = frame.layers.lastIndex - reverseIndex
            Card(Modifier.fillMaxWidth().clickable { onProject(project.copy(activeLayer = index)) }) {
                Column(Modifier.padding(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = layer.visible,
                            onCheckedChange = {
                                val layers = frame.layers.toMutableList()
                                layers[index] = layer.copy(visible = it)
                                onCommit(project.withFrame(frame.copy(layers = layers)))
                            },
                        )
                        Text(layer.name, modifier = Modifier.weight(1f), fontWeight = if (index == project.activeLayer) FontWeight.Bold else FontWeight.Normal)
                        Text("${(layer.opacity * 100).roundToInt()}%")
                    }
                    Slider(
                        layer.opacity,
                        {
                            val layers = frame.layers.toMutableList()
                            layers[index] = layer.copy(opacity = it)
                            onProject(project.withFrame(frame.copy(layers = layers)))
                        },
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = {
                    val layers = frame.layers + PixelLayer("Layer ${frame.layers.size + 1}", pixels = IntArray(project.width * project.height))
                    onCommit(project.withFrame(frame.copy(layers = layers)).copy(activeLayer = layers.lastIndex))
                },
                modifier = Modifier.weight(1f),
            ) { Text("Add layer") }
            OutlinedButton(
                onClick = {
                    val selected = frame.layers[project.activeLayer]
                    val layers = frame.layers + selected.copy(name = "${selected.name} copy", pixels = selected.pixels.copyOf())
                    onCommit(project.withFrame(frame.copy(layers = layers)).copy(activeLayer = layers.lastIndex))
                },
                modifier = Modifier.weight(1f),
            ) { Text("Duplicate") }
            OutlinedButton(
                onClick = {
                    if (frame.layers.size > 1) {
                        val layers = frame.layers.toMutableList().also { it.removeAt(project.activeLayer) }
                        onCommit(project.withFrame(frame.copy(layers = layers)).copy(activeLayer = project.activeLayer.coerceAtMost(layers.lastIndex)))
                    }
                },
                enabled = frame.layers.size > 1,
            ) { Text("Delete") }
        }
    }
}

@Composable
private fun ExportControls(project: PixelProject, onExport: (PixelBitmap, String) -> Unit, onExportProject: (PixelProject) -> Unit) {
    var scale by remember { mutableIntStateOf(1) }
    var columns by remember(project.frames.size) { mutableIntStateOf(project.frames.size.coerceAtMost(8)) }
    val frames = project.frames.map { PixelEngine.flatten(it, project.width, project.height) }
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Export", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text("PNG scale: ${scale}× (nearest-neighbor; stays crisp)")
        Slider(scale.toFloat(), { scale = it.roundToInt() }, valueRange = 1f..16f, steps = 14)
        Button(
            onClick = {
                val active = frames[project.activeFrame]
                val output = if (scale == 1) active else PixelEngine.resize(active, active.width * scale, active.height * scale, PixelResizeMode.NEAREST)
                onExport(output, "${project.safeName()}-frame-${project.activeFrame + 1}.png")
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Export active frame PNG") }
        Text("Sprite-sheet columns: $columns")
        Slider(columns.toFloat(), { columns = it.roundToInt() }, valueRange = 1f..project.frames.size.coerceAtLeast(1).toFloat(), steps = (project.frames.size - 2).coerceAtLeast(0))
        Button(
            onClick = {
                var sheet = PixelEngine.spriteSheet(frames, columns)
                if (scale > 1) sheet = PixelEngine.resize(sheet, sheet.width * scale, sheet.height * scale, PixelResizeMode.NEAREST)
                onExport(sheet, "${project.safeName()}-sheet.png")
            },
            modifier = Modifier.fillMaxWidth(),
        ) { Text("Export sprite sheet PNG") }
        OutlinedButton(onClick = { onExportProject(project) }, modifier = Modifier.fillMaxWidth()) { Text("Export editable project") }
        Text("The project remains editable and is autosaved internally. PNG exports preserve transparency.", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun ColorEditor(color: Int, onColor: (Int) -> Unit) {
    var red by remember(color) { mutableIntStateOf(color ushr 16 and 0xff) }
    var green by remember(color) { mutableIntStateOf(color ushr 8 and 0xff) }
    var blue by remember(color) { mutableIntStateOf(color and 0xff) }
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(Modifier.size(48.dp).background(Color(color), RoundedCornerShape(8.dp)).border(1.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(8.dp)))
        Column(Modifier.weight(1f)) {
            Text("R $red")
            Slider(red.toFloat(), { red = it.roundToInt(); onColor((0xff shl 24) or (red shl 16) or (green shl 8) or blue) }, valueRange = 0f..255f)
            Text("G $green")
            Slider(green.toFloat(), { green = it.roundToInt(); onColor((0xff shl 24) or (red shl 16) or (green shl 8) or blue) }, valueRange = 0f..255f)
            Text("B $blue")
            Slider(blue.toFloat(), { blue = it.roundToInt(); onColor((0xff shl 24) or (red shl 16) or (green shl 8) or blue) }, valueRange = 0f..255f)
        }
    }
}

@Composable
private fun <T> ChoiceRow(label: String, values: List<T>, selected: T, text: (T) -> String, onSelect: (T) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, modifier = Modifier.width(72.dp))
        Box {
            OutlinedButton(onClick = { expanded = true }) { Text(text(selected)) }
            DropdownMenu(expanded, { expanded = false }) {
                values.forEach { value ->
                    DropdownMenuItem(text = { Text(text(value)) }, onClick = { onSelect(value); expanded = false })
                }
            }
        }
    }
}

@Composable
private fun NumericField(value: String, onValue: (String) -> Unit, label: String, modifier: Modifier = Modifier) {
    OutlinedTextField(value, { onValue(it.filter(Char::isDigit).take(4)) }, label = { Text(label) }, modifier = modifier, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
}

private fun PixelProject.withActivePixels(bitmap: PixelBitmap): PixelProject {
    val frameIndex = activeFrame.coerceIn(frames.indices)
    val frame = frames[frameIndex]
    val layerIndex = activeLayer.coerceIn(frame.layers.indices)
    val layers = frame.layers.toMutableList()
    layers[layerIndex] = layers[layerIndex].copy(pixels = bitmap.pixels)
    val newFrames = frames.toMutableList()
    newFrames[frameIndex] = frame.copy(layers = layers)
    return copy(width = bitmap.width, height = bitmap.height, frames = newFrames)
}

private fun PixelProject.withFrame(frame: PixelFrame): PixelProject {
    val newFrames = frames.toMutableList()
    newFrames[activeFrame] = frame
    return copy(frames = newFrames)
}

private fun newProject(name: String, width: Int, height: Int): PixelProject =
    PixelProject(
        id = UUID.randomUUID().toString(),
        name = name,
        width = width,
        height = height,
        frames = listOf(PixelFrame(layers = listOf(PixelLayer(pixels = IntArray(width * height))))),
        updatedAt = System.currentTimeMillis(),
    )

private fun PixelProject.safeName(): String =
    name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "sprite" }

private fun Bitmap.toPixelBitmap(): PixelBitmap {
    val maximum = 1024
    val scale = minOf(1.0, maximum.toDouble() / max(width, height))
    val bitmap =
        if (scale < 1.0) {
            Bitmap.createScaledBitmap(this, (width * scale).roundToInt(), (height * scale).roundToInt(), true)
        } else {
            this
        }
    val pixels = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(pixels, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
    return PixelBitmap(bitmap.width, bitmap.height, pixels)
}

private fun PixelBitmap.toAndroidBitmap(): Bitmap =
    Bitmap.createBitmap(pixels, width, height, Bitmap.Config.ARGB_8888)

private class AndroidPixelProjectStore(private val directory: File) {
    private val json = Json { encodeDefaults = true; ignoreUnknownKeys = true }

    init {
        directory.mkdirs()
    }

    fun loadAll(): List<PixelProject> =
        directory.listFiles()
            ?.filter { it.extension == "json" }
            ?.mapNotNull { file -> runCatching { json.decodeFromString<PixelProject>(file.readText()) }.getOrNull() }
            ?.sortedByDescending { it.updatedAt }
            .orEmpty()

    fun save(project: PixelProject) {
        directory.mkdirs()
        val target = File(directory, "${project.id}.json")
        val temporary = File(directory, "${project.id}.json.tmp")
        temporary.writeText(json.encodeToString(project))
        if (target.exists()) target.delete()
        temporary.renameTo(target)
    }

    fun encode(project: PixelProject): String = json.encodeToString(project)

    fun decode(content: String): PixelProject {
        val project = json.decodeFromString<PixelProject>(content)
        require(project.width in 1..512 && project.height in 1..512 && project.frames.isNotEmpty()) { "Unsupported Pixel Studio project." }
        require(project.frames.all { frame -> frame.layers.isNotEmpty() && frame.layers.all { it.pixels.size == project.width * project.height } }) {
            "Pixel Studio project contains invalid layer dimensions."
        }
        return project
    }

    fun delete(id: String) {
        File(directory, "$id.json").delete()
    }
}
