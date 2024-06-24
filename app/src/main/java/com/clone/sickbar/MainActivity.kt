package com.clone.sickbar

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.datastore.dataStore
import com.google.android.material.button.MaterialButton
import com.google.android.material.slider.RangeSlider
import com.google.android.material.textfield.TextInputEditText
import kotlinx.serialization.json.Json
import org.vosk.LibVosk
import org.vosk.LogLevel
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.SpeechService
import org.vosk.android.SpeechStreamService
import org.vosk.android.StorageService
import java.io.IOException
import kotlin.math.roundToInt


class MainActivity : AppCompatActivity(),org.vosk.android.RecognitionListener {

    private val protoDataStore by dataStore(fileName = "test.json",TestSerializer)
    private var speechRecognizer:SpeechRecognizer?=null
    private lateinit var buttonSave:MaterialButton
    private lateinit var buttonGet:MaterialButton

    private val STATE_START = 0
    private val STATE_READY = 1
    private val STATE_DONE = 2
    private val STATE_FILE = 3
    private val STATE_MIC = 4
    private var previousValue=""

    /* Used to handle permission request */
    private val PERMISSIONS_REQUEST_RECORD_AUDIO = 1

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var speechStreamService: SpeechStreamService? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val slider = findViewById<RangeSlider>(R.id.double_thumb_slider)

        slider.addOnChangeListener { rangeSlider, value, fromUser ->
            // Responds to when slider's value is changed
            println("first thumb value: $${rangeSlider.values[0].roundToInt()}")
            println("second thumb value: $${rangeSlider.values[1].roundToInt()}")
        }
        slider.setLabelFormatter {
            "${slider.values[0].roundToInt()}-${slider.values[1].roundToInt()}"
        }


        val nameInput = findViewById<TextInputEditText>(R.id.name)
        buttonSave = findViewById(R.id.save)
        buttonGet = findViewById(R.id.get)

        // Setup layout

        setUiState(STATE_START)
        buttonSave.setOnClickListener { view: View? -> recognizeMicrophone() }
        buttonGet.setOnClickListener { view: View? -> pause(true) }


        LibVosk.setLogLevel(LogLevel.INFO)


        // Check if user has given permission to record audio, init the model after permission is granted
        val permissionCheck =
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.RECORD_AUDIO)
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf<String>(Manifest.permission.RECORD_AUDIO),
                PERMISSIONS_REQUEST_RECORD_AUDIO
            )
        } else {
            initModel()
        }

       // buttonSave.setOnClickListener {
       //     lifecycleScope.launch {
       //         protoDataStore.updateData {
       //             it.copy(name = nameInput.text.toString(), age = 20)
       //         }
       //         nameInput.text?.clear()
       //     }
       // }
       // buttonGet.setOnClickListener {
       //     lifecycleScope.launch {
       //         protoDataStore.data.collectLatest {
       //             nameInput.setText(it.name)
       //             cancel()
       //         }
       //     }
       // }
    }


    override fun onDestroy() {
        super.onDestroy()
        if (speechService != null) {
            speechService!!.stop()
            speechService!!.shutdown()
        }

        if (speechStreamService != null) {
            speechStreamService!!.stop()
        }
    }

    override fun onPartialResult(hypothesis: String?) {
       if (hypothesis != null) {
          val resultFromJson= Json.decodeFromString<com.clone.sickbar.Model>(hypothesis)
          // if (resultFromJson.partial != previousValue)
           println(resultFromJson.partial)
          // previousValue=resultFromJson.partial
       }

    }

    override fun onResult(hypothesis: String?) {
        println("$hypothesis")
    }

    override fun onFinalResult(hypothesis: String?) {
        println("$hypothesis\n")
        setUiState(STATE_DONE)
        if (speechStreamService != null) {
            speechStreamService = null
        }
    }

    override fun onError(exception: Exception?) {
        setErrorState(exception?.message)
    }

    override fun onTimeout() {
        setUiState(STATE_DONE)
    }

    private fun initModel() {
        StorageService.unpack(
            this, "model-en-us", "model",
            { model: Model? ->
                this.model =
                    model
                setUiState(STATE_READY)
            },
            { exception: IOException ->
                setErrorState(
                    "Failed to unpack the model" + exception.message
                )
            })
    }

    private fun setUiState(state: Int) {
        when (state) {
            STATE_START -> {

            }

            STATE_READY -> {

            }

            STATE_DONE -> {

            }

            STATE_FILE -> {

            }

            STATE_MIC -> {

            }

            else -> throw IllegalStateException("Unexpected value: $state")
        }
    }

    private fun setErrorState(message: String?) {
        println("vosk $message")
    }


    private fun recognizeMicrophone() {
        if (speechService != null) {
            setUiState(STATE_DONE)
            speechService!!.stop()
            speechService = null
        } else {
            setUiState(STATE_MIC)
            try {
                val rec = Recognizer(model, 16000.0f)
                speechService = SpeechService(rec, 16000.0f)
                speechService!!.startListening(this)
            } catch (e: IOException) {
                setErrorState(e.message)
            }
        }
    }


    private fun pause(checked: Boolean) {
        speechService?.setPause(checked)
    }
}