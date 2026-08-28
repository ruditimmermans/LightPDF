package com.light.lighpdf

import android.app.Application
import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfViewerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = UserPreferencesRepository(application)

    val userPreferencesFlow: StateFlow<UserPreferences> = repository.userPreferencesFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = UserPreferences(isDarkMode = false, isContinuousMode = false)
        )

    val pages = mutableStateListOf<Bitmap>()
    var currentUri: Uri? = null

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
        currentUri = uri
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val contentResolver = context.contentResolver
                val pfd = contentResolver.openFileDescriptor(uri, "r") ?: return@launch
                val renderer = PdfRenderer(pfd)
                
                val bitmapList = mutableListOf<Bitmap>()
                for (i in 0 until renderer.pageCount) {
                    val page = renderer.openPage(i)
                    // Higher quality scale
                    val width = page.width * 4
                    val height = page.height * 4
                    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(android.graphics.Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    bitmapList.add(bitmap)
                    page.close()
                }
                renderer.close()
                pfd.close()

                withContext(Dispatchers.Main) {
                    pages.clear()
                    pages.addAll(bitmapList)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun savePdf(context: Context, destUri: Uri) {
        val sourceUri = currentUri ?: return
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
}
