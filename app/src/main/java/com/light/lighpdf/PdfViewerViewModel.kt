package com.light.lighpdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PdfViewerViewModel : ViewModel() {
    val pages = mutableStateListOf<Bitmap>()
    var currentUri: Uri? = null

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
                    val width = page.width * 2
                    val height = page.height * 2
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
