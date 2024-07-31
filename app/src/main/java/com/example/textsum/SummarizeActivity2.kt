package com.example.textsum

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
    import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.BufferedReader

class SummarizeActivity2 : ComponentActivity() {
    private var content = ""
    private lateinit var viewModel: SummarizeViewModel
    private lateinit var promptEditText: TextView
    private lateinit var summarizeButton: Button
    lateinit var outputTextView: TextView
    private lateinit var extractPDFBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summarise)

        viewModel = ViewModelProvider(this).get(SummarizeViewModel::class.java)
       // extractPDFBtn = findViewById(R.id.idBtnExtract)
//        promptEditText = findViewById(R.id.promptEditText)
        summarizeButton = findViewById(R.id.summarizeButton)
        outputTextView = findViewById(R.id.outputTextView)

        summarizeButton.setOnClickListener {
            val prompt = promptEditText.text.toString()
            if (prompt.isNotBlank()) {
                viewModel.sendPrompt(prompt)
            }
        }

//        extractPDFBtn.setOnClickListener {
//            // Launch the file picker
//            openFilePicker()
//        }

        observeViewModel()
    }

    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                readFile(uri)
            }
        } else {
            Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // Set to the desired mime type, e.g., "application/pdf" for PDF files
        }
        filePickerLauncher.launch(intent)
    }

    private fun readFile(uri: Uri) {
        if (isPdfFile(uri)) {
            importPdf(uri)
        } else {
            val content = readTextFromUri(uri)
            content?.let {
                promptEditText.text = it
                viewModel.sendPrompt(it) // Send text to ViewModel for summarization
            } ?: run {
                Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun isPdfFile(uri: Uri): Boolean {
        return contentResolver.getType(uri) == "application/pdf"
    }

    private fun readTextFromUri(uri: Uri): String? {
        return try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val reader = BufferedReader(InputStreamReader(inputStream))
                val stringBuilder = StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    stringBuilder.append(line)
                }
                stringBuilder.toString()
            }
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    private fun importPdf(uri: Uri) {
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes: ByteArray = convertToByteArray(inputStream)
                try {
                    var parsedText = ""
                    val reader = PdfReader(bytes)
                    val n = reader.numberOfPages
                    for (i in 0 until n) {
                        parsedText = parsedText + PdfTextExtractor.getTextFromPage(reader, i + 1)
                            .trim { it <= ' ' } + "\n" // Extracting the content from the different pages
                    }
                    Log.e("TAG", "onActivityResult: $parsedText")
                    content = parsedText
                   // promptEditText.text = content // Display the extracted text
                    reader.close()

                    // Send extracted text to ViewModel for summarization
                    viewModel.sendPrompt(parsedText)
                } catch (e: Exception) {
                    println(e)
                    Toast.makeText(this, "Failed to parse PDF", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (exception: IOException) {
            exception.printStackTrace()
            Toast.makeText(this, "Failed to open PDF file", Toast.LENGTH_SHORT).show()
        }
    }

    @Throws(IOException::class)
    fun convertToByteArray(inputStream: InputStream): ByteArray {
        val buffer = ByteArray(8192)
        var bytesRead: Int
        val output = ByteArrayOutputStream()
        while ((inputStream.read(buffer).also { bytesRead = it }) != -1) {
            output.write(buffer, 0, bytesRead)
        }
        return output.toByteArray()
    }




    private fun observeViewModel() {
        lifecycleScope.launch(Dispatchers.Main) {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Success -> outputTextView.text = state.outputText
                    is UiState.Loading -> outputTextView.text = "Loading...."
                    is UiState.Error -> outputTextView.text = state.errorMessage
                    is UiState.Initial -> outputTextView.text = "Result will appear here"
                }
            }
        }
    }
}
