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
import com.google.firebase.FirebaseApp
import com.itextpdf.text.pdf.PdfReader
import com.itextpdf.text.pdf.parser.PdfTextExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader


class SummarizeActivity : ComponentActivity() {
    private var content = ""
    private lateinit var viewModel: SummarizeViewModel
    private lateinit var promptEditText: TextView
    private lateinit var summarizeButton: Button
    private lateinit var paraphraseButton: Button
    private lateinit var qaButton: Button
    private lateinit var expandButton: Button
    private lateinit var outputTextView: TextView
    private lateinit var extractPDFBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_summarise)
        initViews()
        handleIntent()
        setOnclickListeners()
        // Observe ViewModel state
        observeViewModel()
    }

    private fun handleIntent() {
        var intent : Intent=getIntent();
        if (intent!=null){
            Log.e("TAG", "onCreate: " )
            val uri:Uri? = intent.data
            if (uri != null) {
                readFile(uri)
            } else {
//                Toast.makeText(this, "Failed to get URI", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun setOnclickListeners() {
        // Set button click listeners
        summarizeButton.setOnClickListener {
            val prompt = promptEditText.text.toString()
            if (prompt.isNotBlank()) {
                viewModel.sendPrompt("Please summarise in 5 lines:"+prompt)
            }
        }
        paraphraseButton.setOnClickListener{
            val prompt = promptEditText.text.toString()
            if (prompt.isNotBlank()) {
                viewModel.sendPrompt("Paraphrase the given data:"+prompt)
            }

        }

        qaButton.setOnClickListener{
            val prompt = promptEditText.text.toString()
            if (prompt.isNotBlank()) {
                viewModel.sendPrompt("Create few question and answers using this data :"+prompt)
            }
        }

        expandButton.setOnClickListener{
            val prompt = promptEditText.text.toString()
            if (prompt.isNotBlank()) {
                viewModel.sendPrompt("Expand the given data:"+prompt)
            }

        }

        extractPDFBtn.setOnClickListener {
            // Launch the file picker
            openFilePicker()
        }
    }

    private fun initViews() {

        // Initialize ViewModel and UI components
        viewModel = ViewModelProvider(this).get(SummarizeViewModel::class.java)
        extractPDFBtn = findViewById(R.id.idBtnExtract)
        promptEditText = findViewById(R.id.promptEditText)
        summarizeButton = findViewById(R.id.summarizeButton)
        paraphraseButton =  findViewById(R.id.paraphraseButton)
        qaButton = findViewById(R.id.qaButton)
        expandButton = findViewById(R.id.expandButton)
        outputTextView = findViewById(R.id.outputTextView)
    }

    // File picker launcher
    private val filePickerLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            result.data?.data?.let { uri ->
                readFile(uri)
                Log.d("file picker ",uri.toString())
            }
        } else {
            Toast.makeText(this, "File selection cancelled", Toast.LENGTH_SHORT).show()
        }
    }

    // Open the file picker
    private fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "*/*" // Set to the desired mime type, e.g., "application/pdf" for PDF files
        }
        filePickerLauncher.launch(intent)
    }


    private fun readFile(uri: Uri) {
        Log.e("TAG", "readFile: " )
        when {
            isPdfFile(uri) -> importPdf(uri)
            isTxtFile(uri) -> importTxt(uri)
            else -> {
                val content = readTextFromUri(uri)
                content?.let {
                    promptEditText.text = it
                    viewModel.sendPrompt(it)
                } ?: run {
                    Toast.makeText(this, "Failed to read file", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    private fun readTextFromUri(uri: Uri): String? {
        return try {
            Log.e("TAG", "readTextFromUri: loading ");
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



    private fun isTxtFile(uri: Uri): Boolean {
        return contentResolver.getType(uri)?.startsWith("text/plain") == true
    }

    // Check if the file is a PDF
    private fun isPdfFile(uri: Uri): Boolean {
        Log.e("TAG", "isPdfFile: " )
        return contentResolver.getType(uri) == "application/pdf"
    }

    private fun importTxt(uri: Uri) {
        Log.e("TAG", "importTxt: " )
        try {
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val text = inputStream.bufferedReader().use { it.readText() }
                promptEditText.text = text
                viewModel.sendPrompt(text)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            Toast.makeText(this, "Failed to read .txt file", Toast.LENGTH_SHORT).show()
        }
    }

    // Import and extract text from a PDF file
    private fun importPdf(uri: Uri) {
        try {
            Log.e("TAG", "importPdf: " )
            contentResolver.openInputStream(uri)?.use { inputStream ->
                val bytes: ByteArray = convertToByteArray(inputStream)
                try {
                    var parsedText = ""
                    val reader = PdfReader(bytes)
                    val n = reader.numberOfPages
                    for (i in 0 until n) {
                        parsedText += PdfTextExtractor.getTextFromPage(reader, i + 1)
                            .trim { it <= ' ' } + "\n" // Extracting the content from the different pages
                    }
//                    Log.e("TAG", "onActivityResult: $parsedText")
                    content = parsedText
                    promptEditText.text = content // Display the extracted text
                    reader.close()

                    // Send extracted text to ViewModel for summarization
                    viewModel.sendPrompt(parsedText)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Log.e("TAG", "importPdf: "+e.message )
                    Toast.makeText(this, "Failed to parse PDF", Toast.LENGTH_SHORT).show()
                }
            }
        } catch (exception: IOException) {
            exception.printStackTrace()
            Toast.makeText(this, "Failed to open PDF file", Toast.LENGTH_SHORT).show()
        }
    }

    // Convert InputStream to ByteArray
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

    // Observe ViewModel state changes
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

    override fun onNewIntent(intent: Intent) {
        if (intent!=null){
            Log.d("intent",intent.toString())
        }
        super.onNewIntent(intent)
    }
}

