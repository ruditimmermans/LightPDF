package com.light.lighpdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FileOpen
import androidx.compose.material.icons.rounded.InvertColors
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.ViewStream
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material.icons.rounded.ZoomOut
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.light.lighpdf.ui.theme.LighPDFTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            LighPDFTheme {
                MainScreen(intent = intent)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        setContent {
            LighPDFTheme {
                MainScreen(intent = intent)
            }
        }
    }
}

@Composable
fun MainScreen(viewModel: PdfViewerViewModel = viewModel(), intent: Intent? = null) {
    val context = LocalContext.current
    val userPreferences by viewModel.userPreferencesFlow.collectAsStateWithLifecycle()
    val shareTitle = stringResource(id = R.string.share)

    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    val state = rememberTransformableState { zoomChange, offsetChange, _ ->
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

    LaunchedEffect(intent) {
        intent?.data?.let { uri ->
            viewModel.loadPdf(context, uri)
            // Reset zoom when new PDF is loaded
            scale = 1f
            offset = Offset.Zero
        }
    }
    
    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument(),
    ) { uri: Uri? ->
        uri?.let { 
            viewModel.loadPdf(context, it)
            scale = 1f
            offset = Offset.Zero
        }
    }

    val saveLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        uri?.let { viewModel.savePdf(context, it) }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier.fillMaxSize()
        ) {
            if (viewModel.pages.isEmpty()) {
                // Landing Screen (LightOS Minimalist)
                Column(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.weight(1f))
                    
                    Image(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground),
                        contentDescription = null,
                        modifier = Modifier.size(120.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = stringResource(id = R.string.light_pdf),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Light,
                        letterSpacing = 4.sp,
                        color = Color.White
                    )
                    
                    Spacer(modifier = Modifier.weight(1f))
                    
                    IconButton(
                        onClick = { openLauncher.launch(arrayOf("application/pdf")) },
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.FileOpen,
                            contentDescription = stringResource(id = R.string.open_document),
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                }
            } else {
                // PDF Viewer Screen
                Column(modifier = Modifier.fillMaxSize()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color.Black)
                            .padding(top = innerPadding.calculateTopPadding())
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .zIndex(2f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { 
                                scale = (scale - 0.2f).coerceAtLeast(1f)
                                if (scale == 1f) offset = Offset.Zero
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomOut,
                                contentDescription = stringResource(id = R.string.zoom_out),
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { scale = (scale + 0.2f).coerceAtMost(5f) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ZoomIn,
                                contentDescription = stringResource(id = R.string.zoom_in),
                                tint = Color.White
                            )
                        }

                        Spacer(modifier = Modifier.weight(1f))

                        IconButton(
                            onClick = { saveLauncher.launch("copy.pdf") }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ContentCopy,
                                contentDescription = stringResource(id = R.string.save_copy),
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { 
                                viewModel.currentUri?.let { uri ->
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, shareTitle))
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = stringResource(id = R.string.share),
                                tint = Color.White
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleDarkMode(userPreferences.isDarkMode) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.InvertColors,
                                contentDescription = stringResource(id = R.string.pdf_dark_mode),
                                tint = if (userPreferences.isDarkMode) Color.Cyan else Color.White
                            )
                        }

                        IconButton(
                            onClick = { viewModel.toggleContinuousMode(userPreferences.isContinuousMode) }
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ViewStream,
                                contentDescription = stringResource(id = R.string.continuous_scroll),
                                tint = if (userPreferences.isContinuousMode) Color.Cyan else Color.White
                            )
                        }
                    }
                    
                    HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(if (userPreferences.isDarkMode) Color.Black else Color.DarkGray)
                            .onSizeChanged { containerSize = it }
                            .transformable(state = state)
                    ) {
                        val colorMatrix = remember(userPreferences.isDarkMode) {
                            if (userPreferences.isDarkMode) {
                                ColorMatrix(
                                    floatArrayOf(
                                        -1f, 0f, 0f, 0f, 255f,
                                        0f, -1f, 0f, 0f, 255f,
                                        0f, 0f, -1f, 0f, 255f,
                                        0f, 0f, 0f, 1f, 0f
                                    )
                                )
                            } else {
                                ColorMatrix()
                            }
                        }

                        LazyColumn(
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
                            items(viewModel.pages) { bitmap ->
                                Image(
                                    bitmap = bitmap.asImageBitmap(),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(
                                            vertical = if (userPreferences.isContinuousMode) 0.dp else 8.dp,
                                            horizontal = if (userPreferences.isContinuousMode) 0.dp else 16.dp
                                        ),
                                    contentScale = ContentScale.FillWidth,
                                    colorFilter = ColorFilter.colorMatrix(colorMatrix)
                                )
                            }
                            
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = innerPadding.calculateBottomPadding())
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    IconButton(
                                        onClick = { 
                                            viewModel.pages.clear()
                                            scale = 1f
                                            offset = Offset.Zero
                                        },
                                        modifier = Modifier.background(Color.Black, CircleShape)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.Close,
                                            contentDescription = stringResource(id = R.string.close),
                                            tint = Color.White
                                        )
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
