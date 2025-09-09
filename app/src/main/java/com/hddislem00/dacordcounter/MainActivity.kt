package com.hddislem00.dacordcounter

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.hddislem00.dacordcounter.ui.theme.DacordCounterTheme
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class MainActivity : ComponentActivity() {
    private var speachText by mutableStateOf("Speech text here")

    private var counter  by mutableIntStateOf(0)
    private lateinit var speechRecognizer: SpeechRecognizer
    private var isListening by mutableStateOf(false)
    private val PERMISSION_REQUEST_CODE = 1

    @SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Vérifier les permissions
        checkAudioPermissions()

        //initialisation of speech recognizer
        initializeSpeechRecognizer()
        //fin de initialisation

        enableEdgeToEdge()
        setContent {
            DacordCounterTheme {
                Scaffold(
                    topBar = {
                        Text(
                            text = "Dacord Counter",
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(2.dp, Color.Black) // width + color
                                .padding(8.dp)            // padding so text isn't stuck on the border
                        )
                    },
                    modifier = Modifier.fillMaxSize()) {
                    CounterScreen()
                }
            }
        }
    }

    private fun checkAudioPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.RECORD_AUDIO),
                PERMISSION_REQUEST_CODE
            )
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission accordée, réinitialiser le SpeechRecognizer
                initializeSpeechRecognizer()
            } else {
                speachText = "Permission microphone requise"
                Toast.makeText(this, "Permission microphone requise pour la reconnaissance vocale", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun initializeSpeechRecognizer() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }

        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {
                speachText = "Prêt à écouter..."
            }

            override fun onBeginningOfSpeech() {
                speachText = "Écoute en cours..."
            }

            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}

            override fun onEndOfSpeech() {
                isListening = false
                speachText = "Traitement..."
            }

            override fun onError(error: Int) {
                isListening = false
                val message = when (error) {
                    SpeechRecognizer.ERROR_AUDIO -> "Erreur audio - Vérifiez votre microphone"
                    SpeechRecognizer.ERROR_CLIENT -> "Erreur client"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Permissions insuffisantes"
                    SpeechRecognizer.ERROR_NETWORK -> "Erreur réseau - Vérifiez votre connexion"
                    SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Timeout réseau"
                    SpeechRecognizer.ERROR_NO_MATCH -> "Aucune correspondance - Parlez plus clairement"
                    SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Reconnaissance occupée"
                    SpeechRecognizer.ERROR_SERVER -> "Erreur serveur"
                    SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Pas de parole détectée - Parlez plus fort"
                    else -> "Erreur inconnue: $error"
                }
                speachText = message
                Toast.makeText(this@MainActivity, message, Toast.LENGTH_SHORT).show()
            }

            override fun onEvent(eventType: Int, params: Bundle?) {}

            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    speachText = "Partiel: ${matches[0]}"
                }
            }

            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    speachText = matches[0]
                    counter+=countword(speachText)
                } else {
                    speachText = "Aucun résultat"
                }
                isListening = false
            }
        })
    }

    fun countword(phrase:String, words: Array<String> =arrayOf("dacord","d'accord", "d accord")):Int{
      return   words.sumOf { w ->
            phrase.split(" ").count { it == w }
        }
    }

        @Composable

        fun CounterScreen() {

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Counter display

                Text(
                    text = "Counter: ${counter}",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF333333)
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Speech text display
                Text(
                    text = speachText,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray,
                    modifier = Modifier.padding(horizontal = 16.dp),
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Start / Stop Recording Button
                Button(
                    onClick = {
                        if (!isListening) {
                            startListening()
                        } else {
                            stopListening()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isListening) Color.Red else Color(0xFF4CAF50)
                    ),
                    modifier = Modifier
                        .fillMaxWidth(0.6f)
                        .height(56.dp)
                ) {
                    Text(
                        text = if (isListening) "Arrêter" else "Commencer",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                }
            }
        }


    private fun startListening() {
        if (!isListening && ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "fr-FR")
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000L)
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 1500L)
            }

            try {
                speechRecognizer.startListening(intent)
                isListening = true
                speachText = "Initialisation..."
            } catch (e: Exception) {
                speachText = "Erreur: ${e.message}"
                isListening = false
            }
        } else if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            checkAudioPermissions()
        }
    }

    private fun stopListening() {
        if (isListening) {
            speechRecognizer.stopListening()
            isListening = false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

}
