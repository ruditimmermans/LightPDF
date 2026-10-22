package com.light.lighpdf

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import android.util.LruCache
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import androidx.annotation.StringRes
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface PdfDocumentState {
    object Idle : PdfDocumentState
    data class Loading(val uri: Uri) : PdfDocumentState
    data class Success(
        val uri: Uri,
        val fileName: String,
        val pageCount: Int,
        val pageSizes: List<Pair<Int, Int>>,
        val loader: PdfPageLoader
    ) : PdfDocumentState
    data class Error(val uri: Uri, @param:StringRes val messageResId: Int = R.string.error_loading_pdf) : PdfDocumentState
}

class PdfPageLoader(
    private val context: Context,
    val uri: Uri,
    val fileName: String
) {
    private var pfd: ParcelFileDescriptor? = null
    private var renderer: PdfRenderer? = null
    var pageCount: Int = 0
        private set
    var pageSizes: List<Pair<Int, Int>> = emptyList()
        private set

    private val cache = LruCache<Int, Bitmap>(15)
    private val dispatcher = Dispatchers.IO.limitedParallelism(1)

    suspend fun open(): Boolean = withContext(dispatcher) {
        try {
            val contentResolver = context.contentResolver
            val descriptor = contentResolver.openFileDescriptor(uri, "r") ?: return@withContext false
            pfd = descriptor
            val pdfRenderer = PdfRenderer(descriptor)
            renderer = pdfRenderer
            pageCount = pdfRenderer.pageCount

            val sizes = mutableListOf<Pair<Int, Int>>()
            for (i in 0 until pageCount) {
                val page = pdfRenderer.openPage(i)
                sizes.add(Pair(page.width, page.height))
                page.close()
            }
            pageSizes = sizes
            true
        } catch (e: Exception) {
            e.printStackTrace()
            closeInternal()
            false
        }
    }

    suspend fun loadPageBitmap(pageIndex: Int, targetWidthPx: Int = 1080): Bitmap? = withContext(dispatcher) {
        val cached = cache.get(pageIndex)
        if (cached != null && !cached.isRecycled) {
            return@withContext cached
        }

        val pdfRenderer = renderer ?: return@withContext null
        if (pageIndex !in 0 until pageCount) return@withContext null

        try {
            val page = pdfRenderer.openPage(pageIndex)
            val scale = (targetWidthPx.toFloat() / page.width).coerceAtLeast(1.5f)
            val renderWidth = (page.width * scale).toInt()
            val renderHeight = (page.height * scale).toInt()

            val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(android.graphics.Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            page.close()

            cache.put(pageIndex, bitmap)
            bitmap
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun close() = withContext(dispatcher) {
        closeInternal()
    }

    private fun closeInternal() {
        try {
            cache.evictAll()
            renderer?.close()
            pfd?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            renderer = null
            pfd = null
        }
    }
}

class PdfViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserPreferencesRepository(application)

    val userPreferencesFlow: StateFlow<UserPreferences> = repository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences(isDarkMode = false, isContinuousMode = false)
        )

    private val _pdfState = MutableStateFlow<PdfDocumentState>(PdfDocumentState.Idle)
    val pdfState: StateFlow<PdfDocumentState> = _pdfState.asStateFlow()

    private var activeLoader: PdfPageLoader? = null

    fun toggleDarkMode(currentValue: Boolean) {
        viewModelScope.launch {
            repository.updateDarkMode(!currentValue)
        }
    }

    fun toggleContinuousMode(currentValue: Boolean) {
        viewModelScope.launch {
            repository.updateContinuousMode(!currentValue)
        }
    }

    fun loadPdf(context: Context, uri: Uri) {
        val currentState = _pdfState.value
        if (currentState is PdfDocumentState.Success && currentState.uri == uri) {
            return // Already loaded
        }

        viewModelScope.launch {
            _pdfState.value = PdfDocumentState.Loading(uri)
            
            // Close existing loader
            activeLoader?.close()
            activeLoader = null

            takePersistableUriPermission(context, uri)
            val fileName = getFileNameFromUri(context, uri)

            val loader = PdfPageLoader(context.applicationContext, uri, fileName)
            if (loader.open()) {
                activeLoader = loader
                repository.addRecentFile(uri.toString(), fileName)
                _pdfState.value = PdfDocumentState.Success(
                    uri = uri,
                    fileName = fileName,
                    pageCount = loader.pageCount,
                    pageSizes = loader.pageSizes,
                    loader = loader
                )
            } else {
                _pdfState.value = PdfDocumentState.Error(
                    uri = uri,
                    messageResId = R.string.error_loading_pdf
                )
            }
        }
    }

    fun closePdf() {
        viewModelScope.launch {
            activeLoader?.close()
            activeLoader = null
            _pdfState.value = PdfDocumentState.Idle
        }
    }

    fun removeRecentFile(uriString: String) {
        viewModelScope.launch {
            repository.removeRecentFile(uriString)
        }
    }

    fun clearRecentFiles() {
        viewModelScope.launch {
            repository.clearRecentFiles()
        }
    }

    fun savePdf(context: Context, destUri: Uri) {
        val state = _pdfState.value
        val sourceUri = if (state is PdfDocumentState.Success) state.uri else return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(sourceUri)?.use { input ->
                    context.contentResolver.openOutputStream(destUri)?.use { output ->
                        input.copyTo(output)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun takePersistableUriPermission(context: Context, uri: Uri) {
        if (uri.scheme == "content") {
            try {
                val takeFlags = Intent.FLAG_GRANT_READ_URI_PERMISSION
                context.contentResolver.takePersistableUriPermission(uri, takeFlags)
            } catch (e: Exception) {
                // Ignore if not supported
            }
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (nameIndex != -1) {
                            name = cursor.getString(nameIndex)
                        }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment
        }
        if (name.isNullOrBlank()) {
            name = "Document.pdf"
        }
        return name
    }

    override fun onCleared() {
        super.onCleared()
        viewModelScope.launch {
            activeLoader?.close()
        }
    }
}
