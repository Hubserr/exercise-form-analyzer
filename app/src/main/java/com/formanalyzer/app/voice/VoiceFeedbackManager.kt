package com.formanalyzer.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class VoiceFeedbackManager(context: Context) : TextToSpeech.OnInitListener{
    //tbd - in setting we may disable this
    var enabled = true
    private var ready = false

    private val tts = TextToSpeech(context.applicationContext,this)
    override fun onInit(status: Int) {

     if(status!= TextToSpeech.SUCCESS){
         Log.w(TAG,"Text-to-speech initialization failed: $status ")
         return
     }
        val result = tts.setLanguage(Locale.US)
        ready = result != TextToSpeech.LANG_MISSING_DATA
                &&result!= TextToSpeech.LANG_NOT_SUPPORTED
        if(!ready) {
            Log.w(TAG, "English text-to-speech voice is not available")
        }
    }
    fun speak(text: String){
        if(!enabled||!ready) return
        tts.speak(text, TextToSpeech.QUEUE_ADD,null,text)
    }

    fun shutdown(){
        tts.stop()
        tts.shutdown()
    }
    private companion object{
        const val TAG = "VoiceFeedback"
    }



}