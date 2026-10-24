package com.cinefinder.app.ui

import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.cinefinder.app.model.MatchResult
import com.cinefinder.app.model.ReferenceMovie
import com.cinefinder.app.model.ReferenceRepository
import com.cinefinder.app.processing.VideoAnalyzer
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CineFinderScreen() {
    val context = LocalContext.current
    val repository = remember { ReferenceRepository(context) }
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) }
    var references by remember { mutableStateOf<List<ReferenceMovie>>(emptyList()) }
    var matchResults by remember { mutableStateOf<List<MatchResult>>(emptyList()) }
    var isProcessing by remember { mutableStateOf(false) }
    var progressValue by remember { mutableFloatStateOf(0f) }
    var progressStatus by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { references = repository.getReferences() }

    val videoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            if (references.isEmpty()) {
                errorMessage = "Adiciona primeiro fotogramas à base de referências."
                return@rememberLauncherForActivityResult
            }
            isProcessing = true
            errorMessage = null
            matchResults = emptyList()

            scope.launch {
                try {
                    matchResults = VideoAnalyzer.analyzeVideo(
                        context = context,
                        videoUri = uri,
                        references = references,
                        onProgress = { p, status ->
                            progressValue = p
                            progressStatus = status
                        }
                    )
                } catch (e: Exception) {
                    errorMessage = "Erro no processamento: ${e.localizedMessage ?: "Falha ao ler vídeo."}"
                } finally {
                    isProcessing = false
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("CineFinder", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.primary
                )
            )
        },
        bottomBar = {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.VideoFile, null) },
                    label = { Text("Análise") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.Movie, null) },
                    label = { Text("Referências") }
                )
            }
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            if (selectedTab == 0) {
                Column(modifier = Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { videoPickerLauncher.launch("video/*") },
                        enabled = !isProcessing,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.VideoFile, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Seleccionar Vídeo do Telemóvel")
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    AnimatedVisibility(visible = isProcessing) {
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(progressStatus)
                                Spacer(modifier = Modifier.height(8.dp))
                                LinearProgressIndicator(progress = { progressValue }, modifier = Modifier.fillMaxWidth())
                            }
                        }
                    }

                    errorMessage?.let { error ->
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer), modifier = Modifier.fillMaxWidth()) {
                            Text(text = error, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.padding(12.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    if (matchResults.isNotEmpty()) {
                        LazyColumn(modifier = Modifier.fillMaxSize()) {
                            items(matchResults) { result ->
                                Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                        val bitmap = remember(result.movie.imagePath) { BitmapFactory.decodeFile(result.movie.imagePath) }
                                        if (bitmap != null) {
                                            Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(64.dp).clip(RoundedCornerShape(8.dp)))
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(result.movie.title, fontWeight = FontWeight.Bold)
                                            Text(String.format(Locale.getDefault(), "Semelhança: %.1f%%", result.similarityPercentage), color = MaterialTheme.colorScheme.primary)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                            }
                        }
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    Button(onClick = { showAddDialog = true }, modifier = Modifier.fillMaxWidth().height(52.dp)) {
                        Icon(Icons.Default.Add, null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Adicionar Referência")
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    LazyColumn(modifier = Modifier.fillMaxSize()) {
                        items(references) { item ->
                            Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                                Row(modifier = Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                    val bitmap = remember(item.imagePath) { BitmapFactory.decodeFile(item.imagePath) }
                                    if (bitmap != null) {
                                        Image(bitmap = bitmap.asImageBitmap(), contentDescription = null, modifier = Modifier.size(48.dp).clip(RoundedCornerShape(6.dp)))
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(item.title, modifier = Modifier.weight(1f))
                                    IconButton(onClick = { scope.launch { repository.removeReference(item.id); references = repository.getReferences() } }) {
                                        Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        var title by remember { mutableStateOf("") }
        var selectedUri by remember { mutableStateOf<Uri?>(null) }
        val imagePicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { selectedUri = it }

        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Adicionar Referência") },
            text = {
                Column {
                    OutlinedTextField(value = title, onValueChange = { title = it }, label = { Text("Título") }, modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { imagePicker.launch("image/*") }, modifier = Modifier.fillMaxWidth()) {
                        Text(if (selectedUri != null) "Imagem Escolhida ✓" else "Escolher Imagem")
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = title.isNotBlank() && selectedUri != null,
                    onClick = {
                        scope.launch {
                            context.contentResolver.openInputStream(selectedUri!!)?.use {
                                val b = BitmapFactory.decodeStream(it)
                                if (b != null) { repository.addReference(title, b); references = repository.getReferences() }
                            }
                            showAddDialog = false
                        }
                    }
                ) { Text("Guardar") }
            },
            dismissButton = { TextButton(onClick = { showAddDialog = false }) { Text("Cancelar") } }
        )
    }
}