package com.example.textsum

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Firebase
import com.google.firebase.vertexai.GenerativeModel
import com.google.firebase.vertexai.type.content
import com.google.firebase.vertexai.vertexAI
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch


class SummarizeViewModel : ViewModel() {
    private val _uiState: MutableStateFlow<UiState> = MutableStateFlow(UiState.Initial)
    val uiState: StateFlow<UiState> = _uiState

    /*private val generativeModel = GenerativeModel(
        modelName = "gemini-pro",
        apiKey = "AIzaSyC2NAWzj_Ih0q6AhkEoIna6mKw3GA4_5Bo"
    )*/

//    private val generativeModel = Firebase.vertexAI.generativeModel("gemini-pro" )
        private var generativeModel: GenerativeModel?
        init {
            try {
                generativeModel = Firebase.vertexAI.generativeModel("gemini-pro")
                println("Generative model initialized successfully!")
            } catch (e: Exception) {
                generativeModel = null
                Log.d("srujal","Error initializing generative model:",e)
            }
        }
    fun sendPrompt(prompt: String) {
        Log.d("srujal", "sendPrompt called: $prompt")
        _uiState.value = UiState.Loading

        viewModelScope.launch(Dispatchers.IO) {
            try {

                val response = generativeModel!!.generateContent (

                    content {
                        text(prompt)
                    },)

                response.text?.let { outputContent ->
                    Log.d("response", "output: $outputContent")
                    _uiState.value = UiState.Success(outputContent)
                }
            } catch (e: Exception) {
                Log.d("Exception", "output: ${e.toString()}")
                _uiState.value = UiState.Error(e.localizedMessage ?: "An error occurred")
            }
        }
    }
}

