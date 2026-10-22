package com.light.lighpdf

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Numbers
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.core.net.toUri
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.light.lighpdf.ui.theme.LighPDFTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LighPDFTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }
}

@Composable
fun MainScreen(viewModel: PdfViewerViewModel = viewModel()) {
    val context = LocalContext.current
    val pdfState by viewModel.pdfState.collectAsStateWithLifecycle()
    val userPreferences by viewModel.userPreferencesFlow.collectAsStateWithLifecycle()

    var showAboutScreen by remember { mutableStateOf(false) }

    val activity = context as? ComponentActivity
    val currentIntent = activity?.intent

    LaunchedEffect(currentIntent?.data) {
        currentIntent?.data?.let { uri ->
            viewModel.loadPdf(context, uri)
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.loadPdf(context, it) }
    }

    if (showAboutScreen) {
        AboutScreen(onBack = { showAboutScreen = false })
        return
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = pdfState) {
                is PdfDocumentState.Idle -> {
                    LandingScreen(
                        userPreferences = userPreferences,
                        onOpenDocument = { openLauncher.launch(arrayOf("application/pdf")) },
                        onSelectRecentFile = { uriString -> viewModel.loadPdf(context, uriString.toUri()) },
                        onRemoveRecentFile = { uriString -> viewModel.removeRecentFile(uriString) },
                        onClearRecentFiles = { viewModel.clearRecentFiles() },
                        onOpenAbout = { showAboutScreen = true }
                    )
                }

                is PdfDocumentState.Loading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            CircularProgressIndicator(
                                color = Color.White,
                                strokeWidth = 3.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(id = R.string.opening_document),
                                color = Color.Gray,
                                fontSize = 14.sp
                            )
                        }
                    }
                }

                is PdfDocumentState.Error -> {
                    LandingScreen(
                        userPreferences = userPreferences,
                        onOpenDocument = { openLauncher.launch(arrayOf("application/pdf")) },
                        onSelectRecentFile = { uriString -> viewModel.loadPdf(context, uriString.toUri()) },
                        onRemoveRecentFile = { uriString -> viewModel.removeRecentFile(uriString) },
                        onClearRecentFiles = { viewModel.clearRecentFiles() },
                        onOpenAbout = { showAboutScreen = true },
                        errorMessage = stringResource(id = state.messageResId),
                        onDismissError = { viewModel.closePdf() }
                    )
                }

                is PdfDocumentState.Success -> {
                    PdfViewerScreen(
                        state = state,
                        userPreferences = userPreferences,
                        viewModel = viewModel,
                        onClose = { viewModel.closePdf() },
                        onOpenAbout = { showAboutScreen = true }
                    )
                }
            }
        }
    }
}

@Composable
fun LandingScreen(
    userPreferences: UserPreferences,
    onOpenDocument: () -> Unit,
    onSelectRecentFile: (String) -> Unit,
    onRemoveRecentFile: (String) -> Unit,
    onClearRecentFiles: () -> Unit,
    onOpenAbout: () -> Unit,
    errorMessage: String? = null,
    onDismissError: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(onClick = onOpenAbout) {
                Icon(
                    imageVector = Icons.Rounded.Info,
                    contentDescription = stringResource(id = R.string.about),
                    tint = Color.Gray
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Image(
            painter = painterResource(id = R.drawable.ic_launcher_foreground),
            contentDescription = null,
            modifier = Modifier.size(96.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(id = R.string.light_pdf),
            fontSize = 22.sp,
            fontWeight = FontWeight.Light,
            letterSpacing = 3.sp,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onOpenDocument,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            ),
            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = Icons.Rounded.FileOpen,
                contentDescription = null,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(id = R.string.open_document),
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
        }

        if (errorMessage != null) {
            Spacer(modifier = Modifier.height(16.dp))
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF2C1C1C)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFF6B6B),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = errorMessage,
                        color = Color.White,
                        fontSize = 13.sp,
                        modifier = Modifier.weight(1f)
                    )
                    if (onDismissError != null) {
                        IconButton(onClick = onDismissError) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = Color.Gray
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Rounded.History,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(id = R.string.recent_files),
                    color = Color.Gray,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            if (userPreferences.recentFiles.isNotEmpty()) {
                TextButton(onClick = onClearRecentFiles) {
                    Text(
                        text = stringResource(id = R.string.clear_history),
                        color = Color.DarkGray,
                        fontSize = 12.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (userPreferences.recentFiles.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(id = R.string.no_recent_files),
                    color = Color.DarkGray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(userPreferences.recentFiles) { recentFile ->
                    RecentFileItem(
                        recentFile = recentFile,
                        onClick = { onSelectRecentFile(recentFile.uriString) },
                        onRemove = { onRemoveRecentFile(recentFile.uriString) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecentFileItem(
    recentFile: RecentFile,
    onClick: () -> Unit,
    onRemove: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF141414)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xFF222222),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Description,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = recentFile.name,
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                val context = LocalContext.current
                Text(
                    text = formatTimeAgo(context, recentFile.timestamp),
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(28.dp)) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = stringResource(id = R.string.remove),
                    tint = Color.DarkGray,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun PdfViewerScreen(
    state: PdfDocumentState.Success,
    userPreferences: UserPreferences,
    viewModel: PdfViewerViewModel,
    onClose: () -> Unit,
    onOpenAbout: () -> Unit
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val shareTitle = stringResource(id = R.string.share)
    val fileSavedText = stringResource(id = R.string.file_saved)

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    var showControls by remember { mutableStateOf(true) }
    var showJumpDialog by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    val lazyListState = rememberLazyListState()

    val currentVisiblePage by remember {
        derivedStateOf {
            (lazyListState.firstVisibleItemIndex + 1).coerceAtMost(state.pageCount)
        }
    }

    val transformState = rememberTransformableState { zoomChange, offsetChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 5f)
        val newOffset = if (newScale > 1f) {
            val maxX = (containerSize.width * (newScale - 1)) / 2f
            val maxY = (containerSize.height * (newScale - 1)) / 2f
            Offset(
                x = (offset.x + offsetChange.x).coerceIn(-maxX, maxX),
                y = (offset.y + offsetChange.y).coerceIn(-maxY, maxY)
            )
        } else {
            Offset.Zero
        }
        scale = newScale
        offset = newOffset
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let {
            viewModel.savePdf(context, it)
            Toast.makeText(context, fileSavedText, Toast.LENGTH_SHORT).show()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(if (userPreferences.isDarkMode) Color.Black else Color(0xFF222222))
            .onSizeChanged { containerSize = it }
    ) {
        // PDF Pages View
        Box(
            modifier = Modifier
                .fillMaxSize()
                .transformable(state = transformState)
                .pointerInput(Unit) {
                    detectTapGestures(
                        onTap = { showControls = !showControls },
                        onDoubleTap = {
                            if (scale > 1f) {
                                scale = 1f
                                offset = Offset.Zero
                            } else {
                                scale = 2.5f
                            }
                        }
                    )
                }
        ) {
            val colorMatrix = remember(userPreferences.isDarkMode) {
                if (userPreferences.isDarkMode) {
                    ColorMatrix(
                        floatArrayOf(
                            -0.85f, 0f, 0f, 0f, 255f,
                            0f, -0.85f, 0f, 0f, 255f,
                            0f, 0f, -0.85f, 0f, 255f,
                            0f, 0f, 0f, 1f, 0f
                        )
                    )
                } else {
                    ColorMatrix()
                }
            }

            LazyColumn(
                state = lazyListState,
                userScrollEnabled = (scale == 1f),
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y,
                        clip = true
                    )
            ) {
                item {
                    Spacer(modifier = Modifier.height(64.dp))
                }

                itemsIndexed(state.pageSizes) { pageIndex, pageSize ->
                    PdfPageItem(
                        pageIndex = pageIndex,
                        pageSize = pageSize,
                        loader = state.loader,
                        isContinuousMode = userPreferences.isContinuousMode,
                        colorMatrix = colorMatrix,
                        containerWidthPx = if (containerSize.width > 0) containerSize.width else 1080
                    )
                }

                item {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }

        // Top Navigation Bar
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn() + slideInVertically { -it },
            exit = fadeOut() + slideOutVertically { -it },
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(10f)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.92f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onClose) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(id = R.string.back),
                            tint = Color.White
                        )
                    }

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    ) {
                        Text(
                            text = state.fileName,
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(id = R.string.page_indicator, currentVisiblePage, state.pageCount),
                            color = Color.Gray,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    IconButton(onClick = { showJumpDialog = true }) {
                        Icon(
                            imageVector = Icons.Rounded.Numbers,
                            contentDescription = stringResource(id = R.string.go_to_page),
                            tint = Color.White
                        )
                    }

                    IconButton(
                        onClick = {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "application/pdf"
                                putExtra(Intent.EXTRA_STREAM, state.uri)
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, shareTitle))
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Share,
                            contentDescription = stringResource(id = R.string.share),
                            tint = Color.White
                        )
                    }

                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.MoreVert,
                                contentDescription = stringResource(id = R.string.more_options),
                                tint = Color.White
                            )
                        }

                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(Color(0xFF1E1E1E))
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.zoom_in), color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.ZoomIn,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                onClick = {
                                    scale = (scale + 0.5f).coerceAtMost(5f)
                                    showMoreMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.zoom_out), color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.ZoomOut,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                onClick = {
                                    scale = (scale - 0.5f).coerceAtLeast(1f)
                                    if (scale == 1f) offset = Offset.Zero
                                    showMoreMenu = false
                                }
                            )

                            HorizontalDivider(color = Color(0xFF333333))

                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.save_copy), color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.ContentCopy,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                onClick = {
                                    saveLauncher.launch(state.fileName)
                                    showMoreMenu = false
                                }
                            )

                            HorizontalDivider(color = Color(0xFF333333))

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(id = R.string.pdf_dark_mode),
                                        color = if (userPreferences.isDarkMode) Color.Cyan else Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.InvertColors,
                                        contentDescription = null,
                                        tint = if (userPreferences.isDarkMode) Color.Cyan else Color.White
                                    )
                                },
                                onClick = {
                                    viewModel.toggleDarkMode(userPreferences.isDarkMode)
                                    showMoreMenu = false
                                }
                            )

                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = stringResource(id = R.string.continuous_scroll),
                                        color = if (userPreferences.isContinuousMode) Color.Cyan else Color.White
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.ViewStream,
                                        contentDescription = null,
                                        tint = if (userPreferences.isContinuousMode) Color.Cyan else Color.White
                                    )
                                },
                                onClick = {
                                    viewModel.toggleContinuousMode(userPreferences.isContinuousMode)
                                    showMoreMenu = false
                                }
                            )

                            HorizontalDivider(color = Color(0xFF333333))

                            DropdownMenuItem(
                                text = { Text(stringResource(id = R.string.about), color = Color.White) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                },
                                onClick = {
                                    showMoreMenu = false
                                    onOpenAbout()
                                }
                            )
                        }
                    }
                }
                HorizontalDivider(color = Color(0xFF333333), thickness = 0.5.dp)
            }
        }

        // Floating Page Pill
        AnimatedVisibility(
            visible = showControls && state.pageCount > 1,
            enter = fadeIn() + slideInVertically { it },
            exit = fadeOut() + slideOutVertically { it },
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp)
                .zIndex(10f)
        ) {
            Surface(
                shape = CircleShape,
                color = Color.Black.copy(alpha = 0.85f),
                contentColor = Color.White,
                shadowElevation = 6.dp,
                modifier = Modifier.clickable { showJumpDialog = true }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Numbers,
                        contentDescription = null,
                        tint = Color.Cyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(id = R.string.page_indicator, currentVisiblePage, state.pageCount),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        // Jump To Page Dialog
        if (showJumpDialog) {
            var pageInput by remember { mutableStateOf(currentVisiblePage.toString()) }
            AlertDialog(
                onDismissRequest = { showJumpDialog = false },
                title = {
                    Text(
                        text = stringResource(id = R.string.go_to_page),
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                text = {
                    Column {
                        Text(
                            text = stringResource(id = R.string.page_indicator, 1, state.pageCount),
                            fontSize = 13.sp,
                            color = Color.Gray
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = pageInput,
                            onValueChange = { pageInput = it.filter { char -> char.isDigit() } },
                            label = { Text(stringResource(id = R.string.page_number)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.Cyan,
                                unfocusedBorderColor = Color.Gray,
                                focusedLabelColor = Color.Cyan,
                                unfocusedLabelColor = Color.Gray
                            )
                        )
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            val target = pageInput.toIntOrNull()
                            if (target != null && target in 1..state.pageCount) {
                                coroutineScope.launch {
                                    lazyListState.scrollToItem(target - 1)
                                }
                            }
                            showJumpDialog = false
                        }
                    ) {
                        Text(
                            text = stringResource(id = R.string.go),
                            color = Color.Cyan,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showJumpDialog = false }) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            color = Color.White
                        )
                    }
                },
                containerColor = Color(0xFF1C1C1C),
                shape = RoundedCornerShape(16.dp)
            )
        }
    }
}

@Composable
fun AboutScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val versionName = remember {
        try {
            val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            pInfo.versionName ?: "1.1.4"
        } catch (e: Exception) {
            "1.1.4"
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color.Black
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(id = R.string.back),
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = stringResource(id = R.string.about),
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.weight(1f))
                Spacer(modifier = Modifier.width(48.dp))
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                // Header Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF23242A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 28.dp, horizontal = 20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.ic_launcher_foreground),
                            contentDescription = null,
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(id = R.string.app_name),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.version_format, versionName),
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 1: Support & Development
                Text(
                    text = stringResource(id = R.string.support_and_development),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8C9EFF),
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF333540)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column {
                        // Ko-fi row
                        AboutActionRow(
                            icon = Icons.Rounded.OpenInNew,
                            title = stringResource(id = R.string.support_on_kofi),
                            onClick = { openUrl(context, "https://ko-fi.com/ruditimmermans") }
                        )

                        HorizontalDivider(color = Color(0xFF2A2C36), thickness = 1.dp)

                        // Report issues row
                        AboutActionRow(
                            icon = Icons.Rounded.BugReport,
                            title = stringResource(id = R.string.report_issues),
                            onClick = { openUrl(context, "https://github.com/ruditimmermans/LightPDF/issues") }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Section 2: Legal Information
                Text(
                    text = stringResource(id = R.string.legal_information),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF8C9EFF),
                    modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
                )

                Card(
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF333540)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF18191E)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(id = R.string.copyright_notice),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(id = R.string.gpl_license_text),
                            fontSize = 13.sp,
                            color = Color(0xFFB0B3C2),
                            lineHeight = 19.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun AboutActionRow(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(14.dp))
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.weight(1f)
        )
    }
}

fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
        context.startActivity(intent)
    } catch (e: Exception) {
        Toast.makeText(context, context.getString(R.string.could_not_open_browser), Toast.LENGTH_SHORT).show()
    }
}

@Composable
fun PdfPageItem(
    pageIndex: Int,
    pageSize: Pair<Int, Int>,
    loader: PdfPageLoader,
    isContinuousMode: Boolean,
    colorMatrix: ColorMatrix,
    containerWidthPx: Int
) {
    var pageBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val aspectRatio = if (pageSize.first > 0) pageSize.second.toFloat() / pageSize.first.toFloat() else 1.414f

    LaunchedEffect(pageIndex, containerWidthPx) {
        pageBitmap = loader.loadPageBitmap(pageIndex, containerWidthPx)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                vertical = if (isContinuousMode) 0.dp else 6.dp,
                horizontal = if (isContinuousMode) 0.dp else 12.dp
            )
            .aspectRatio(1f / aspectRatio)
            .clip(RoundedCornerShape(if (isContinuousMode) 0.dp else 4.dp))
            .background(Color(0xFF1E1E1E)),
        contentAlignment = Alignment.Center
    ) {
        val bitmap = pageBitmap
        if (bitmap != null && !bitmap.isRecycled) {
            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = stringResource(id = R.string.page_x, pageIndex + 1),
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.FillWidth,
                colorFilter = ColorFilter.colorMatrix(colorMatrix)
            )
        } else {
            CircularProgressIndicator(
                color = Color.Gray,
                strokeWidth = 2.dp,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

fun formatTimeAgo(context: Context, timestamp: Long): String {
    val diff = System.currentTimeMillis() - timestamp
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return when {
        seconds < 60 -> context.getString(R.string.just_now)
        minutes < 60 -> context.getString(R.string.minutes_ago, minutes)
        hours < 24 -> context.getString(R.string.hours_ago, hours)
        days < 7 -> context.getString(R.string.days_ago, days)
        else -> {
            val sdf = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            sdf.format(Date(timestamp))
        }
    }
}
